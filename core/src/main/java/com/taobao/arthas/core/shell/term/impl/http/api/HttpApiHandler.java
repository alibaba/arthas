package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.distribution.PackingResultDistributor;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.distribution.impl.PackingResultDistributorImpl;
import com.taobao.arthas.core.distribution.impl.ResultConsumerImpl;
import com.taobao.arthas.core.distribution.impl.SharingResultDistributorImpl;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.history.HistoryManager;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSession;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.termd.core.function.Function;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Http Restful Api Handler
 *
 * @author gongdewei 2020-03-18
 */
public class HttpApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpApiHandler.class);
    private static final ValueFilter[] JSON_FILTERS = new ValueFilter[] { new ObjectVOFilter() };
    private static final String ONETIME_SESSION_KEY = "oneTimeSession";
    public static final int DEFAULT_EXEC_TIMEOUT = 30000;
    private final SessionManager sessionManager;
    private final InternalCommandManager commandManager;
    private final JobController jobController;
    private final HistoryManager historyManager;

    public HttpApiHandler(HistoryManager historyManager, SessionManager sessionManager) {
        this.historyManager = historyManager;
        this.sessionManager = sessionManager;
        commandManager = this.sessionManager.getCommandManager();
        jobController = this.sessionManager.getJobController();
    }

    public HttpResponse handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        ApiResponse result;
        String requestBody = null;
        String requestId = null;
        try {
            HttpMethod method = request.method();
            if (HttpMethod.POST.equals(method)) {
                requestBody = getBody(request);
                ApiRequest apiRequest = parseRequest(requestBody);
                requestId = apiRequest.getRequestId();
                result = processRequest(ctx, apiRequest);
            } else {
                result = createResponse(ApiState.REFUSED, "Unsupported http method: " + method.name());
            }
        } catch (Throwable e) {
            result = createResponse(ApiState.FAILED, "Process request error: " + e.getMessage());
            logger.error("arthas process http api request error: " + request.uri() + ", request body: " + requestBody, e);
        }
        if (result == null) {
            result = createResponse(ApiState.FAILED, "The request was not processed");
        }
        result.setRequestId(requestId);

        byte[] jsonBytes = JSON.toJSONBytes(result, JSON_FILTERS);

        // create http response
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(jsonBytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
        return response;
    }

    private ApiRequest parseRequest(String requestBody) throws ApiException {
        if (StringUtils.isBlank(requestBody)) {
            throw new ApiException("parse request failed: request body is empty");
        }
        try {
            //ObjectMapper objectMapper = new ObjectMapper();
            //return objectMapper.readValue(requestBody, ApiRequest.class);
            return JSON.parseObject(requestBody, ApiRequest.class);
        } catch (Exception e) {
            throw new ApiException("parse request failed: " + e.getMessage(), e);
        }
    }

    private ApiResponse processRequest(ChannelHandlerContext ctx, ApiRequest apiRequest) {

        String actionStr = apiRequest.getAction();
        try {
            if (StringUtils.isBlank(actionStr)) {
                throw new ApiException("'action' is required");
            }
            ApiAction action;
            try {
                action = ApiAction.valueOf(actionStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("unknown action: " + actionStr);
            }

            //no session required
            if (ApiAction.INIT_SESSION.equals(action)) {
                return processInitSessionRequest(apiRequest);
            }

            //required session
            Session session = null;
            boolean allowNullSession = ApiAction.EXEC.equals(action);
            String sessionId = apiRequest.getSessionId();
            if (StringUtils.isBlank(sessionId)) {
                if (!allowNullSession) {
                    throw new ApiException("'sessionId' is required");
                }
            } else {
                session = sessionManager.getSession(sessionId);
                if (session == null) {
                    throw new ApiException("session not found: " + sessionId);
                }
                sessionManager.updateAccessTime(session);
            }

            // 标记所谓的一次性session
            if (session == null) {
                session = sessionManager.createSession();
                session.put(ONETIME_SESSION_KEY, new Object());
            }

            // 请求到达这里，如果有需要鉴权，则已经在前面的handler里处理过了
            // 如果有鉴权取到的 Subject，则传递到 arthas的session里
            HttpSession httpSession = HttpSessionManager.getHttpSessionFromContext(ctx);
            if (httpSession != null) {
                Object subject = httpSession.getAttribute(ArthasConstants.SUBJECT_KEY);
                if (subject != null) {
                    session.put(ArthasConstants.SUBJECT_KEY, subject);
                }
            }

            //dispatch requests
            ApiResponse response = dispatchRequest(action, apiRequest, session);
            if (response != null) {
                return response;
            }

        } catch (ApiException e) {
            logger.info("process http api request failed: {}", e.getMessage());
            return createResponse(ApiState.FAILED, e.getMessage());
        } catch (Throwable e) {
            logger.error("process http api request failed: " + e.getMessage(), e);
            return createResponse(ApiState.FAILED, "process http api request failed: " + e.getMessage());
        }

        return createResponse(ApiState.REFUSED, "Unsupported action: " + actionStr);
    }

    private ApiResponse dispatchRequest(ApiAction action, ApiRequest apiRequest, Session session) throws ApiException {
        switch (action) {
            case EXEC:
                return processExecRequest(apiRequest, session);
            case ASYNC_EXEC:
                return processAsyncExecRequest(apiRequest, session);
            case INTERRUPT_JOB:
                return processInterruptJob(apiRequest, session);
            case PULL_RESULTS:
                return processPullResultsRequest(apiRequest, session);
            case SESSION_INFO:
                return processSessionInfoRequest(apiRequest, session);
            case JOIN_SESSION:
                return processJoinSessionRequest(apiRequest, session);
            case CLOSE_SESSION:
                return processCloseSessionRequest(apiRequest, session);
            case INIT_SESSION:
                break;
        }
        return null;
    }

    private ApiResponse processInitSessionRequest(ApiRequest apiRequest) throws ApiException {
        ApiResponse response = new ApiResponse();

        //create session
        Session session = sessionManager.createSession();
        if (session != null) {

            //Result Distributor
            SharingResultDistributorImpl resultDistributor = new SharingResultDistributorImpl(session);
            //create consumer
            ResultConsumer resultConsumer = new ResultConsumerImpl();
            resultDistributor.addConsumer(resultConsumer);
            session.setResultDistributor(resultDistributor);

            resultDistributor.appendResult(new MessageModel("Welcome to arthas!"));

            //welcome message
            WelcomeModel welcomeModel = new WelcomeModel();
            welcomeModel.setVersion(ArthasBanner.version());
            welcomeModel.setWiki(ArthasBanner.wiki());
            welcomeModel.setTutorials(ArthasBanner.tutorials());
            welcomeModel.setMainClass(PidUtils.mainClass());
            welcomeModel.setPid(PidUtils.currentPid());
            welcomeModel.setTime(DateUtils.getCurrentDateTime());
            resultDistributor.appendResult(welcomeModel);

            //allow input
            updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);

            response.setSessionId(session.getSessionId())
                    .setConsumerId(resultConsumer.getConsumerId())
                    .setState(ApiState.SUCCEEDED);
        } else {
            throw new ApiException("create api session failed");
        }
        return response;
    }

    /**
     * Update session input status for all consumer
     *
     * @param session
     * @param inputStatus
     */
    private void updateSessionInputStatus(Session session, InputStatus inputStatus) {
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            resultDistributor.appendResult(new InputStatusModel(inputStatus));
        }
    }

    private ApiResponse processJoinSessionRequest(ApiRequest apiRequest, Session session) {

        //create consumer
        ResultConsumer resultConsumer = new ResultConsumerImpl();
        //disable input and interrupt
        resultConsumer.appendResult(new InputStatusModel(InputStatus.DISABLED));
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            resultDistributor.addConsumer(resultConsumer);
        }

        ApiResponse response = new ApiResponse();
        response.setSessionId(session.getSessionId())
                .setConsumerId(resultConsumer.getConsumerId())
                .setState(ApiState.SUCCEEDED);
        return response;
    }

    private ApiResponse processSessionInfoRequest(ApiRequest apiRequest, Session session) {
        ApiResponse response = new ApiResponse();
        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("pid", session.getPid());
        body.put("createTime", session.getCreateTime());
        body.put("lastAccessTime", session.getLastAccessTime());

        response.setState(ApiState.SUCCEEDED)
                .setSessionId(session.getSessionId())
                //.setConsumerId(consumerId)
                .setBody(body);
        return response;
    }

    private ApiResponse processCloseSessionRequest(ApiRequest apiRequest, Session session) {
        sessionManager.removeSession(session.getSessionId());
        ApiResponse response = new ApiResponse();
        response.setState(ApiState.SUCCEEDED);
        return response;
    }

    /**
     * Execute command sync, wait for job finish or timeout, sending results immediately
     *
     * @param apiRequest
     * @param session
     * @return
     */
    private ApiResponse processExecRequest(ApiRequest apiRequest, Session session) {
        boolean oneTimeAccess = false;
        if (session.get(ONETIME_SESSION_KEY) != null) {
            oneTimeAccess = true;
        }

        try {
            String commandLine = apiRequest.getCommand();
            Map<String, Object> body = new TreeMap<String, Object>();
            body.put("command", commandLine);

            ApiResponse response = new ApiResponse();
            response.setSessionId(session.getSessionId())
                    .setBody(body);

            if (!session.tryLock()) {
                response.setState(ApiState.REFUSED)
                        .setMessage("Another command is executing.");
                return response;
            }

            int lock = session.getLock();
            PackingResultDistributor packingResultDistributor = null;
            Job job = null;
            try {
                Job foregroundJob = session.getForegroundJob();
                if (foregroundJob != null) {
                    response.setState(ApiState.REFUSED)
                            .setMessage("Another job is running.");
                    logger.info("Another job is running, jobId: {}", foregroundJob.id());
                    return response;
                }

                packingResultDistributor = new PackingResultDistributorImpl(session);
                //distribute result message both to origin session channel and request channel by CompositeResultDistributor
                //ResultDistributor resultDistributor = new CompositeResultDistributorImpl(packingResultDistributor, session.getResultDistributor());
                job = this.createJob(commandLine, session, packingResultDistributor);
                session.setForegroundJob(job);
                updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

                job.run();

            } catch (Throwable e) {
                logger.error("Exec command failed:" + e.getMessage() + ", command:" + commandLine, e);
                response.setState(ApiState.FAILED).setMessage("Exec command failed:" + e.getMessage());
                return response;
            } finally {
                if (session.getLock() == lock) {
                    session.unLock();
                }
            }

            //wait for job completed or timeout
            Integer timeout = apiRequest.getExecTimeout();
            if (timeout == null || timeout <= 0) {
                timeout = DEFAULT_EXEC_TIMEOUT;
            }
            boolean timeExpired = !waitForJob(job, timeout);
            if (timeExpired) {
                logger.warn("Job is exceeded time limit, force interrupt it, jobId: {}", job.id());
                job.interrupt();
                response.setState(ApiState.INTERRUPTED).setMessage("The job is exceeded time limit, force interrupt");
            } else {
                response.setState(ApiState.SUCCEEDED);
            }

            //packing results
            body.put("jobId", job.id());
            body.put("jobStatus", job.status());
            body.put("timeExpired", timeExpired);
            if (timeExpired) {
                body.put("timeout", timeout);
            }
            body.put("results", packingResultDistributor.getResults());

            response.setSessionId(session.getSessionId())
                    //.setConsumerId(consumerId)
                    .setBody(body);
            return response;
        } finally {
            if (oneTimeAccess) {
                sessionManager.removeSession(session.getSessionId());
            }
        }
    }

    /**
     * Execute command async, create and schedule the job running, but no wait for the results.
     *
     * @param apiRequest
     * @param session
     * @return
     */
    private ApiResponse processAsyncExecRequest(ApiRequest apiRequest, Session session) {
        String commandLine = apiRequest.getCommand();
        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("command", commandLine);

        ApiResponse response = new ApiResponse();
        response.setSessionId(session.getSessionId())
                .setBody(body);

        if (!session.tryLock()) {
            response.setState(ApiState.REFUSED)
                    .setMessage("Another command is executing.");
            return response;
        }
        int lock = session.getLock();
        try {

            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                response.setState(ApiState.REFUSED)
                        .setMessage("Another job is running.");
                logger.info("Another job is running, jobId: {}", foregroundJob.id());
                return response;
            }

            //create job
            Job job = this.createJob(commandLine, session, session.getResultDistributor());
            body.put("jobId", job.id());
            body.put("jobStatus", job.status());
            response.setState(ApiState.SCHEDULED);

            //add command before exec job
            CommandRequestModel commandRequestModel = new CommandRequestModel(commandLine, response.getState());
            commandRequestModel.setJobId(job.id());
            SharingResultDistributor resultDistributor = session.getResultDistributor();
            if (resultDistributor != null) {
                resultDistributor.appendResult(commandRequestModel);
            }
            session.setForegroundJob(job);
            updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

            //run job
            job.run();

            return response;
        } catch (Throwable e) {
            logger.error("Async exec command failed:" + e.getMessage() + ", command:" + commandLine, e);
            response.setState(ApiState.FAILED).setMessage("Async exec command failed:" + e.getMessage());
            CommandRequestModel commandRequestModel = new CommandRequestModel(commandLine, response.getState(), response.getMessage());
            session.getResultDistributor().appendResult(commandRequestModel);
            return response;
        } finally {
            if (session.getLock() == lock) {
                session.unLock();
            }
        }
    }

    private ApiResponse processInterruptJob(ApiRequest apiRequest, Session session) {
        Job job = session.getForegroundJob();
        if (job == null) {
            return new ApiResponse().setState(ApiState.FAILED).setMessage("no foreground job is running");
        }
        job.interrupt();

        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("jobId", job.id());
        body.put("jobStatus", job.status());
        return new ApiResponse()
                .setState(ApiState.SUCCEEDED)
                .setBody(body);
    }

    /**
     * Pull results from result queue
     *
     * @param apiRequest
     * @param session
     * @return
     */
    private ApiResponse processPullResultsRequest(ApiRequest apiRequest, Session session) throws ApiException {
        String consumerId = apiRequest.getConsumerId();
        if (StringUtils.isBlank(consumerId)) {
            throw new ApiException("'consumerId' is required");
        }
        ResultConsumer consumer = null;
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            consumer = resultDistributor.getConsumer(consumerId);
        }
        if (consumer == null) {
            throw new ApiException("consumer not found: " + consumerId);
        }

        List<ResultModel> results = consumer.pollResults();
        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("results", results);

        ApiResponse response = new ApiResponse();
        response.setState(ApiState.SUCCEEDED)
                .setSessionId(session.getSessionId())
                .setConsumerId(consumerId)
                .setBody(body);
        return response;
    }

    private boolean waitForJob(Job job, int timeout) {
        long startTime = System.currentTimeMillis();
        while (true) {
            switch (job.status()) {
                case STOPPED:
                case TERMINATED:
                    return true;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                return false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    private synchronized Job createJob(List<CliToken> args, Session session, ResultDistributor resultDistributor) {
        Job job = jobController.createJob(commandManager, args, session, new ApiJobHandler(session), new ApiTerm(session), resultDistributor);
        return job;
    }

    private Job createJob(String line, Session session, ResultDistributor resultDistributor) {
        historyManager.addHistory(line);
        return createJob(CliTokens.tokenize(line), session, resultDistributor);
    }

    private ApiResponse createResponse(ApiState apiState, String message) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setState(apiState);
        apiResponse.setMessage(message);
        return apiResponse;
    }

    private String getBody(FullHttpRequest request) {
        ByteBuf buf = request.content();
        return buf.toString(CharsetUtil.UTF_8);
    }

    private class ApiJobHandler implements JobListener {

        private Session session;

        public ApiJobHandler(Session session) {
            this.session = session;
        }

        @Override
        public void onForeground(Job job) {
            session.setForegroundJob(job);
        }

        @Override
        public void onBackground(Job job) {
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
            }
        }

        @Override
        public void onTerminated(Job job) {
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
            }
        }

        @Override
        public void onSuspend(Job job) {
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
            }
        }
    }

    private static class ApiTerm implements Term {

        private Session session;

        public ApiTerm(Session session) {
            this.session = session;
        }

        @Override
        public Term resizehandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public String type() {
            return "web";
        }

        @Override
        public int width() {
            return 1000;
        }

        @Override
        public int height() {
            return 200;
        }

        @Override
        public Term stdinHandler(Handler<String> handler) {
            return this;
        }

        @Override
        public Term stdoutHandler(Function<String, String> handler) {
            return this;
        }

        @Override
        public Term write(String data) {
            return this;
        }

        @Override
        public long lastAccessedTime() {
            return session.getLastAccessTime();
        }

        @Override
        public Term echo(String text) {
            return this;
        }

        @Override
        public Term setSession(Session session) {
            return this;
        }

        @Override
        public Term interruptHandler(SignalHandler handler) {
            return this;
        }

        @Override
        public Term suspendHandler(SignalHandler handler) {
            return this;
        }

        @Override
        public void readline(String prompt, Handler<String> lineHandler) {

        }

        @Override
        public void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler) {

        }

        @Override
        public Term closeHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public void close() {

        }
    }
}

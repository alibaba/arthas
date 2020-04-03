package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.core.command.model.CommandModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.PackingResultDistributor;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.impl.PackingResultDistributorImpl;
import com.taobao.arthas.core.distribution.impl.ResultConsumerImpl;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.logger.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.termd.core.function.Function;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Http Restful Api Handler
 *
 * @author gongdewei 2020-03-18
 */
public class HttpApiHandler {

    private static final Logger logger = LogUtil.getArthasLogger();
    private final SessionManager sessionManager;
    private final AtomicInteger requestIdGenerator = new AtomicInteger(0);
    private static HttpApiHandler instance;
    private final InternalCommandManager commandManager;
    private final JobControllerImpl jobController;

    public static HttpApiHandler getInstance() {
        if (instance == null) {
            synchronized (HttpApiHandler.class) {
                instance = new HttpApiHandler();
            }
        }
        return instance;
    }

    private HttpApiHandler() {
        sessionManager = ArthasBootstrap.getInstance().getSessionManager();
        commandManager = sessionManager.getCommandManager();
        jobController = sessionManager.getJobController();
    }

    public HttpResponse handle(FullHttpRequest request) throws Exception {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                HttpResponseStatus.OK);

        ApiResponse result;
        String requestBody = null;
        String requestId = "req_" + requestIdGenerator.addAndGet(1);
        try {
            HttpMethod method = request.method();
            if (HttpMethod.POST.equals(method)) {
                requestBody = getBody(request);
                ApiRequest apiRequest = parseRequest(requestBody);
                apiRequest.setRequestId(requestId);
                result = processRequest(apiRequest);
            } else {
                result = createResponse(ApiState.REFUSED, "Unsupported http method: " + method.name());
            }
        } catch (Throwable e) {
            result = createResponse(ApiState.FAILED, "Process request error: " + e.getMessage());
            logger.error("arthas", "arthas process http api request error: " + request.uri() + ", request body: " + requestBody, e);
        }
        if (result == null) {
            result = createResponse(ApiState.FAILED, "The request was not processed");
        }

        result.setRequestId(requestId);
        String jsonResult = JSON.toJSONString(result);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
        response.content().writeBytes(jsonResult.getBytes("UTF-8"));
        return response;
    }

    private ApiRequest parseRequest(String requestBody) throws ApiException {
        if (StringUtils.isBlank(requestBody)) {
            throw new ApiException("parse request failed: request body is empty");
        }
        try {
            //Object jsonRequest = JSON.parse(requestBody);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(requestBody, ApiRequest.class);
        } catch (Exception e) {
            throw new ApiException("parse request failed: " + e.getMessage(), e);
        }
    }

    private ApiResponse processRequest(ApiRequest apiRequest) {

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
            String sessionId = apiRequest.getSessionId();
            if (StringUtils.isBlank(sessionId)) {
                throw new ApiException("'sessionId' is required");
            }
            Session session = sessionManager.getSession(sessionId);
            if (session == null) {
                throw new ApiException("session not found: "+sessionId);
            }
            sessionManager.updateAccessTime(session);

            //dispatch requests
            ApiResponse response = dispatchRequest(action, apiRequest, session);
            if (response != null) {
                return response;
            }

        } catch (ApiException e) {
            logger.info("arthas", e.getMessage(), e);
            return createResponse(ApiState.FAILED, e.getMessage());
        } catch (Throwable e) {
            logger.error("arthas", "process http api request failed: " + e.getMessage(), e);
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

            //create consumer
            ResultConsumer resultConsumer = new ResultConsumerImpl();
            session.getResultDistributor().addConsumer(resultConsumer);

            response.setSessionId(session.getSessionId())
                    .setConsumerId(resultConsumer.getConsumerId())
                    .setState(ApiState.SUCCEEDED);
        } else {
            throw new ApiException("create api session failed");
        }
        return response;
    }

    private ApiResponse processJoinSessionRequest(ApiRequest apiRequest, Session session) {

        //create consumer
        ResultConsumer resultConsumer = new ResultConsumerImpl();
        session.getResultDistributor().addConsumer(resultConsumer);

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
        String commandLine = apiRequest.getCommand();
        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("command", commandLine);

        ApiResponse response = new ApiResponse();
        response.setSessionId(session.getSessionId())
                .setBody(body);

        PackingResultDistributor packingResultDistributor = null;
        Job job = null;
        try {
            packingResultDistributor = new PackingResultDistributorImpl(session);
            job = this.createJob(commandLine, session, packingResultDistributor);
            job.run();

            response.setState(ApiState.SCHEDULED);
        } catch (Throwable e) {
            logger.error("arthas", "Exec command failed:"+e.getMessage()+", command:"+commandLine, e);
            response.setState(ApiState.FAILED).setMessage("Exec command failed:"+e.getMessage());
            return response;
        }

        //wait for job completed or timeout
        boolean timeExpired = !waitForJob(job);
        if (timeExpired) {
            logger.warn("arthas", "Job is exceeded time limit, force interrupt it, jobId: {}", job.id());
            job.interrupt();
        }

        //packing results
        body.put("jobId", job.id());
        body.put("jobStatus", job.status());
        body.put("timeExpired", timeExpired);
        body.put("results", packingResultDistributor.getResults());

        response.setSessionId(session.getSessionId())
                //.setConsumerId(consumerId)
                .setBody(body);
        return response;
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
        try {
            //create job
            Job job = this.createJob(commandLine, session, session.getResultDistributor());
            body.put("jobId", job.id());
            body.put("jobStatus", job.status());
            response.setState(ApiState.SCHEDULED);

            //add command before exec job
            CommandModel commandModel = new CommandModel(commandLine, response.getState());
            commandModel.setJobId(job.id());
            session.getResultDistributor().appendResult(commandModel);

            //run job
            job.run();

            return response;

        } catch (Throwable e) {
            logger.error("arthas", "Async exec command failed:"+e.getMessage()+", command:"+commandLine, e);
            response.setState(ApiState.FAILED).setMessage("Async exec command failed:"+e.getMessage());
            CommandModel commandModel = new CommandModel(commandLine, response.getState(), response.getMessage());
            session.getResultDistributor().appendResult(commandModel);
            return response;
        }
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
        ResultConsumer consumer = session.getResultDistributor().getConsumer(consumerId);
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

    private boolean waitForJob(Job job) {
        long startTime = System.currentTimeMillis();
        while (true) {
            switch (job.status()) {
                case STOPPED:
                case TERMINATED:
                    return true;
            }
            if (System.currentTimeMillis() - startTime > 30000) {
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

        }

        @Override
        public void onBackground(Job job) {

        }

        @Override
        public void onTerminated(Job job) {

        }

        @Override
        public void onSuspend(Job job) {

        }
    }

    private class ApiTerm implements Term {

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

package com.taobao.arthas.core.channel;

import com.alibaba.arthas.channel.client.ChannelClient;
import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.ExecuteParams;
import com.alibaba.arthas.channel.proto.ExecuteResult;
import com.alibaba.arthas.channel.proto.RequestAction;
import com.alibaba.arthas.channel.proto.ResponseStatus;
import com.alibaba.arthas.channel.proto.ResultFormat;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.model.CommandRequestModel;
import com.taobao.arthas.core.command.model.InputStatus;
import com.taobao.arthas.core.command.model.InputStatusModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.command.model.WelcomeModel;
import com.taobao.arthas.core.distribution.PackingResultDistributor;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.impl.PackingResultDistributorImpl;
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
import com.taobao.arthas.core.shell.term.impl.http.api.ApiState;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import io.termd.core.function.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/8/16
 */
public class ChannelRequestHandler implements ChannelClient.RequestListener {

    private static final Logger logger = LoggerFactory.getLogger(ChannelRequestHandler.class);
    public static final int DEFAULT_EXEC_TIMEOUT = 30000;

    private ChannelClient channelClient;
    private final SessionManager sessionManager;
    private final HistoryManager historyManager;
    private final InternalCommandManager commandManager;
    private final JobController jobController;

    private PBResultConverter pbResultConverter = new PBResultConverter();

    public ChannelRequestHandler(ChannelClient channelClient, SessionManager sessionManager, HistoryManager historyManager) {
        this.channelClient = channelClient;
        this.sessionManager = sessionManager;
        this.historyManager = historyManager;
        commandManager = this.sessionManager.getCommandManager();
        jobController = this.sessionManager.getJobController();
    }

    @Override
    public void onRequest(ActionRequest request) {

        try {
            processRequest(request);
        } catch (Throwable e) {
            String msg = "process request error: " + e.toString();
            logger.error(msg, e);

            sendResponse(ActionResponse.newBuilder()
                    .setAgentId(request.getAgentId())
                    .setRequestId(request.getRequestId())
                    .setStatus(ResponseStatus.FAILED)
                    .setMessage(StringValue.of(msg)));
        }

    }

    private void processRequest(ActionRequest request) throws Exception {
        String sessionId = null;
        if (request.hasSessionId()) {
            sessionId = request.getSessionId().getValue();
        }

        Session session = null;
        if (!StringUtils.isBlank(sessionId)) {
            session = sessionManager.getSession(sessionId);
            if (session == null) {
                throw new Exception("session not found: " + sessionId);
            }
        }

        RequestAction action = request.getAction();
        switch (action) {
            case EXECUTE:
                processExec(request, session);
                return;
            case ASYNC_EXECUTE:
                processAsyncExec(request, session);
                return;
            case INIT_SESSION:
                processInitSession(request);
                return;
            case UNRECOGNIZED:
                processUnrecognizedAction(request, session);
                return;
        }

        //required session
        if (session == null) {
            throw new Exception("session is required");
        }
        switch (action) {
            case INTERRUPT_JOB:
                processInterruptJob(request, session);
                return;
            case CLOSE_SESSION:
                processCloseSession(request, session);
                return;
//            case JOIN_SESSION:
//                processJoinSession(request, session);
//                return;
        }

    }

    private void processExec(ActionRequest request, Session session) {
        boolean oneTimeAccess = false;
        if (session == null) {
            oneTimeAccess = true;
            session = sessionManager.createSession();
        }

        try {
            ExecuteParams executeParams = request.getExecuteParams();
            String commandLine = executeParams.getCommandLine();
            PackingResultDistributor packingResultDistributor = null;
            Job job = null;

            if (!session.tryLock()) {
                sendError(request, session, ResponseStatus.REFUSED, "Another command is executing.");
                return;
            }

            int lock = session.getLock();
            try {
                Job foregroundJob = session.getForegroundJob();
                if (foregroundJob != null) {
                    logger.info("Another job is running, jobId: {}", foregroundJob.id());
                    sendError(request, session, ResponseStatus.REFUSED, "Another job is running.");
                    return;
                }

                packingResultDistributor = new PackingResultDistributorImpl(session);
                job = this.createJob(commandLine, session, packingResultDistributor);
                session.setForegroundJob(job);
                //updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

                job.run();

            } catch (Throwable e) {
                logger.error("Exec command failed:" + e.getMessage() + ", command:" + commandLine, e);
                sendError(request, session, ResponseStatus.FAILED, "Exec command failed:" + e.getMessage());
                return;
            } finally {
                if (session.getLock() == lock) {
                    session.unLock();
                }
            }

            //wait for job completed or timeout
            int timeout = executeParams.getExecTimeout();
            if (timeout <= 0) {
                timeout = DEFAULT_EXEC_TIMEOUT;
            }
            boolean timeExpired = !waitForJob(job, timeout);

            ActionResponse.Builder responseBuilder;
            if (timeExpired) {
                logger.warn("Job is exceeded time limit, force interrupt it, jobId: {}", job.id());
                job.interrupt();
                responseBuilder = createResponse(request, session, ResponseStatus.INTERRUPTED,
                        "The job is exceeded time limit, force interrupt");
            } else {
                responseBuilder = createResponse(request, session, ResponseStatus.SUCCEEDED);
            }

            List<ResultModel> resultModels = packingResultDistributor.getResults();
            sendResults(responseBuilder, resultModels, executeParams.getResultFormat());

        } finally {
            if (oneTimeAccess) {
                sessionManager.closeSession(session.getSessionId());
            }
        }
    }

    private void processAsyncExec(ActionRequest request, Session session) {
        //TODO
        ExecuteParams executeParams = request.getExecuteParams();
        String commandLine = executeParams.getCommandLine();

        if (session == null) {
            session = sessionManager.createSession();
        }

        if (!session.tryLock()) {
            sendError(request, session, ResponseStatus.REFUSED, "Another command is executing.");
            return;
        }
        int lock = session.getLock();
        try {

            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                sendError(request, session, ResponseStatus.REFUSED, "Another job is running.");
                return;
            }

            //create job
//            Job job = this.createJob(commandLine, session, session.getResultDistributor());
            ChannelResultDistributor channelResultDistributor = new ChannelResultDistributor(request, session);
            session.setResultDistributor(channelResultDistributor);
            Job job = this.createJob(commandLine, session, channelResultDistributor);
            session.setForegroundJob(job);

            //add command before exec job
            CommandRequestModel commandRequestModel = new CommandRequestModel(commandLine, ApiState.SCHEDULED);
            commandRequestModel.setJobId(job.id());
            session.getResultDistributor().appendResult(commandRequestModel);
            updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

            //run job
            job.run();

            ActionResponse.Builder response = createResponse(request, session, ResponseStatus.CONTINUOUS);
            sendResponse(response);

        } catch (Throwable e) {
            logger.error("Async exec command failed:" + e.getMessage() + ", command:" + commandLine, e);
            sendError(request, session, ResponseStatus.FAILED, "Async exec command failed:" + e.getMessage());
        } finally {
            if (session.getLock() == lock) {
                session.unLock();
            }
        }
    }

    private void processInterruptJob(ActionRequest request, Session session) {
        Job job = session.getForegroundJob();
        if (job != null) {
            job.interrupt();
        }

        sendResponse(createResponse(request, session, ResponseStatus.SUCCEEDED));
    }

    private void processInitSession(ActionRequest request) throws Exception {
        //create session
        Session session = sessionManager.createSession();
        if (session != null) {

            //Result Distributor
//            SharingResultDistributorImpl resultDistributor = new SharingResultDistributorImpl(session);
//            //create consumer
//            ResultConsumer resultConsumer = new ResultConsumerImpl();
//            resultDistributor.addConsumer(resultConsumer);

            ChannelResultDistributor resultDistributor = new ChannelResultDistributor(request, session);
            session.setResultDistributor(resultDistributor);
            resultDistributor.appendResult(new MessageModel("Welcome to arthas!"));

            //welcome message
            WelcomeModel welcomeModel = new WelcomeModel();
            welcomeModel.setVersion(ArthasBanner.version());
            welcomeModel.setWiki(ArthasBanner.wiki());
            welcomeModel.setTutorials(ArthasBanner.tutorials());
            welcomeModel.setPid(PidUtils.currentPid());
            welcomeModel.setTime(DateUtils.getCurrentDate());
            resultDistributor.appendResult(welcomeModel);

            //allow input
            updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);

            sendResponse(ActionResponse.newBuilder()
                    .setAgentId(request.getAgentId())
                    .setRequestId(request.getRequestId())
                    .setSessionId(StringValue.of(session.getSessionId()))
                    //.setConsumerId(StringValue.of(resultConsumer.getConsumerId()))
                    .setStatus(ResponseStatus.SUCCEEDED));
        } else {
            throw new Exception("create api session failed");
        }
    }

    /**
     * Update session input status for all consumer
     *
     * @param session
     * @param inputStatus
     */
    private void updateSessionInputStatus(Session session, InputStatus inputStatus) {
        ResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            resultDistributor.appendResult(new InputStatusModel(inputStatus));
        }
    }

//    private void processJoinSession(ActionRequest request, Session session) {
//        //create consumer
//        ResultConsumer resultConsumer = new ResultConsumerImpl();
//        //disable input and interrupt
//        resultConsumer.appendResult(new InputStatusModel(InputStatus.DISABLED));
//        ResultDistributor resultDistributor = session.getResultDistributor();
//        if (resultDistributor instanceof SharingResultDistributor) {
//            SharingResultDistributor sharingResultDistributor = (SharingResultDistributor) resultDistributor;
//            sharingResultDistributor.addConsumer(resultConsumer);
//        } else {
//            throw new UnsupportedOperationException("This session does not support joining.");
//        }
//
//        ActionResponse actionResponse = ActionResponse.newBuilder()
//                .setAgentId(request.getAgentId())
//                .setRequestId(request.getRequestId())
//                .setSessionId(StringValue.of(session.getSessionId()))
//                .setConsumerId(StringValue.of(resultConsumer.getConsumerId()))
//                .setStatus(ResponseStatus.SUCCEEDED)
//                .build();
//        channelClient.submitResponse(actionResponse);
//    }

    private void processCloseSession(ActionRequest request, Session session) {
        sessionManager.closeSession(session.getSessionId());
        ActionResponse actionResponse = createResponse(request, session, ResponseStatus.SUCCEEDED)
                .build();
        channelClient.submitResponse(actionResponse);
    }

    private void processUnrecognizedAction(ActionRequest request, Session session) {
        ActionResponse actionResponse = ActionResponse.newBuilder()
                .setAgentId(request.getAgentId())
                .setRequestId(request.getRequestId())
                .setStatus(ResponseStatus.FAILED)
                .setMessage(StringValue.of("unsupported action"))
                .build();
        channelClient.submitResponse(actionResponse);
    }

    private void sendResults(ActionResponse.Builder response, List<ResultModel> resultModels, ResultFormat resultFormat) {
        if (resultFormat.equals(ResultFormat.JSON)) {
            response.setExecuteResult(ExecuteResult.newBuilder()
                    .setResultsJson(StringValue.of(JSON.toJSONString(resultModels)))
                    .build());
        } else {
            // return pb results
            List<Any> results = pbResultConverter.convertResults(resultModels);
            response.setExecuteResult(ExecuteResult.newBuilder()
                    .addAllResults(results)
                    .build());
        }
        sendResponse(response);
    }

    private void sendError(ActionRequest request, Session session, ResponseStatus status, String msg) {
        sendResponse(createResponse(request, session, status, msg));
    }

    private void sendResponse(ActionResponse.Builder response) {
        channelClient.submitResponse(response.build());
    }

    private ActionResponse.Builder createResponse(ActionRequest request, Session session, ResponseStatus status) {
        return ActionResponse.newBuilder()
                .setAgentId(request.getAgentId())
                .setRequestId(request.getRequestId())
                .setSessionId(StringValue.of(session.getSessionId()))
                .setStatus(status);
    }

    private ActionResponse.Builder createResponse(ActionRequest request, Session session, ResponseStatus status, String message) {
        return ActionResponse.newBuilder()
                .setAgentId(request.getAgentId())
                .setRequestId(request.getRequestId())
                .setSessionId(StringValue.of(session.getSessionId()))
                .setStatus(status)
                .setMessage(StringValue.of(message));
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
        historyManager.saveHistory();
        return createJob(CliTokens.tokenize(line), session, resultDistributor);
    }

    private class ChannelResultDistributor implements ResultDistributor {
        private ActionRequest request;
        private Session session;

        public ChannelResultDistributor(ActionRequest request, Session session) {
            this.request = request;
            this.session = session;
        }

        @Override
        public void appendResult(ResultModel result) {
            //TODO 优化输出，适度合并
            List<ResultModel> resultModels = new ArrayList<ResultModel>();
            resultModels.add(result);

            ResponseStatus status;
            //StatusModel 为命令执行结束状态，最后一个结果消息
            if (result instanceof StatusModel) {
                StatusModel statusModel = (StatusModel) result;
                status = (statusModel.getStatusCode() == 0) ? ResponseStatus.SUCCEEDED : ResponseStatus.FAILED;
            } else {
                status = ResponseStatus.CONTINUOUS;
            }
            ActionResponse.Builder response = createResponse(request, session, status);
            sendResults(response, resultModels, request.getExecuteParams().getResultFormat());
        }

        @Override
        public void close() {

        }
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

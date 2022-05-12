package com.taobao.arthas.core.channel;

import com.alibaba.arthas.channel.client.ChannelClient;
import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.ConsoleData;
import com.alibaba.arthas.channel.proto.ConsoleParams;
import com.alibaba.arthas.channel.proto.ConsoleResult;
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
import com.google.protobuf.UnsafeByteOperations;
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
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.termd.core.function.Consumer;
import io.termd.core.function.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.taobao.arthas.core.channel.LocalConsoleClientManager.writeData;

/**
 * @author gongdewei 2020/8/16
 */
public class ChannelRequestHandler implements ChannelClient.RequestListener {

    private static final Logger logger = LoggerFactory.getLogger(ChannelRequestHandler.class);
    private static final String ONETIME_SESSION_KEY = "oneTimeSession";
    public static final int DEFAULT_EXEC_TIMEOUT = 30000;
    private final ScheduledExecutorService executorService;

    private EventLoopGroup group;
    private ChannelClient channelClient;
    private final SessionManager sessionManager;
    private final HistoryManager historyManager;
    private final InternalCommandManager commandManager;
    private final JobController jobController;

    private LocalConsoleClientManager consoleClientManager = new LocalConsoleClientManager();
    private PBResultConverter pbResultConverter = new PBResultConverter();

    public ChannelRequestHandler(ChannelClient channelClient, SessionManager sessionManager, HistoryManager historyManager) {
        this.channelClient = channelClient;
        this.sessionManager = sessionManager;
        this.historyManager = historyManager;
        commandManager = this.sessionManager.getCommandManager();
        jobController = this.sessionManager.getJobController();

        group = channelClient.getGroup();
        executorService = channelClient.getExecutorService();
    }

    @Override
    public void onRequest(ActionRequest request) {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("accept request: {}", request);
            }
            processRequest(request);
        } catch (Throwable e) {
            String msg = "process request error: " + e.toString();
            logger.error(msg, e);

            sendResponse(ActionResponse.newBuilder()
                    .setAgentId(request.getAgentId())
                    .setRequestId(request.getRequestId())
                    .setStatus(ResponseStatus.FAILED)
                    .setMessage(msg));
        }

    }

    private void processRequest(ActionRequest request) throws Exception {
        RequestAction action = request.getAction();

        // handle console actions
        switch (action) {
            case OPEN_CONSOLE:
                processOpenConsole(request);
                return;
            case CONSOLE_INPUT:
                processConsoleInput(request);
                return;
            case CLOSE_CONSOLE:
                processCloseConsole(request);
                return;
        }

        // handle init session (No session required)
        switch (action) {
            case INIT_SESSION:
                processInitSession(request);
                return;
            case UNRECOGNIZED:
                processUnrecognizedAction(request);
                return;
        }

        // try reuse session
        String sessionId = request.getSessionId();
        Session session = null;
        if (!StringUtils.isBlank(sessionId)) {
            session = sessionManager.getSession(sessionId);
            if (session == null) {
                throw new Exception("session not found: " + sessionId);
            }
        }
        //handle other command request actions
        switch (action) {
            case EXECUTE:
                processExec(request, session);
                return;
            case ASYNC_EXECUTE:
                processAsyncExec(request, session);
                return;
        }

        // The following actions is required a session
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
        }

    }

    private void processOpenConsole(final ActionRequest request) {
        final String consoleId = consoleClientManager.generateConsoleId();
        try {

            final BlockingQueue<ConsoleData> queue = new LinkedBlockingQueue<ConsoleData>(1000);

            LocalWebsocketClient websocketClient = new LocalWebsocketClient();
            Channel clientChannel = websocketClient.connectLocalServer(group, new Consumer<TextWebSocketFrame>() {
                @Override
                public void accept(TextWebSocketFrame frame) {
                    try {
                        //get tty output bytes
                        ByteBuf content = frame.content();
                        byte[] bytes = new byte[content.readableBytes()];
                        content.readBytes(bytes);

                        queue.put(ConsoleData.newBuilder()
                                .setDataType("tty")
                                .setDataBytes(UnsafeByteOperations.unsafeWrap(bytes))
                                .build());
                    } catch (Exception e) {
                        logger.error("process console data error", e);
                    }
                }
            });

            //send console data
            final ScheduledFuture<?> sendDataFuture = executorService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<ConsoleData> results = new ArrayList<ConsoleData>();
                        int len = 0;
                        while (true) {
                            ConsoleData consoleData = queue.poll();
                            if (consoleData != null) {
                                results.add(consoleData);
                                len += consoleData.getDataBytes().size();
                                if (len > 10240) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }

                        if (results.size() > 0) {
                            //send tty output to channel server
                            ActionResponse.Builder response = ActionResponse.newBuilder()
                                    .setAgentId(request.getAgentId())
                                    .setRequestId(consoleId)
                                    .setStatus(ResponseStatus.CONTINUOUS)
                                    .setConsoleResult(ConsoleResult.newBuilder()
                                            .addAllResults(results)
                                            .build());
                            try {
                                sendResponseWithThrows(response);
                            } catch (Exception e) {
                                logger.error("send console tty output error, consoleId: {}", consoleId, e);
                                consoleClientManager.closeConsoleClient(consoleId);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("send console data error", e);
                    }

                }
            }, 10, 10, TimeUnit.MILLISECONDS);


            consoleClientManager.addConsoleClient(consoleId, clientChannel);
            clientChannel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    ResponseStatus status = future.isSuccess() ? ResponseStatus.SUCCEEDED : ResponseStatus.FAILED;
                    consoleClientManager.closeConsoleClient(consoleId);

                    sendDataFuture.cancel(true);

                    //send final close event
                    ActionResponse.Builder response = ActionResponse.newBuilder()
                            .setAgentId(request.getAgentId())
                            .setRequestId(consoleId)
                            .setStatus(status)
                            .setMessage("console closed");
                    sendResponse(response);
                }
            });

            //send consoleId
            ActionResponse.Builder response = createResponse(request, ResponseStatus.SUCCEEDED);
            response.setConsoleResult(ConsoleResult.newBuilder()
                    .setConsoleId(consoleId)
                    .build());
            sendResponse(response);
        } catch (Throwable e) {
            logger.error("open console error", e);
            consoleClientManager.closeConsoleClient(consoleId);
            ActionResponse.Builder response = createResponse(request, ResponseStatus.FAILED);
            response.setMessage("open console error");
            sendResponse(response);
        }
    }

    private void processCloseConsole(ActionRequest request) {
        String consoleId = request.getConsoleParams().getConsoleId();
        consoleClientManager.closeConsoleClient(consoleId);
    }

    private void processConsoleInput(ActionRequest request) {

        ConsoleParams consoleParams = request.getConsoleParams();
        String consoleId = consoleParams.getConsoleId();
        String inputData = consoleParams.getInputData();
        Channel consoleClient = consoleClientManager.getConsoleClient(consoleId);
        if (consoleClient == null) {
            ActionResponse.Builder response = ActionResponse.newBuilder()
                    .setAgentId(request.getAgentId())
                    .setRequestId(consoleId)
                    .setStatus(ResponseStatus.FAILED)
                    .setMessage("console not found");
            sendResponse(response);
            return;
        }

        try {
            writeData(consoleClient, inputData);
        } catch (Throwable e) {
            logger.error("console write error, consoleId: {}", consoleId, e);
            ActionResponse.Builder response = ActionResponse.newBuilder()
                    .setAgentId(request.getAgentId())
                    .setRequestId(consoleId)
                    .setStatus(ResponseStatus.FAILED)
                    .setMessage("console write error");
            sendResponse(response);
            return;
        }
    }

    private void processExec(final ActionRequest request, Session session) {

        final ExecuteParams executeParams = request.getExecuteParams();
        String commandLine = executeParams.getCommandLine();

        if (session == null) {
            session = sessionManager.createSession();
            session.put(ONETIME_SESSION_KEY, true);
        }
        if (!session.tryLock()) {
            sendError(request, session, ResponseStatus.REFUSED, "Another command is executing.");
            return;
        }
        final PackingResultDistributor packingResultDistributor = new PackingResultDistributorImpl(session);

        Job job = null;
        int lock = session.getLock();
        try {
            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                logger.info("Another job is running, jobId: {}", foregroundJob.id());
                sendError(request, session, ResponseStatus.REFUSED, "Another job is running.");
                return;
            }

            job = this.createJob(commandLine, session, packingResultDistributor);
            session.setForegroundJob(job);
            //updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

            // exec job
            job.run();
        } catch (Throwable e) {
            logger.error("Exec command failed:" + e.getMessage() + ", command:" + commandLine, e);
            sendError(request, session, ResponseStatus.FAILED, "Exec command failed:" + e.getMessage());
            tryCloseOneTimeSession(session);
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

        final long startTime = System.currentTimeMillis();
        final int finalTimeout = timeout;
        final Job finalJob = job;
        final int checkingIntervalMs = 100;
        Runnable checkJobTask = new Runnable() {
            @Override
            public void run() {
                Session session = finalJob.getSession();
                boolean timeExpired = (System.currentTimeMillis() - startTime > finalTimeout);
                if (isJobCompleted(finalJob) || timeExpired) {
                    onJobCompletedOrExpired(session, timeExpired, finalJob, request, packingResultDistributor);
                } else {
                    // delay check again
                    executorService.schedule(this, checkingIntervalMs, TimeUnit.MILLISECONDS);
                }
            }
        };
        executorService.schedule(checkJobTask, checkingIntervalMs, TimeUnit.MILLISECONDS);
    }

    private boolean isJobCompleted(final Job finalJob) {
        switch (finalJob.status()) {
            case STOPPED:
            case TERMINATED:
                return true;
        }
        return false;
    }

    private void onJobCompletedOrExpired(Session session, boolean timeExpired, Job finalJob, ActionRequest request, PackingResultDistributor packingResultDistributor) {
        try {
            ExecuteParams executeParams = request.getExecuteParams();
            ActionResponse.Builder responseBuilder;
            if (timeExpired) {
                logger.warn("Job is exceeded time limit, force interrupt it, jobId: {}", finalJob.id());
                finalJob.interrupt();
                responseBuilder = createResponse(request, session, ResponseStatus.INTERRUPTED,
                        "The job is exceeded time limit, force interrupt");
            } else {
                responseBuilder = createResponse(request, session, ResponseStatus.SUCCEEDED);
            }

            List<ResultModel> resultModels = packingResultDistributor.getResults();
            sendResults(responseBuilder, resultModels, executeParams.getResultFormat());
        }catch (Throwable e) {
            logger.error("process job result failed", e);
        } finally {
            tryCloseOneTimeSession(session);
        }
    }

    private void tryCloseOneTimeSession(Session session) {
        if (session.get(ONETIME_SESSION_KEY) != null) {
            sessionManager.removeSession(session.getSessionId());
        }
    }

    private void processAsyncExec(ActionRequest request, Session session) {
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
                    .setSessionId(session.getSessionId())
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
//                .setSessionId(session.getSessionId())
//                .setConsumerId(StringValue.of(resultConsumer.getConsumerId()))
//                .setStatus(ResponseStatus.SUCCEEDED)
//                .build();
//        channelClient.submitResponse(actionResponse);
//    }

    private void processCloseSession(ActionRequest request, Session session) {
        sessionManager.removeSession(session.getSessionId());
        ActionResponse.Builder response = createResponse(request, session, ResponseStatus.SUCCEEDED);
        sendResponse(response);
    }

    private void processUnrecognizedAction(ActionRequest request) {
        ActionResponse.Builder response = ActionResponse.newBuilder()
                .setAgentId(request.getAgentId())
                .setRequestId(request.getRequestId())
                .setStatus(ResponseStatus.FAILED)
                .setMessage("unsupported action");
        sendResponse(response);
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
        try {
            channelClient.submitResponse(response.build());
        } catch (Exception e) {
            // Has been processed, ignore
        }
    }

    private void sendResponseWithThrows(ActionResponse.Builder response) throws Exception {
        channelClient.submitResponse(response.build());
    }

    private ActionResponse.Builder createResponse(ActionRequest request, ResponseStatus status) {
        return ActionResponse.newBuilder()
                .setAgentId(request.getAgentId())
                .setRequestId(request.getRequestId())
                .setStatus(status);
    }

    private ActionResponse.Builder createResponse(ActionRequest request, Session session, ResponseStatus status) {
        return ActionResponse.newBuilder()
                .setAgentId(request.getAgentId())
                .setRequestId(request.getRequestId())
                .setSessionId(session.getSessionId())
                .setStatus(status);
    }

    private ActionResponse.Builder createResponse(ActionRequest request, Session session, ResponseStatus status, String message) {
        return ActionResponse.newBuilder()
                .setAgentId(request.getAgentId())
                .setRequestId(request.getRequestId())
                .setSessionId(session.getSessionId())
                .setStatus(status)
                .setMessage(message);
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
            //TODO 优化输出，合并小结果？
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

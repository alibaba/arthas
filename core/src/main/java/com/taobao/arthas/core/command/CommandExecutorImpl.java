package com.taobao.arthas.core.command;

import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.model.*;
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
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.mcp.server.CommandExecutor;
import io.termd.core.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 命令执行器，用于执行Arthas命令，支持同步和异步执行
 */
public class CommandExecutorImpl implements CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutorImpl.class);
    private static final String ONETIME_SESSION_KEY = "oneTimeSession";
    
    private final SessionManager sessionManager;
    private final JobController jobController;
    private final InternalCommandManager commandManager;

    public CommandExecutorImpl(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.commandManager = sessionManager.getCommandManager();
        this.jobController = sessionManager.getJobController();
    }

    public Session getCurrentSession(String sessionId, boolean oneTimeIsAllowed) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            if (!oneTimeIsAllowed) {
                throw new SessionNotFoundException("SessionId is required for this operation");
            }

            Session session = sessionManager.createSession();
            if (session == null) {
                throw new SessionNotFoundException("Failed to create temporary session");
            }
            session.put(ONETIME_SESSION_KEY, new Object());
            logger.debug("Created one-time session {}", session.getSessionId());
            return session;
        } else {
            Session session = sessionManager.getSession(sessionId);
            if (session == null) {
                throw new SessionNotFoundException("Session not found: " + sessionId);
            }
            sessionManager.updateAccessTime(session);
            logger.debug("Using existing session {}", sessionId);
            return session;
        }
    }

    @Override
    public Map<String, Object> executeSync(String commandLine, long timeout, String sessionId) {
        Session session = null;
        boolean oneTimeAccess = false;
        
        try {
            session = getCurrentSession(sessionId, true);
            if (session.get(ONETIME_SESSION_KEY) != null) {
                oneTimeAccess = true;
            }

            PackingResultDistributorImpl resultDistributor = new PackingResultDistributorImpl(session);
            Job job = this.createJob(commandLine, session, resultDistributor);
            
            if (job == null) {
                logger.error("Failed to create job for command: {}", commandLine);
                return createErrorResult(commandLine, "Failed to create job");
            }

            job.run();
            boolean finished = waitForJob(job, (int) timeout);
            if (!finished) {
                logger.warn("Command timeout after {} ms: {}", timeout, commandLine);
                job.interrupt();
                return createTimeoutResult(commandLine, timeout);
            }

            Map<String, Object> result = new TreeMap<>();
            result.put("command", commandLine);
            result.put("success", true);
            result.put("sessionId", session.getSessionId());
            result.put("executionTime", System.currentTimeMillis());

            List<ResultModel> results = resultDistributor.getResults();
            if (results != null && !results.isEmpty()) {
                result.put("results", results);
                result.put("resultCount", results.size());
            } else {
                result.put("results", results);
                result.put("resultCount", 0);
            }

            return result;

        } catch (SessionNotFoundException e) {
            logger.error("Session error for command: {}", commandLine, e);
            return createErrorResult(commandLine, e.getMessage());
        } catch (Exception e) {
            logger.error("Error executing command: {}", commandLine, e);
            return createErrorResult(commandLine, "Error executing command: " + e.getMessage());
        } finally {
            if (oneTimeAccess && session != null) {
                try {
                    sessionManager.removeSession(session.getSessionId());
                    logger.debug("Destroyed one-time session {}", session.getSessionId());
                } catch (Exception e) {
                    logger.warn("Error removing one-time session", e);
                }
            }
        }
    }

    @Override
    public Map<String, Object> executeAsync(String commandLine, String sessionId) {
        Map<String, Object> result = new TreeMap<>();
        Session session = getCurrentSession(sessionId, false);
        if (!session.tryLock()) {
            logger.warn("Another command is executing in session: {}", session.getSessionId());
            return createErrorResult(commandLine, "Another command is executing");
        }
        int lock = session.getLock();

        try {
            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                logger.warn("Another job is running in session: {}, jobId: {}", session.getSessionId(), foregroundJob.id());
                session.unLock();
                return createErrorResult(commandLine, "Another job is running, jobId: " + foregroundJob.id());
            }

            Job job = this.createJob(commandLine, session, session.getResultDistributor());

            if (job == null) {
                logger.error("Failed to create job for command: {}", commandLine);
                session.unLock();
                return createErrorResult(commandLine, "Failed to create job");
            }

            session.setForegroundJob(job);
            updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

            job.run();

            result.put("success", true);
            result.put("command", commandLine);
            result.put("sessionId", session.getSessionId());
            result.put("jobId", job.id());
            result.put("jobStatus", job.status().toString());

            return result;

        } catch (SessionNotFoundException e) {
            logger.error("Session error for async command: {}", commandLine, e);
            return createErrorResult(commandLine, e.getMessage());
        } catch (Exception e) {
            logger.error("Error executing async command: {}", commandLine, e);
            return createErrorResult(commandLine, "Error executing async command: " + e.getMessage());
        }finally {
            if (session.getLock() == lock) {
                session.unLock();
            }
        }
    }

    @Override
    public Map<String, Object> pullResults(String sessionId, String consumerId) {
        if (StringUtils.isBlank(consumerId)) {
            return createErrorResult(null, "Consumer ID is null or empty");
        }

        try {
            Session session = getCurrentSession(sessionId, false);
            SharingResultDistributor resultDistributor = session.getResultDistributor();
            if (resultDistributor == null) {
                return createErrorResult(null, "No result distributor found for session: " + sessionId);
            }

            ResultConsumer consumer = resultDistributor.getConsumer(consumerId);
            if (consumer == null) {
                return createErrorResult(null, "Consumer not found: " + consumerId);
            }

            List<ResultModel> results = consumer.pollResults();

            if (results != null && results.isEmpty()) {
                logger.debug("Filtered empty result list for session: {}, consumer: {}", sessionId, consumerId);
                return null;
            }
            
            Map<String, Object> result = new TreeMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("consumerId", consumerId);
            result.put("results", results);

            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                result.put("jobId", foregroundJob.id());
                result.put("jobStatus", foregroundJob.status().toString());
            }

            return result;

        } catch (SessionNotFoundException e) {
            return createErrorResult(null, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> interruptJob(String sessionId) {
        try {
            Session session = getCurrentSession(sessionId, false);
            Job job = session.getForegroundJob();
            if (job == null) {
                return createErrorResult(null, "no foreground job is running");
            }
            job.interrupt();

            Map<String, Object> result = new TreeMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("jobId", job.id());
            result.put("jobStatus", job.status().toString());
            return result;

        } catch (SessionNotFoundException e) {
            return createErrorResult(null, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> createSession() {
        Session session = sessionManager.createSession();
        if (session == null) {
            return createErrorResult(null, "create api session failed");
        }

        SharingResultDistributorImpl resultDistributor = new SharingResultDistributorImpl(session);
        ResultConsumer resultConsumer = new ResultConsumerImpl();
        resultDistributor.addConsumer(resultConsumer);
        session.setResultDistributor(resultDistributor);

        resultDistributor.appendResult(new MessageModel("Welcome to arthas!"));

        WelcomeModel welcomeModel = new WelcomeModel();
        welcomeModel.setVersion(ArthasBanner.version());
        welcomeModel.setWiki(ArthasBanner.wiki());
        welcomeModel.setTutorials(ArthasBanner.tutorials());
        welcomeModel.setMainClass(PidUtils.mainClass());
        welcomeModel.setPid(PidUtils.currentPid());
        welcomeModel.setTime(DateUtils.getCurrentDateTime());
        resultDistributor.appendResult(welcomeModel);

        updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);

        Map<String, Object> result = new TreeMap<>();
        result.put("success", true);
        result.put("sessionId", session.getSessionId());
        result.put("consumerId", resultConsumer.getConsumerId());
        return result;
    }

    @Override
    public Map<String, Object> closeSession(String sessionId) {
        try {
            Session session = getCurrentSession(sessionId, false);

            if (session.isLocked()) {
                session.unLock();
            }
            
            sessionManager.removeSession(session.getSessionId());
            
            Map<String, Object> result = new TreeMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            return result;

        } catch (SessionNotFoundException e) {
            return createErrorResult(null, e.getMessage());
        }
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

    private Map<String, Object> createErrorResult(String commandLine, String errorMessage) {
        Map<String, Object> result = new TreeMap<>();
        result.put("success", false);
        result.put("error", errorMessage);
        if (commandLine != null) {
            result.put("command", commandLine);
        }
        return result;
    }

    private Map<String, Object> createTimeoutResult(String commandLine, long timeout) {
        Map<String, Object> result = new TreeMap<>();
        result.put("command", commandLine);
        result.put("success", false);
        result.put("error", "Command timeout after " + timeout + " ms");
        result.put("timeout", true);
        result.put("executionTime", System.currentTimeMillis());
        return result;
    }

    private void updateSessionInputStatus(Session session, InputStatus inputStatus) {
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            resultDistributor.appendResult(new InputStatusModel(inputStatus));
        }
    }

    private Job createJob(String line, Session session, ResultDistributor resultDistributor) {
        return createJob(CliTokens.tokenize(line), session, resultDistributor);
    }

    private synchronized Job createJob(List<CliToken> args, Session session, ResultDistributor resultDistributor) {
        Job job = jobController.createJob(commandManager, args, session, new JobHandler(session), new McpTerm(session), resultDistributor);
        return job;
    }

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String message) {
            super(message);
        }
    }

    private class JobHandler implements JobListener {
        private final Session session;

        public JobHandler(Session session) {
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
                session.unLock();
            }
        }

        @Override
        public void onTerminated(Job job) {
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
                session.unLock();
            }
        }

        @Override
        public void onSuspend(Job job) {
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
                session.unLock();
            }
        }
    }

    public static class McpTerm implements Term {
        private Session session;

        public McpTerm(Session session) {
            this.session = session;
        }

        @Override
        public Term resizehandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public String type() {
            return "mcp";
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

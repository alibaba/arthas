package com.taobao.arthas.core.command;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.impl.PackingResultDistributorImpl;
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
import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 命令执行器，用于执行Arthas命令
 */
public class CommandExecutorImpl implements CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutorImpl.class);
    
    private final SessionManager sessionManager;
    private final JobController jobController;
    private final InternalCommandManager commandManager;

    public CommandExecutorImpl(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.commandManager = sessionManager.getCommandManager();
        this.jobController = sessionManager.getJobController();
    }

    /**
     * 同步执行命令
     * @param commandLine 命令行
     * @param timeout 超时时间(毫秒)
     * @return 命令执行结果
     */
    @Override
    public Map<String, Object> execute(String commandLine, long timeout) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            logger.error("Command line is null or empty");
            return createErrorResult(commandLine, "Command line is null or empty");
        }

        List<CliToken> tokens = CliTokens.tokenize(commandLine);
        if (tokens.isEmpty()) {
            logger.error("No command found in command line: {}", commandLine);
            return createErrorResult(commandLine, "No command found in command line");
        }

        Map<String, Object> result = new TreeMap<>();
        Session session = null;
        PackingResultDistributorImpl resultDistributor = null;
        Job job = null;

        try {
            session = sessionManager.createSession();
            if (session == null) {
                logger.error("Failed to create session for command: {}", commandLine);
                return createErrorResult(commandLine, "Failed to create session");
            }
            resultDistributor = new PackingResultDistributorImpl(session);
            InMemoryTerm term = new InMemoryTerm();
            term.setSession(session);

            job = jobController.createJob(commandManager, tokens, session,
                    new JobHandle(), term, resultDistributor);
            
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

            result.put("command", commandLine);
            result.put("success", true);
            result.put("executionTime", System.currentTimeMillis());
            List<ResultModel> results = resultDistributor.getResults();

            if (results != null && !results.isEmpty()) {
                result.put("results", results);
                result.put("resultCount", results.size());
            } else {
                result.put("results", results);
                result.put("resultCount", 0);
            }
            String termOutput = term.getOutput();
            if (termOutput != null && !termOutput.trim().isEmpty()) {
                result.put("terminalOutput", termOutput);
            }

            logger.info("Command executed successfully: {}", commandLine);
            return result;

        } catch (Exception e) {
            logger.error("Error executing command: {}", commandLine, e);
            return createErrorResult(commandLine, "Error executing command: " + e.getMessage());
        } finally {
            if (session != null) {
                try {
                    sessionManager.removeSession(session.getSessionId());
                } catch (Exception e) {
                    logger.warn("Error removing session", e);
                }
            }
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
        result.put("command", commandLine);
        result.put("success", false);
        result.put("error", errorMessage);
        result.put("executionTime", System.currentTimeMillis());
        return result;
    }

    /**
     * 创建超时结果
     */
    private Map<String, Object> createTimeoutResult(String commandLine, long timeout) {
        Map<String, Object> result = new TreeMap<>();
        result.put("command", commandLine);
        result.put("success", false);
        result.put("error", "Command timeout after " + timeout + " ms");
        result.put("timeout", true);
        result.put("executionTime", System.currentTimeMillis());
        return result;
    }

    private static class JobHandle implements JobListener {
        private final CountDownLatch latch = new CountDownLatch(1);
        
        @Override
        public void onForeground(Job job) {
        }

        @Override
        public void onBackground(Job job) {
        }

        @Override
        public void onTerminated(Job job) {
            latch.countDown();
        }

        @Override
        public void onSuspend(Job job) {
        }
        
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }

    public static class InMemoryTerm implements Term {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private Session session;
        private volatile boolean closed = false;

        @Override
        public Term setSession(Session session) {
            this.session = session;
            return this;
        }

        public Session getSession() {
            return session;
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
        public long lastAccessedTime() {
            return System.currentTimeMillis();
        }

        @Override
        public String type() {
            return "inmemory";
        }

        @Override
        public int width() {
            return 120;
        }

        @Override
        public int height() {
            return 40;
        }

        @Override
        public Term resizehandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public Term stdinHandler(Handler<String> handler) {
            return this;
        }

        @Override
        public Term stdoutHandler(io.termd.core.function.Function<String, String> handler) {
            return this;
        }

        @Override
        public synchronized Term write(String data) {
            if (closed) {
                return this;
            }
            
            try {
                if (data != null) {
                    outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                logger.error("Error writing to terminal", e);
            }
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
        public synchronized void close() {
            closed = true;
            try {
                outputStream.close();
            } catch (Exception e) {
                logger.error("Error closing output stream", e);
            }
        }

        @Override
        public Term echo(String text) {
            return write(text);
        }

        public synchronized String getOutput() {
            try {
                return outputStream.toString(StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                logger.error("Error getting output", e);
                return "";
            }
        }

        public synchronized void clearOutput() {
            outputStream.reset();
        }
        
        public boolean isClosed() {
            return closed;
        }
    }
}

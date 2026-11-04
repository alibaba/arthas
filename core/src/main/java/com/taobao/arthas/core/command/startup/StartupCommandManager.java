package com.taobao.arthas.core.command.startup;

import static com.taobao.arthas.common.ArthasConstants.SUBJECT_KEY;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.CommandExecutorImpl;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.DateUtils;

/**
 * Starts commands configured for JVM startup and keeps each command in an isolated session.
 */
public class StartupCommandManager implements AutoCloseable {
    static final int MAX_COMMANDS = 32;
    static final int MAX_COMMAND_LENGTH = 8192;
    static final long STARTUP_BARRIER_TIMEOUT_SECONDS = 30;

    private static final Logger logger = LoggerFactory.getLogger(StartupCommandManager.class);
    private static final DateTimeFormatter RUN_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final SessionManager sessionManager;
    private final JobController jobController;
    private final InternalCommandManager commandManager;
    private final ExecutorService commandExecutor;
    private final File outputRoot;
    private final ConcurrentMap<String, CommandContext> activeCommands =
                    new ConcurrentHashMap<String, CommandContext>();
    private final List<StartupCommandResultWriter> resultWriters =
                    Collections.synchronizedList(new ArrayList<StartupCommandResultWriter>());
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile File runDirectory;

    public StartupCommandManager(SessionManager sessionManager, JobController jobController,
                    InternalCommandManager commandManager, ExecutorService commandExecutor, File outputRoot) {
        this.sessionManager = sessionManager;
        this.jobController = jobController;
        this.commandManager = commandManager;
        this.commandExecutor = commandExecutor;
        this.outputRoot = outputRoot;
    }

    public void start(File scriptFile) throws IOException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Startup commands have already been started");
        }

        List<String> commands = loadCommands(scriptFile.toPath());
        if (commands.isEmpty()) {
            logger.warn("No startup command found in script: {}", scriptFile);
            return;
        }

        runDirectory = createRunDirectory();
        writeManifest(scriptFile, commands);

        int submitted = 0;
        for (int i = 0; i < commands.size(); i++) {
            if (startCommand(i + 1, commands.get(i))) {
                submitted++;
            }
        }

        if (submitted > 0) {
            awaitStartupBarrier();
        }
        logger.info("Started {}/{} startup command(s), output directory: {}", submitted, commands.size(),
                        runDirectory.getAbsolutePath());
    }

    static List<String> loadCommands(Path script) throws IOException {
        if (!Files.isRegularFile(script) || !Files.isReadable(script)) {
            throw new IOException("Startup command script is not a readable file: " + script);
        }

        List<String> commands = new ArrayList<String>();
        try (BufferedReader reader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.length() > 0 && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                if (line.length() > MAX_COMMAND_LENGTH) {
                    throw new IOException("Startup command at line " + lineNumber + " exceeds "
                                    + MAX_COMMAND_LENGTH + " characters");
                }
                String command = line.trim();
                if (command.length() == 0 || command.startsWith("#")) {
                    continue;
                }
                commands.add(command);
                if (commands.size() > MAX_COMMANDS) {
                    throw new IOException("Startup command script contains more than " + MAX_COMMANDS + " commands");
                }
            }
        }
        return commands;
    }

    private File createRunDirectory() throws IOException {
        String pid = PidUtils.currentPid();
        String runId = RUN_ID_FORMATTER.format(LocalDateTime.now()) + "-"
                        + UUID.randomUUID().toString().substring(0, 8);
        Path pidDirectory = outputRoot.toPath().resolve("startup").resolve(pid);
        Path currentRunDirectory = pidDirectory.resolve(runId);
        Files.createDirectories(currentRunDirectory);
        return currentRunDirectory.toFile();
    }

    private void writeManifest(File scriptFile, List<String> commands) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        manifest.put("formatVersion", 1);
        manifest.put("pid", PidUtils.currentPid());
        manifest.put("createdAt", DateUtils.getCurrentDateTime());
        manifest.put("script", scriptFile.getCanonicalPath());
        manifest.put("outputDirectory", runDirectory.getCanonicalPath());

        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>(commands.size());
        for (int i = 0; i < commands.size(); i++) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("commandIndex", i + 1);
            entry.put("command", commands.get(i));
            entry.put("resultFile", resultFile(i + 1).getName());
            entries.add(entry);
        }
        manifest.put("commands", entries);

        byte[] json = JSON.toJSONString(manifest, JSONWriter.Feature.PrettyFormat).getBytes(StandardCharsets.UTF_8);
        Files.write(new File(runDirectory, "manifest.json").toPath(), json, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
        Files.write(runDirectory.getParentFile().toPath().resolve("latest"),
                        (runDirectory.getName() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private boolean startCommand(int commandIndex, String command) {
        StartupCommandResultWriter resultWriter = null;
        Session session = null;
        CommandContext context = null;
        try {
            resultWriter = new StartupCommandResultWriter(commandIndex, command, resultFile(commandIndex));
            resultWriters.add(resultWriter);
            resultWriter.appendLifecycle("commandSubmitted");

            session = sessionManager.createSession();
            session.put(Session.QUIET, Boolean.TRUE);
            session.put(Session.STARTUP_COMMAND, Boolean.TRUE);
            session.put(SUBJECT_KEY, new Subject());
            session.setUserId("startup");

            List<CliToken> tokens = CliTokens.tokenize(command);
            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("Startup command is empty");
            }

            context = new CommandContext(session, resultWriter);
            StartupJobListener listener = new StartupJobListener(context);
            Term term = new StartupTerm(session, resultWriter);
            Job job = jobController.createJob(commandManager, tokens, session, listener, term, resultWriter);
            context.setJob(job);
            activeCommands.put(session.getSessionId(), context);
            session.setForegroundJob(job);

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("jobId", job.id());
            data.put("sessionId", session.getSessionId());
            resultWriter.appendLifecycle("commandStarted", data);
            job.run(true);
            return true;
        } catch (Throwable e) {
            logger.error("Start startup command failed: {}", command, e);
            if (context != null) {
                activeCommands.remove(context.session.getSessionId());
            }
            if (session != null) {
                session.setForegroundJob(null);
                sessionManager.removeSession(session.getSessionId());
            }
            if (resultWriter != null) {
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("error", errorMessage(e));
                resultWriter.appendLifecycle("commandFailed", data);
                resultWriter.close();
            }
            return false;
        }
    }

    private void awaitStartupBarrier() {
        final CountDownLatch barrier = new CountDownLatch(1);
        try {
            commandExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    barrier.countDown();
                }
            });
            boolean ready = barrier.await(STARTUP_BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String event = ready ? "startupReady" : "startupTimeout";
            for (CommandContext context : activeCommands.values()) {
                context.resultWriter.appendLifecycle(event);
            }
            if (!ready) {
                logger.warn("Startup command barrier timed out after {} seconds", STARTUP_BARRIER_TIMEOUT_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            for (CommandContext context : activeCommands.values()) {
                context.resultWriter.appendLifecycle("startupInterrupted");
            }
            logger.warn("Interrupted while waiting for startup commands", e);
        } catch (Throwable e) {
            for (CommandContext context : activeCommands.values()) {
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("error", errorMessage(e));
                context.resultWriter.appendLifecycle("startupBarrierFailed", data);
            }
            logger.warn("Submit startup command barrier failed", e);
        }
    }

    private File resultFile(int commandIndex) {
        return new File(runDirectory, String.format("command-%03d.jsonl", commandIndex));
    }

    public File getRunDirectory() {
        return runDirectory;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        List<CommandContext> contexts = new ArrayList<CommandContext>(activeCommands.values());
        for (CommandContext context : contexts) {
            context.resultWriter.appendLifecycle("managerStopping");
            try {
                Job job = context.job;
                if (job != null) {
                    job.terminate();
                }
            } catch (Throwable e) {
                logger.warn("Terminate startup command failed: {}", context.session.getSessionId(), e);
                context.finish("commandFailed", errorMessage(e));
            }
        }

        synchronized (resultWriters) {
            for (StartupCommandResultWriter resultWriter : resultWriters) {
                resultWriter.close();
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            for (StartupCommandResultWriter resultWriter : resultWriters) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    break;
                }
                try {
                    resultWriter.awaitClosed(remaining, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static String errorMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.toString() : message;
    }

    private class StartupJobListener implements JobListener {
        private final CommandContext context;

        private StartupJobListener(CommandContext context) {
            this.context = context;
        }

        @Override
        public void onForeground(Job job) {
            context.session.setForegroundJob(job);
        }

        @Override
        public void onBackground(Job job) {
            context.session.setForegroundJob(job);
        }

        @Override
        public void onTerminated(Job job) {
            Integer exitCode = job.process().exitCode();
            String event = exitCode != null && exitCode.intValue() == 0 ? "commandCompleted" : "commandFailed";
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("jobId", job.id());
            data.put("exitCode", exitCode);
            context.finish(event, data);
        }

        @Override
        public void onSuspend(Job job) {
            context.resultWriter.appendLifecycle("commandSuspended");
        }
    }

    private class CommandContext {
        private final Session session;
        private final StartupCommandResultWriter resultWriter;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private volatile Job job;

        private CommandContext(Session session, StartupCommandResultWriter resultWriter) {
            this.session = session;
            this.resultWriter = resultWriter;
        }

        private void setJob(Job job) {
            this.job = job;
        }

        private void finish(String event, String error) {
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("error", error);
            finish(event, data);
        }

        private void finish(String event, Map<String, Object> data) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            resultWriter.appendLifecycle(event, data);
            resultWriter.close();
            activeCommands.remove(session.getSessionId());
            session.setForegroundJob(null);
            sessionManager.removeSession(session.getSessionId());
        }
    }

    private static class StartupTerm extends CommandExecutorImpl.McpTerm {
        private final StartupCommandResultWriter resultWriter;

        private StartupTerm(Session session, StartupCommandResultWriter resultWriter) {
            super(session);
            this.resultWriter = resultWriter;
        }

        @Override
        public String type() {
            return "startup";
        }

        @Override
        public Term write(String data) {
            resultWriter.appendStdout(data);
            return this;
        }

        @Override
        public Term echo(String text) {
            resultWriter.appendStdout(text);
            return this;
        }
    }
}

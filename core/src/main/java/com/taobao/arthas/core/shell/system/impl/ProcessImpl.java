package com.taobao.arthas.core.shell.system.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.command.basic1000.HelpCommand;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.impl.TermResultDistributorImpl;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.internal.CloseFunction;
import com.taobao.arthas.core.shell.command.internal.StatisticsFunction;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.ProcessAware;
import com.taobao.arthas.core.shell.term.Tty;
import com.taobao.middleware.cli.CLIException;
import com.taobao.middleware.cli.CommandLine;
import io.termd.core.function.Function;

import java.lang.instrument.ClassFileTransformer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author beiwei30 on 10/11/2016.
 * @author gongdewei 2020-03-26
 */
public class ProcessImpl implements Process {

    private static final Logger logger = LoggerFactory.getLogger(ProcessImpl.class);

    private Command commandContext;
    private Handler<CommandProcess> handler;
    private List<CliToken> args;
    private Tty tty;
    private Session session;
    private Handler<Void> interruptHandler;
    private Handler<Void> suspendHandler;
    private Handler<Void> resumeHandler;
    private Handler<Void> endHandler;
    private Handler<Void> backgroundHandler;
    private Handler<Void> foregroundHandler;
    private Handler<Integer> terminatedHandler;
    private boolean foreground;
    private ExecStatus processStatus;
    private boolean processForeground;
    private Handler<String> stdinHandler;
    private Handler<Void> resizeHandler;
    private Integer exitCode;
    private CommandProcessImpl process;
    private Date startTime;
    private ProcessOutput processOutput;
    private int jobId;
    private ResultDistributor resultDistributor;

    public ProcessImpl(Command commandContext, List<CliToken> args, Handler<CommandProcess> handler,
                       ProcessOutput processOutput, ResultDistributor resultDistributor) {
        this.commandContext = commandContext;
        this.handler = handler;
        this.args = args;
        this.resultDistributor = resultDistributor;
        this.processStatus = ExecStatus.READY;
        this.processOutput = processOutput;
    }

    @Override
    public Integer exitCode() {
        return exitCode;
    }

    @Override
    public ExecStatus status() {
        return processStatus;
    }

    @Override
    public synchronized Process setTty(Tty tty) {
        this.tty = tty;
        return this;
    }

    @Override
    public synchronized Tty getTty() {
        return tty;
    }

    @Override
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    @Override
    public synchronized Process setSession(Session session) {
        this.session = session;
        return this;
    }

    @Override
    public synchronized Session getSession() {
        return session;
    }

    @Override
    public int times() {
        return process.times().get();
    }

    public Date startTime() {
        return startTime;
    }

    @Override
    public String cacheLocation() {
        if (processOutput != null) {
            return processOutput.cacheLocation;
        }
        return null;
    }

    @Override
    public Process terminatedHandler(Handler<Integer> handler) {
        terminatedHandler = handler;
        return this;
    }

    @Override
    public boolean interrupt() {
        return interrupt(null);
    }

    @Override
    public boolean interrupt(final Handler<Void> completionHandler) {
        if (processStatus == ExecStatus.RUNNING || processStatus == ExecStatus.STOPPED) {
            final Handler<Void> handler = interruptHandler;
            try {
                if (handler != null) {
                    handler.handle(null);
                }
            } finally {
                if (completionHandler != null) {
                    completionHandler.handle(null);
                }
            }
            return handler != null;
        } else {
            throw new IllegalStateException("Cannot interrupt process in " + processStatus + " state");
        }
    }

    @Override
    public void resume() {
        resume(true);
    }

    @Override
    public void resume(boolean foreground) {
        resume(foreground, null);
    }

    @Override
    public void resume(Handler<Void> completionHandler) {
        resume(true, completionHandler);
    }

    @Override
    public synchronized void resume(boolean fg, Handler<Void> completionHandler) {
        if (processStatus == ExecStatus.STOPPED) {
            updateStatus(ExecStatus.RUNNING, null, fg, resumeHandler, terminatedHandler, completionHandler);
            if (process != null) {
                process.resume();
            }
        } else {
            throw new IllegalStateException("Cannot resume process in " + processStatus + " state");
        }
    }

    @Override
    public void suspend() {
        suspend(null);
    }

    @Override
    public synchronized void suspend(Handler<Void> completionHandler) {
        if (processStatus == ExecStatus.RUNNING) {
            updateStatus(ExecStatus.STOPPED, null, false, suspendHandler, terminatedHandler, completionHandler);
            if (process != null) {
                process.suspend();
            }
        } else {
            throw new IllegalStateException("Cannot suspend process in " + processStatus + " state");
        }
    }

    @Override
    public void toBackground() {
        toBackground(null);
    }

    @Override
    public void toBackground(Handler<Void> completionHandler) {
        if (processStatus == ExecStatus.RUNNING) {
            if (processForeground) {
                updateStatus(ExecStatus.RUNNING, null, false, backgroundHandler, terminatedHandler, completionHandler);
            }
        } else {
            throw new IllegalStateException("Cannot set to background a process in " + processStatus + " state");
        }
    }

    @Override
    public void toForeground() {
        toForeground(null);
    }

    @Override
    public void toForeground(Handler<Void> completionHandler) {
        if (processStatus == ExecStatus.RUNNING) {
            if (!processForeground) {
                updateStatus(ExecStatus.RUNNING, null, true, foregroundHandler, terminatedHandler, completionHandler);
            }
        } else {
            throw new IllegalStateException("Cannot set to foreground a process in " + processStatus + " state");
        }
    }

    @Override
    public void terminate() {
        terminate(null);
    }

    @Override
    public void terminate(Handler<Void> completionHandler) {
        if (!terminate(-10, completionHandler, null)) {
            throw new IllegalStateException("Cannot terminate terminated process");
        }
    }

    private synchronized boolean terminate(int exitCode, Handler<Void> completionHandler, String message) {
        if (processStatus != ExecStatus.TERMINATED) {
            //add status message
            this.appendResult(new StatusModel(exitCode, message));
            if (process != null) {
                processOutput.close();
            }
            updateStatus(ExecStatus.TERMINATED, exitCode, false, endHandler, terminatedHandler, completionHandler);
            if (process != null) {
                process.unregister();
            }
            return true;
        } else {
            return false;
        }
    }

    private void appendResult(ResultModel result) {
        result.setJobId(jobId);
        if (resultDistributor != null) {
            resultDistributor.appendResult(result);
        }
    }

    private void updateStatus(ExecStatus statusUpdate, Integer exitCodeUpdate, boolean foregroundUpdate,
                              Handler<Void> handler, Handler<Integer> terminatedHandler,
                              Handler<Void> completionHandler) {
        processStatus = statusUpdate;
        exitCode = exitCodeUpdate;
        if (!foregroundUpdate) {
            if (processForeground) {
                processForeground = false;
                if (stdinHandler != null) {
                    tty.stdinHandler(null);
                }
                if (resizeHandler != null) {
                    tty.resizehandler(null);
                }
            }
        } else {
            if (!processForeground) {
                processForeground = true;
                if (stdinHandler != null) {
                    tty.stdinHandler(stdinHandler);
                }
                if (resizeHandler != null) {
                    tty.resizehandler(resizeHandler);
                }
            }
        }

        foreground = foregroundUpdate;
        try {
            if (handler != null) {
                handler.handle(null);
            }
        } finally {
            if (completionHandler != null) {
                completionHandler.handle(null);
            }
            if (terminatedHandler != null && statusUpdate == ExecStatus.TERMINATED) {
                terminatedHandler.handle(exitCodeUpdate);
            }
        }
    }

    @Override
    public void run() {
        run(true);
    }

    @Override
    public synchronized void run(boolean fg) {
        if (processStatus != ExecStatus.READY) {
            throw new IllegalStateException("Cannot run proces in " + processStatus + " state");
        }

        processStatus = ExecStatus.RUNNING;
        processForeground = fg;
        foreground = fg;
        startTime = new Date();

        // Make a local copy
        final Tty tty = this.tty;
        if (tty == null) {
            throw new IllegalStateException("Cannot execute process without a TTY set");
        }

        process = new CommandProcessImpl(this, tty);
        if (resultDistributor == null) {
            resultDistributor = new TermResultDistributorImpl(process, ArthasBootstrap.getInstance().getResultViewResolver());
        }

        final List<String> args2 = new LinkedList<String>();
        for (CliToken arg : args) {
            if (arg.isText()) {
                args2.add(arg.value());
            }
        }

        CommandLine cl = null;
        try {
            if (commandContext.cli() != null) {
                if (commandContext.cli().parse(args2, false).isAskingForHelp()) {
                    appendResult(new HelpCommand().createHelpDetailModel(commandContext));
                    terminate();
                    return;
                }

                cl = commandContext.cli().parse(args2);
                process.setArgs2(args2);
                process.setCommandLine(cl);
            }
        } catch (CLIException e) {
            terminate(-10, null, e.getMessage());
            return;
        }

        if (cacheLocation() != null) {
            process.echoTips("job id  : " + this.jobId + "\n");
            process.echoTips("cache location  : " + cacheLocation() + "\n");
        }
        Runnable task = new CommandProcessTask(process);
        ArthasBootstrap.getInstance().execute(task);
    }

    private class CommandProcessTask implements Runnable {

        private CommandProcess process;

        public CommandProcessTask(CommandProcess process) {
            this.process = process;
        }

        @Override
        public void run() {
            try {
                handler.handle(process);
            } catch (Throwable t) {
                logger.error("Error during processing the command:", t);
                process.end(1, "Error during processing the command: " + t.getClass().getName() + ", message:" + t.getMessage()
                        + ", please check $HOME/logs/arthas/arthas.log for more details." );
            }
        }
    }

    private class CommandProcessImpl implements CommandProcess {

        private final Process process;
        private final Tty tty;
        private List<String> args2;
        private CommandLine commandLine;
        private AtomicInteger times = new AtomicInteger();
        private AdviceListener listener = null;
        private ClassFileTransformer transformer;

        public CommandProcessImpl(Process process, Tty tty) {
            this.process = process;
            this.tty = tty;
        }

        @Override
        public List<CliToken> argsTokens() {
            return args;
        }

        @Override
        public List<String> args() {
            return args2;
        }

        @Override
        public String type() {
            return tty.type();
        }

        @Override
        public boolean isForeground() {
            return foreground;
        }

        @Override
        public int width() {
            return tty.width();
        }

        @Override
        public int height() {
            return tty.height();
        }

        @Override
        public CommandLine commandLine() {
            return commandLine;
        }

        @Override
        public Session session() {
            return session;
        }

        @Override
        public AtomicInteger times() {
            return times;
        }

        public void setArgs2(List<String> args2) {
            this.args2 = args2;
        }

        public void setCommandLine(CommandLine commandLine) {
            this.commandLine = commandLine;
        }

        @Override
        public CommandProcess stdinHandler(Handler<String> handler) {
            stdinHandler = handler;
            if (processForeground && stdinHandler != null) {
                tty.stdinHandler(stdinHandler);
            }
            return this;
        }

        @Override
        public CommandProcess write(String data) {
            synchronized (ProcessImpl.this) {
                if (processStatus != ExecStatus.RUNNING) {
                    throw new IllegalStateException(
                            "Cannot write to standard output when " + status().name().toLowerCase());
                }
            }
            processOutput.write(data);
            return this;
        }

        @Override
        public void echoTips(String tips) {
            processOutput.term.write(tips);
        }

        @Override
        public String cacheLocation() {
            return ProcessImpl.this.cacheLocation();
        }

        @Override
        public CommandProcess resizehandler(Handler<Void> handler) {
            resizeHandler = handler;
            tty.resizehandler(resizeHandler);
            return this;
        }

        @Override
        public CommandProcess interruptHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                interruptHandler = handler;
            }
            return this;
        }

        @Override
        public CommandProcess suspendHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                suspendHandler = handler;
            }
            return this;
        }

        @Override
        public CommandProcess resumeHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                resumeHandler = handler;
            }
            return this;
        }

        @Override
        public CommandProcess endHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                endHandler = handler;
            }
            return this;
        }

        @Override
        public CommandProcess backgroundHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                backgroundHandler = handler;
            }
            return this;
        }

        @Override
        public CommandProcess foregroundHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                foregroundHandler = handler;
            }
            return this;
        }

        @Override
        public void register(AdviceListener adviceListener, ClassFileTransformer transformer) {
            if (adviceListener instanceof ProcessAware) {
                ProcessAware processAware = (ProcessAware) adviceListener;
                // listener 有可能是其它 command 创建的
                if(processAware.getProcess() == null) {
                    processAware.setProcess(this.process);
                }
            }
            this.listener = adviceListener;
            AdviceWeaver.reg(listener);
            
            this.transformer = transformer;
        }

        @Override
        public void unregister() {
            if (transformer != null) {
                ArthasBootstrap.getInstance().getTransformerManager().removeTransformer(transformer);
            }
            
            if (listener instanceof ProcessAware) {
                // listener有可能其它 command 创建的，所以不能unRge
                if (this.process.equals(((ProcessAware) listener).getProcess())) {
                    AdviceWeaver.unReg(listener);
                }
            } else {
                AdviceWeaver.unReg(listener);
            }
        }

        @Override
        public void resume() {
//            if (suspendedListener != null) {
//                AdviceWeaver.resume(suspendedListener);
//                suspendedListener = null;
//            }
        }

        @Override
        public void suspend() {
//            if (this.enhanceLock >= 0) {
//                suspendedListener = AdviceWeaver.suspend(enhanceLock);
//            }
        }

        @Override
        public void end() {
            end(0);
        }

        @Override
        public void end(int statusCode) {
            end(statusCode, null);
        }

        @Override
        public void end(int statusCode, String message) {
            terminate(statusCode, null, message);
        }

        @Override
        public boolean isRunning() {
            return processStatus == ExecStatus.RUNNING;
        }

        @Override
        public void appendResult(ResultModel result) {
            if (processStatus != ExecStatus.RUNNING) {
                throw new IllegalStateException(
                        "Cannot write to standard output when " + status().name().toLowerCase());
            }
            ProcessImpl.this.appendResult(result);
        }
    }

    static class ProcessOutput {

        private List<Function<String, String>> stdoutHandlerChain;
        private StatisticsFunction statisticsHandler = null;
        private List<Function<String, String>> flushHandlerChain = null;
        private String cacheLocation;
        private Tty term;

        public ProcessOutput(List<Function<String, String>> stdoutHandlerChain, String cacheLocation, Tty term) {
            // this.stdoutHandlerChain = stdoutHandlerChain;

            int i = 0;
            for (; i < stdoutHandlerChain.size(); i++) {
                if (stdoutHandlerChain.get(i) instanceof StatisticsFunction) {
                    break;
                }
            }
            if (i < stdoutHandlerChain.size()) {
                this.stdoutHandlerChain = stdoutHandlerChain.subList(0, i + 1);
                this.statisticsHandler = (StatisticsFunction) stdoutHandlerChain.get(i);
                if (i < stdoutHandlerChain.size() - 1) {
                    flushHandlerChain = stdoutHandlerChain.subList(i + 1, stdoutHandlerChain.size());
                }
            } else {
                this.stdoutHandlerChain = stdoutHandlerChain;
            }

            this.cacheLocation = cacheLocation;
            this.term = term;
        }

        private void write(String data) {
            if (stdoutHandlerChain != null) {
                //hotspot, reduce memory fragment (foreach/iterator)
                int size = stdoutHandlerChain.size();
                for (int i = 0; i < size; i++) {
                    Function<String, String> function = stdoutHandlerChain.get(i);
                    data = function.apply(data);
                }
            }
        }

        private void close() {
            if (statisticsHandler != null && flushHandlerChain != null) {
                String data = statisticsHandler.result();

                for (Function<String, String> function : flushHandlerChain) {
                    data = function.apply(data);
                    if (function instanceof StatisticsFunction) {
                        data = ((StatisticsFunction) function).result();
                    }
                }
            }

            if (stdoutHandlerChain != null) {
                for (Function<String, String> function : stdoutHandlerChain) {
                    if (function instanceof CloseFunction) {
                        ((CloseFunction) function).close();
                    }
                }
            }
        }
    }
}

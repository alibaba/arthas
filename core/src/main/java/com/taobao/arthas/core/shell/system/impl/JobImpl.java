package com.taobao.arthas.core.shell.system.impl;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.shell.ShellForegroundUpdateHandler;
import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.Process;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JobImpl implements Job {

    final int id;
    final JobControllerImpl controller;
    final Process process;
    final String line;
    private volatile ExecStatus actualStatus; // Used internally for testing only
    volatile long lastStopped; // When the job was last stopped
    volatile ShellImpl shell;
    volatile Handler<ExecStatus> statusUpdateHandler;
    volatile Date timeoutDate;
    final Future<Void> terminateFuture;
    final AtomicBoolean runInBackground;
    final Handler<Job> foregroundUpdatedHandler;

    JobImpl(int id, final JobControllerImpl controller, Process process, String line, boolean runInBackground,
            ShellImpl shell) {
        this.id = id;
        this.controller = controller;
        this.process = process;
        this.line = line;
        this.terminateFuture = Future.future();
        this.runInBackground = new AtomicBoolean(runInBackground);
        this.shell = shell;
        this.foregroundUpdatedHandler = new ShellForegroundUpdateHandler(shell);
        process.terminatedHandler(new TerminatedHandler(controller));
    }

    public ExecStatus actualStatus() {
        return actualStatus;
    }

    @Override
    public boolean interrupt() {
        return process.interrupt();
    }

    @Override
    public Job resume() {
        return resume(true);
    }

    @Override
    public Date timeoutDate() {
        return timeoutDate;
    }

    @Override
    public void setTimeoutDate(Date date) {
        this.timeoutDate = date;
    }

    @Override
    public Session getSession() {
        return shell.session();
    }

    @Override
    public Job resume(boolean foreground) {
        try {
            process.resume(new ResumeHandler());
        } catch (IllegalStateException ignore) {

        }

        runInBackground.set(!foreground);

        if (foreground) {
            if (foregroundUpdatedHandler != null) {
                foregroundUpdatedHandler.handle(this);
            }
        }
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(process.status());
        }

        if (foreground) {
            shell.setForegroundJob(this);
        } else {
            shell.setForegroundJob(null);
        }
        return this;
    }

    @Override
    public Job suspend() {
        try {
            process.suspend(new SuspendHandler());
        } catch (IllegalStateException ignore) {
            return this;
        }
        if (!runInBackground.get() && foregroundUpdatedHandler != null) {
            foregroundUpdatedHandler.handle(null);
        }
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(process.status());
        }

        shell.setForegroundJob(null);
        return this;
    }

    @Override
    public void terminate() {
        try {
            process.terminate();
        } catch (IllegalStateException ignore) {
            // Process already terminated, likely by itself
        }
        controller.removeJob(this.id);
    }

    @Override
    public Process process() {
        return process;
    }

    public ExecStatus status() {
        return process.status();
    }

    public String line() {
        return line;
    }

    @Override
    public Job toBackground() {
        if (!this.runInBackground.get()) {
            // run in foreground mode
            if (runInBackground.compareAndSet(false, true)) {
                process.toBackground();
                if (statusUpdateHandler != null) {
                    statusUpdateHandler.handle(process.status());
                }
            }
        }

        shell.setForegroundJob(null);
        return this;
    }

    @Override
    public Job toForeground() {
        if (this.runInBackground.get()) {
            if (runInBackground.compareAndSet(true, false)) {
                if (foregroundUpdatedHandler != null) {
                    foregroundUpdatedHandler.handle(this);
                }
                process.toForeground();
                if (statusUpdateHandler != null) {
                    statusUpdateHandler.handle(process.status());
                }

                shell.setForegroundJob(this);
            }
        }

        return this;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Job run() {
        return run(!runInBackground.get());
    }

    @Override
    public Job run(boolean foreground) {
        if (foreground && foregroundUpdatedHandler != null) {
            foregroundUpdatedHandler.handle(this);
        }

        actualStatus = ExecStatus.RUNNING;
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(ExecStatus.RUNNING);
        }
        process.setTty(shell.term());
        process.setSession(shell.session());
        process.run(foreground);

        if (!foreground && foregroundUpdatedHandler != null) {
            foregroundUpdatedHandler.handle(null);
        }

        if (foreground) {
            shell.setForegroundJob(this);
        } else {
            shell.setForegroundJob(null);
        }
        return this;
    }

    private class TerminatedHandler implements Handler<Integer> {

        private final JobControllerImpl controller;

        public TerminatedHandler(JobControllerImpl controller) {
            this.controller = controller;
        }

        @Override
        public void handle(Integer exitCode) {
            if (!runInBackground.get() && actualStatus.equals(ExecStatus.RUNNING)) {
                // 只有前台在运行的任务，才需要调用foregroundUpdateHandler
                if (foregroundUpdatedHandler != null) {
                    foregroundUpdatedHandler.handle(null);
                }
            }
            controller.removeJob(JobImpl.this.id);
            if (statusUpdateHandler != null) {
                statusUpdateHandler.handle(ExecStatus.TERMINATED);
            }
            terminateFuture.complete();

        }
    }

    private class ResumeHandler implements Handler<Void> {

        @Override
        public void handle(Void event) {
            actualStatus = ExecStatus.RUNNING;
        }
    }

    private class SuspendHandler implements Handler<Void> {

        @Override
        public void handle(Void event) {
            actualStatus = ExecStatus.STOPPED;
        }
    }
}

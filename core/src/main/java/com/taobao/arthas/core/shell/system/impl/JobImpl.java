package com.taobao.arthas.core.shell.system.impl;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.Process;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-05-14
 * @author gongdewei 2020-03-23
 */
public class JobImpl implements Job {

    final int id;
    final JobControllerImpl controller;
    final Process process;
    final String line;
    private volatile Session session;
    private volatile ExecStatus actualStatus; // Used internally for testing only
    volatile long lastStopped; // When the job was last stopped
    volatile JobListener jobHandler;
    volatile Handler<ExecStatus> statusUpdateHandler;
    volatile Date timeoutDate;
    final Future<Void> terminateFuture;
    final AtomicBoolean runInBackground;
    //final Handler<Job> foregroundUpdatedHandler;

    JobImpl(int id, final JobControllerImpl controller, Process process, String line, boolean runInBackground,
            Session session, JobListener jobHandler) {
        this.id = id;
        this.controller = controller;
        this.process = process;
        this.line = line;
        this.session = session;
        this.terminateFuture = Future.future();
        this.runInBackground = new AtomicBoolean(runInBackground);
        this.jobHandler = jobHandler;
        if (jobHandler == null) {
            throw new IllegalArgumentException("JobListener is required");
        }
        //this.foregroundUpdatedHandler = new ShellForegroundUpdateHandler(shell);
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
        return session;
    }

    @Override
    public Job resume(boolean foreground) {
        try {
            process.resume(foreground, new ResumeHandler());
        } catch (IllegalStateException ignore) {

        }

        runInBackground.set(!foreground);

//        if (foreground) {
//            if (foregroundUpdatedHandler != null) {
//                foregroundUpdatedHandler.handle(this);
//            }
//        }
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(process.status());
        }

        if (this.status() == ExecStatus.RUNNING) {
            if (foreground) {
                jobHandler.onForeground(this);
            } else {
                jobHandler.onBackground(this);
            }
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
//        if (!runInBackground.get() && foregroundUpdatedHandler != null) {
//            foregroundUpdatedHandler.handle(null);
//        }
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(process.status());
        }

//        shell.setForegroundJob(null);
        jobHandler.onSuspend(this);
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
    public boolean isRunInBackground() {
        return runInBackground.get();
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
                jobHandler.onBackground(this);
            }
        }

//        shell.setForegroundJob(null);
//        jobHandler.onBackground(this);
        return this;
    }

    @Override
    public Job toForeground() {
        if (this.runInBackground.get()) {
            if (runInBackground.compareAndSet(true, false)) {
//                if (foregroundUpdatedHandler != null) {
//                    foregroundUpdatedHandler.handle(this);
//                }
                process.toForeground();
                if (statusUpdateHandler != null) {
                    statusUpdateHandler.handle(process.status());
                }

//                shell.setForegroundJob(this);
                jobHandler.onForeground(this);
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
//        if (foreground && foregroundUpdatedHandler != null) {
//            foregroundUpdatedHandler.handle(this);
//        }

        actualStatus = ExecStatus.RUNNING;
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(ExecStatus.RUNNING);
        }
        //set process's tty in JobControllerImpl.createCommandProcess
        //process.setTty(shell.term());
        process.setSession(this.session);
        process.run(foreground);

//        if (!foreground && foregroundUpdatedHandler != null) {
//            foregroundUpdatedHandler.handle(null);
//        }
//
//        if (foreground) {
//            shell.setForegroundJob(this);
//        } else {
//            shell.setForegroundJob(null);
//        }
        if (this.status() == ExecStatus.RUNNING) {
            if (foreground) {
                jobHandler.onForeground(this);
            } else {
                jobHandler.onBackground(this);
            }
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
//            if (!runInBackground.get() && actualStatus.equals(ExecStatus.RUNNING)) {
                // 只有前台在运行的任务，才需要调用foregroundUpdateHandler
//                if (foregroundUpdatedHandler != null) {
//                    foregroundUpdatedHandler.handle(null);
//                }
//            }
            jobHandler.onTerminated(JobImpl.this);
            controller.removeJob(JobImpl.this.id);
            if (statusUpdateHandler != null) {
                statusUpdateHandler.handle(ExecStatus.TERMINATED);
            }
            terminateFuture.complete();

            // save command history (move to JobControllerImpl.ShellJobHandler.onTerminated)
//            Term term = shell.term();
//            if (term instanceof TermImpl) {
//                List<int[]> history = ((TermImpl) term).getReadline().getHistory();
//                FileUtils.saveCommandHistory(history, new File(Constants.CMD_HISTORY_FILE));
//            }
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

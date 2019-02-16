package com.taobao.arthas.core.shell.impl;

import com.taobao.arthas.core.shell.Shell;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.shell.CloseHandler;
import com.taobao.arthas.core.shell.handlers.shell.CommandManagerCompletionHandler;
import com.taobao.arthas.core.shell.handlers.shell.FutureHandler;
import com.taobao.arthas.core.shell.handlers.shell.InterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.ShellLineHandler;
import com.taobao.arthas.core.shell.handlers.shell.SuspendHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.impl.SessionImpl;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

import java.lang.instrument.Instrumentation;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The shell session as seen from the shell server perspective.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellImpl implements Shell {

    private static final Logger logger = LogUtil.getArthasLogger();

    private JobControllerImpl jobController;
    final String id;
    final Future<Void> closedFuture;
    private InternalCommandManager commandManager;
    private Session session = new SessionImpl();
    private Term term;
    private String welcome;
    private Job currentForegroundJob;

    public ShellImpl(ShellServer server, Term term, InternalCommandManager commandManager,
            Instrumentation instrumentation, int pid, JobControllerImpl jobController) {
        session.put(Session.COMMAND_MANAGER, commandManager);
        session.put(Session.INSTRUMENTATION, instrumentation);
        session.put(Session.PID, pid);
        session.put(Session.SERVER, server);
        session.put(Session.TTY, term);
        this.id = UUID.randomUUID().toString();
        session.put(Session.ID, id);
        this.commandManager = commandManager;
        this.closedFuture = Future.future();
        this.term = term;
        this.jobController = jobController;

        if (term != null) {
            term.setSession(session);
        }
    }

    public JobController jobController() {
        return jobController;
    }

    public Set<Job> jobs() {
        return jobController.jobs();
    }

    @Override
    public synchronized Job createJob(List<CliToken> args) {
        Job job = jobController.createJob(commandManager, args, this);
        return job;
    }

    @Override
    public Job createJob(String line) {
        return createJob(CliTokens.tokenize(line));
    }

    @Override
    public Session session() {
        return session;
    }

    public Term term() {
        return term;
    }

    public FutureHandler closedFutureHandler() {
        return new FutureHandler(closedFuture);
    }

    public long lastAccessedTime() {
        return term.lastAccessedTime();
    }

    public void setWelcome(String welcome) {
        this.welcome = welcome;
    }

    public ShellImpl init() {
        term.interruptHandler(new InterruptHandler(this));
        term.suspendHandler(new SuspendHandler(this));
        term.closeHandler(new CloseHandler(this));

        if (welcome != null && welcome.length() > 0) {
            term.write(welcome + "\n");
        }
        return this;
    }

    public String statusLine(Job job, ExecStatus status) {
        StringBuilder sb = new StringBuilder("[").append(job.id()).append("]");
        if (this.session().equals(job.getSession())) {
            sb.append("*");
        }
        sb.append("\n");
        sb.append("       ").append(Character.toUpperCase(status.name().charAt(0)))
                .append(status.name().substring(1).toLowerCase());
        sb.append("           ").append(job.line()).append("\n");
        sb.append("       execution count : ").append(job.process().times()).append("\n");
        sb.append("       start time      : ").append(job.process().startTime()).append("\n");
        String cacheLocation = job.process().cacheLocation();
        if (cacheLocation != null) {
            sb.append("       cache location  : ").append(cacheLocation).append("\n");
        }
        Date timeoutDate = job.timeoutDate();
        if (timeoutDate != null) {
            sb.append("       timeout date    : ").append(timeoutDate).append("\n");
        }
        sb.append("       session         : ").append(job.getSession().getSessionId()).append(
                session.equals(job.getSession()) ? " (current)" : "").append("\n");
        return sb.toString();
    }

    public void readline() {
        term.readline(Constants.DEFAULT_PROMPT, new ShellLineHandler(this),
                new CommandManagerCompletionHandler(commandManager));
    }

    public void close(String reason) {
        if (term != null) {
            try {
                term.write("session (" + session.getSessionId() + ") is closed because " + reason + "\n");
            } catch (Throwable t) {
                // sometimes an NPE will be thrown during shutdown via web-socket,
                // this ensures the shutdown process is finished properly
                // https://github.com/alibaba/arthas/issues/320
                logger.error("ARTHAS", "Error writing data:", t);
            }
            term.close();
        } else {
            jobController.close(closedFutureHandler());
        }
    }

    public void setForegroundJob(Job job) {
        currentForegroundJob = job;
    }

    public Job getForegroundJob() {
        return currentForegroundJob;
    }
}

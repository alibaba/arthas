package com.taobao.arthas.core.shell.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.security.AuthUtils;
import com.taobao.arthas.core.security.SecurityAuthenticator;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.Shell;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.shell.*;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.impl.SessionImpl;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.impl.TermImpl;
import com.taobao.arthas.core.shell.term.impl.http.ExtHttpTtyConnection;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.FileUtils;

import io.netty.channel.ChannelHandlerContext;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.telnet.netty.NettyTelnetConnection;
import io.termd.core.tty.TtyConnection;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * The shell session as seen from the shell server perspective.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellImpl implements Shell {
    private static final Logger logger = LoggerFactory.getLogger(ShellImpl.class);
    private SecurityAuthenticator securityAuthenticator = ArthasBootstrap.getInstance().getSecurityAuthenticator();

    private JobControllerImpl jobController;
    final String id;
    final Future<Void> closedFuture;
    private InternalCommandManager commandManager;
    private Session session = new SessionImpl();
    private Term term;
    private String welcome;
    private Job currentForegroundJob;
    private String prompt;

    public ShellImpl(ShellServer server, Term term, InternalCommandManager commandManager,
            Instrumentation instrumentation, long pid, JobControllerImpl jobController) {
        if (term instanceof TermImpl) {
            TermImpl termImpl = (TermImpl) term;
            TtyConnection conn = termImpl.getConn();
            // 处理telnet本地连接鉴权
            if (conn instanceof TelnetTtyConnection) {
                TelnetConnection telnetConnection = ((TelnetTtyConnection) conn).getTelnetConnection();
                if (telnetConnection instanceof NettyTelnetConnection) {
                    ChannelHandlerContext handlerContext = ((NettyTelnetConnection) telnetConnection)
                            .channelHandlerContext();
                    Principal principal = AuthUtils.localPrincipal(handlerContext);
                    if (principal != null) {
                        try {
                            Subject subject = securityAuthenticator.login(principal);
                            if (subject != null) {
                                session.put(ArthasConstants.SUBJECT_KEY, subject);
                            }
                        } catch (LoginException e) {
                            logger.error("local connection auth error", e);
                        }
                    }
                }
            }

            if (conn instanceof ExtHttpTtyConnection) {
                // 传递http cookie 里的鉴权信息到新建立的session中
                ExtHttpTtyConnection extConn = (ExtHttpTtyConnection) conn;
                Map<String, Object> extSessions = extConn.extSessions();
                for (Entry<String, Object> entry : extSessions.entrySet()) {
                    session.put(entry.getKey(), entry.getValue());
                }
            }
        }
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

        this.setPrompt();
    }

    public JobController jobController() {
        return jobController;
    }

    public Set<Job> jobs() {
        return jobController.jobs();
    }

    @Override
    public synchronized Job createJob(List<CliToken> args) {
        Job job = jobController.createJob(commandManager, args, session, new ShellJobHandler(this), term, null);
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

    private void setPrompt(){
        this.prompt = "[arthas@" +
                session.getPid() +
                "]$ ";
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
        term.readline(prompt, new ShellLineHandler(this),
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
                logger.error("Error writing data:", t);
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

    private static class ShellJobHandler implements JobListener {
        ShellImpl shell;

        public ShellJobHandler(ShellImpl shell) {
            this.shell = shell;
        }

        @Override
        public void onForeground(Job job) {
            shell.setForegroundJob(job);
            //reset stdin handler to job's origin handler
            //shell.term().stdinHandler(job.process().getStdinHandler());
        }

        @Override
        public void onBackground(Job job) {
            resetAndReadLine();
        }

        @Override
        public void onTerminated(Job job) {
            if (!job.isRunInBackground()){
                resetAndReadLine();
            }

            // save command history
            Term term = shell.term();
            if (term instanceof TermImpl) {
                List<int[]> history = ((TermImpl) term).getReadline().getHistory();
                FileUtils.saveCommandHistory(history, new File(Constants.CMD_HISTORY_FILE));
            }
        }

        @Override
        public void onSuspend(Job job) {
            if (!job.isRunInBackground()){
                resetAndReadLine();
            }
        }

        private void resetAndReadLine() {
            //reset stdin handler to echo handler
            //shell.term().stdinHandler(null);
            shell.setForegroundJob(null);
            shell.readline();
        }
    }

}

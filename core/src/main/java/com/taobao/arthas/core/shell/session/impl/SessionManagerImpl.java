package com.taobao.arthas.core.shell.session.impl;

import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Arthas Session Manager
 * @author gongdewei 2020-03-20
 */
public class SessionManagerImpl implements SessionManager {
    private final ArthasBootstrap bootstrap;
    private final InternalCommandManager commandManager;
    private final Instrumentation instrumentation;
    private final JobControllerImpl jobController;
    private final long timeoutMillis;
    private final long reaperInterval;
    private final Map<String, Session> sessions;
    private final long pid;
    private boolean closed = false;

    public SessionManagerImpl(ShellServerOptions options, ArthasBootstrap bootstrap, InternalCommandManager commandManager,
                              JobControllerImpl jobController) {
        this.bootstrap = bootstrap;
        this.commandManager = commandManager;
        this.jobController = jobController;
        this.sessions = new ConcurrentHashMap<String, Session>();
        this.timeoutMillis = options.getSessionTimeout();
        this.reaperInterval = options.getReaperInterval();
        this.instrumentation = options.getInstrumentation();
        this.pid = options.getPid();
    }


    @Override
    public Session createSession() {
        Session session = new SessionImpl();
        session.put(Session.COMMAND_MANAGER, commandManager);
        session.put(Session.INSTRUMENTATION, instrumentation);
        session.put(Session.PID, pid);
        //session.put(Session.SERVER, server);
        //session.put(Session.TTY, term);
        String sessionId = UUID.randomUUID().toString();
        session.put(Session.ID, sessionId);

        sessions.put(sessionId, session);
        return session;
    }

    @Override
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public Session removeSession(String sessionId) {
        return sessions.remove(sessionId);
    }

    @Override
    public void updateAccessTime(Session session) {
        session.setLastAccessTime(System.currentTimeMillis());
    }

    @Override
    public InternalCommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    @Override
    public JobControllerImpl getJobController() {
        return jobController;
    }
}

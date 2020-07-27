package com.taobao.arthas.core.shell.session.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.distribution.impl.SharingResultDistributorImpl;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.*;

/**
 * Arthas Session Manager
 *
 * @author gongdewei 2020-03-20
 */
public class SessionManagerImpl implements SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManagerImpl.class);
    private final ArthasBootstrap bootstrap;
    private final InternalCommandManager commandManager;
    private final Instrumentation instrumentation;
    private final JobController jobController;
    private final long timeoutMillis;
    private final long reaperInterval;
    private final Map<String, Session> sessions;
    private final long pid;
    private boolean closed = false;
    private ScheduledExecutorService scheduledExecutorService;

    public SessionManagerImpl(ShellServerOptions options, ArthasBootstrap bootstrap, InternalCommandManager commandManager,
                              JobController jobController) {
        this.bootstrap = bootstrap;
        this.commandManager = commandManager;
        this.jobController = jobController;
        this.sessions = new ConcurrentHashMap<String, Session>();
        this.timeoutMillis = options.getSessionTimeout();
        this.reaperInterval = options.getReaperInterval();
        this.instrumentation = options.getInstrumentation();
        this.pid = options.getPid();
        //start evict session timer
        this.setEvictTimer();
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

        //Result Distributor
        session.setResultDistributor(new SharingResultDistributorImpl(session));

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
    public void close() {
        //TODO clear resources while shutdown arthas
        closed = true;
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }

        ArrayList<Session> sessions = new ArrayList<Session>(this.sessions.values());
        for (Session session : sessions) {
            SharingResultDistributor resultDistributor = session.getResultDistributor();
            resultDistributor.appendResult(new MessageModel("arthas server is going to shutdown."));
            resultDistributor.close();
            logger.info("Removing session before shutdown: {}, last access time: {}", session.getSessionId(), session.getLastAccessTime());
            this.removeSession(session.getSessionId());
        }

        jobController.close();
        bootstrap.destroy();
    }

    private synchronized void setEvictTimer() {
        if (!closed && reaperInterval > 0) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    final Thread t = new Thread(r, "arthas-shell-server");
                    t.setDaemon(true);
                    return t;
                }
            });
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    evictSessions();
                }
            }, 0, reaperInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Check and remove inactive session
     */
    public void evictSessions() {
        long now = System.currentTimeMillis();
        List<Session> toClose = new ArrayList<Session>();
        for (Session session : sessions.values()) {
            // do not close if there is still job running,
            // e.g. trace command might wait for a long time before condition is met
            //TODO check background job size
            if (now - session.getLastAccessTime() > timeoutMillis && session.getForegroundJob() == null) {
                toClose.add(session);
            }
            evictConsumers(session);
        }
        for (Session session : toClose) {
            //interrupt foreground job
            Job job = session.getForegroundJob();
            if (job != null) {
                job.interrupt();
            }
            long timeOutInMinutes = timeoutMillis / 1000 / 60;
            String reason = "session is inactive for " + timeOutInMinutes + " min(s).";
            session.getResultDistributor().appendResult(new MessageModel(reason));
            this.removeSession(session.getSessionId());
            logger.info("Removing inactive session: {}, last access time: {}", session.getSessionId(), session.getLastAccessTime());
        }
    }

    /**
     * Check and remove inactive consumer
     */
    public void evictConsumers(Session session) {
        SharingResultDistributor distributor = session.getResultDistributor();
        if (distributor instanceof SharingResultDistributor) {
            SharingResultDistributor sharingResultDistributor = (SharingResultDistributor) distributor;
            List<ResultConsumer> consumers = sharingResultDistributor.getConsumers();
            //remove inactive consumer from session directly
            long now = System.currentTimeMillis();
            for (ResultConsumer consumer : consumers) {
                long inactiveTime = now - consumer.getLastAccessTime();
                if (inactiveTime > 30000) {
                    //inactive duration must be large than pollTimeLimit
                    logger.info("Removing inactive consumer from session, sessionId: {}, consumerId: {}, inactive duration: {}",
                            session.getSessionId(), consumer.getConsumerId(), inactiveTime);
                    consumer.appendResult(new MessageModel("consumer is inactive for a while, please refresh the page."));
                    sharingResultDistributor.removeConsumer(consumer);
                }
            }
        }
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
    public JobController getJobController() {
        return jobController;
    }
}

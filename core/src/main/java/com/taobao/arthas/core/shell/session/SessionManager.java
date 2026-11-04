package com.taobao.arthas.core.shell.session;

import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

import java.lang.instrument.Instrumentation;

/**
 * Arthas Session Manager
 * @author gongdewei 2020-03-20
 */
public interface SessionManager {

    Session createSession();

    Session getSession(String sessionId);

    Session removeSession(String sessionId);

    void updateAccessTime(Session session);

    void close();

    InternalCommandManager getCommandManager();

    Instrumentation getInstrumentation();

    JobController getJobController();
}

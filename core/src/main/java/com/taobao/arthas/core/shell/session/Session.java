package com.taobao.arthas.core.shell.session;

import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.command.CommandResolver;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * A shell session.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Session {
    String COMMAND_MANAGER = "arthas-command-manager";
    String PID = "pid";
    String INSTRUMENTATION = "instrumentation";
    String ID = "id";
    String SERVER = "server";
    /**
     * The tty this session related to.
     */
    String TTY = "tty";

    /**
     * Put some data in a session
     *
     * @param key the key for the data
     * @param obj the data
     * @return a reference to this, so the API can be used fluently
     */
    Session put(String key, Object obj);

    /**
     * Get some data from the session
     *
     * @param key the key of the data
     * @return the data
     */
    <T> T get(String key);

    /**
     * Remove some data from the session
     *
     * @param key the key of the data
     * @return the data that was there or null if none there
     */
    <T> T remove(String key);

    /**
     * Check if the session has been already locked
     *
     * @return locked or not
     */
    boolean isLocked();

    /**
     * Unlock the session
     *
     */
    void unLock();

    /**
     * Try to fetch the current session's lock
     *
     * @return success or not
     */
    boolean tryLock();

    /**
     * Check current lock's sequence id
     *
     * @return lock's sequence id
     */
    int getLock();

    /**
     * Get session id
     * @return session id
     */
    String getSessionId();

    /**
     * Get shell server
     *
     * @return shell server
     */
    ShellServer getServer();

    /**
     * Get Java PID
     *
     * @return java pid
     */
    int getPid();

    /**
     * Get all registered command resolvers
     *
     * @return command resolvers
     */
    List<CommandResolver> getCommandResolvers();

    /**
     * Get java instrumentation
     *
     * @return instrumentation instance
     */
    Instrumentation getInstrumentation();
}

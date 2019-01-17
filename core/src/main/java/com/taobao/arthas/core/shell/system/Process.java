package com.taobao.arthas.core.shell.system;

import java.util.Date;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;

/**
 * A process managed by the shell.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Process {
    /**
     * @return the current process status
     */
    ExecStatus status();

    /**
     * @return the process exit code when the status is {@link ExecStatus#TERMINATED} otherwise {@code null}
     */
    Integer exitCode();

    /**
     * Set the process tty.
     *
     * @param tty the process tty
     * @return this object
     */
    Process setTty(Tty tty);

    /**
     * @return the process tty
     */
    Tty getTty();

    /**
     * Set the process session
     *
     * @param session the process session
     * @return this object
     */
    Process setSession(Session session);

    /**
     * @return the process session
     */
    Session getSession();

    /**
     * Set an handler for being notified when the process terminates.
     *
     * @param handler the handler called when the process terminates.
     * @return this object
     */
    Process terminatedHandler(Handler<Integer> handler);

    /**
     * Run the process.
     */
    void run();

    /**
     * Run the process.
     */
    void run(boolean foreground);

    /**
     * Attempt to interrupt the process.
     *
     * @return true if the process caught the signal
     */
    boolean interrupt();

    /**
     * Attempt to interrupt the process.
     *
     * @param completionHandler handler called after interrupt callback
     * @return true if the process caught the signal
     */
    boolean interrupt(Handler<Void> completionHandler);

    /**
     * Suspend the process.
     */
    void resume();

    /**
     * Suspend the process.
     */
    void resume(boolean foreground);

    /**
     * Suspend the process.
     *
     * @param completionHandler handler called after resume callback
     */
    void resume(Handler<Void> completionHandler);

    /**
     * Suspend the process.
     *
     * @param completionHandler handler called after resume callback
     */
    void resume(boolean foreground, Handler<Void> completionHandler);

    /**
     * Resume the process.
     */
    void suspend();

    /**
     * Resume the process.
     *
     * @param completionHandler handler called after suspend callback
     */
    void suspend(Handler<Void> completionHandler);

    /**
     * Terminate the process.
     */
    void terminate();

    /**
     * Terminate the process.
     *
     * @param completionHandler handler called after end callback
     */
    void terminate(Handler<Void> completionHandler);

    /**
     * Set the process in background.
     */
    void toBackground();

    /**
     * Set the process in background.
     *
     * @param completionHandler handler called after background callback
     */
    void toBackground(Handler<Void> completionHandler);

    /**
     * Set the process in foreground.
     */
    void toForeground();

    /**
     * Set the process in foreground.
     *
     * @param completionHandler handler called after foreground callback
     */
    void toForeground(Handler<Void> completionHandler);

    /**
     * Execution times
     */
    int times();

    /**
     * Build time
     */
    Date startTime();

    /**
     * Get cache file location
     */
    String cacheLocation();

    /**
     * Set job id
     * 
     * @param jobId job id
     */
    void setJobId(int jobId);
}

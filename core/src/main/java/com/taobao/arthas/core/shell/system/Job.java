package com.taobao.arthas.core.shell.system;

import java.util.Date;

import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;

/**
 * A job executed in a {@link JobController}, grouping one or several process.<p/>
 *
 * The job life cycle can be controlled with the {@link #run}, {@link #resume} and {@link #suspend} and {@link #interrupt}
 * methods.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Job {

    /**
     * @return the job id
     */
    int id();

    /**
     * @return the job exec status
     */
    ExecStatus status();

    /**
     * @return the execution line of the job, i.e the shell command line that launched this job
     */
    String line();


    /**
     * Run the job, before running the job a {@link Tty} must be set.
     *
     * @return this object
     */
    Job run();

    /**
     * Run the job, before running the job a {@link Tty} must be set.
     *
     * @return this object
     */
    Job run(boolean foreground);

    /**
     * Attempt to interrupt the job.
     *
     * @return true if the job is actually interrupted
     */
    boolean interrupt();

    /**
     * Resume the job to foreground.
     */
    Job resume();

    /**
     * @return true if the job is running in background
     */
    boolean isRunInBackground();

    /**
     * Send the job to background.
     *
     * @return this object
     */
    Job toBackground();

    /**
     * Send the job to foreground.
     *
     * @return this object
     */
    Job toForeground();

    /**
     * Resume the job.
     *
     * @param foreground true when the job is resumed in foreground
     */
    Job resume(boolean foreground);

    /**
     * Resume the job.
     *
     * @return this object
     */
    Job suspend();

    /**
     * Terminate the job.
     */
    void terminate();

    /**
     * @return the first process in the job
     */
    Process process();

    /**
     * @return the date with job timeout
     */
    Date timeoutDate();

    /**
     * Set the date with job timeout
     * @param date the date with job timeout
     */
    void setTimeoutDate(Date date);

    /**
     * @return the session this job belongs to
     */
    Session getSession();
}

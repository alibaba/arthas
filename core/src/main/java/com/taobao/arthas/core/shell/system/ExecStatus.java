package com.taobao.arthas.core.shell.system;

/**
 * The status of an execution.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public enum ExecStatus {

    /**
     * The job is ready, it can be running or terminated.
     */
    READY,

    /**
     * The job is running, it can be stopped or terminated.
     */
    RUNNING,

    /**
     * The job is stopped, it can be running or terminated.
     */
    STOPPED,

    /**
     * The job is terminated.
     */
    TERMINATED


}

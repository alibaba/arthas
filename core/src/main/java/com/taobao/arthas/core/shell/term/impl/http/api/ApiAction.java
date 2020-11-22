package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * Http api action enums
 *
 * @author gongdewei 2020-03-25
 */
public enum ApiAction {
    /**
     * Execute command synchronized
     */
    EXEC,

    /**
     * Execute command async
     */
    ASYNC_EXEC,

    /**
     * Interrupt executing job
     */
    INTERRUPT_JOB,

    /**
     * Pull the results from result queue of the session
     */
    PULL_RESULTS,

    /**
     * Create a new session
     */
    INIT_SESSION,

    /**
     * Join a exist session
     */
    JOIN_SESSION,

    /**
     * Terminate the session
     */
    CLOSE_SESSION,

    /**
     * Get session info
     */
    SESSION_INFO
}

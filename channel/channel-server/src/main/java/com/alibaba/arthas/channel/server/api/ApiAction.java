package com.alibaba.arthas.channel.server.api;

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
     * Create a new session
     */
    INIT_SESSION,

    /**
     * Terminate the session
     */
    CLOSE_SESSION,

    /**
     * Join a exist session
     */
    JOIN_SESSION,

    /**
     * Open new WebConsole and create new session
     */
    OPEN_CONSOLE,

    /**
     * WebConsole input
     */
    CONSOLE_INPUT
}

package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * Http API response state
 *
 * @author gongdewei 2020-03-19
 */
public enum ApiState {
    /**
     * Scheduled async exec job
     */
    SCHEDULED,

//    RUNNING,

    /**
     * Request processed successfully
     */
    SUCCEEDED,

    /**
     * Request processing interrupt
     */
    INTERRUPTED,

    /**
     * Request processing failed
     */
    FAILED,

    /**
     * Request is refused
     */
    REFUSED
}

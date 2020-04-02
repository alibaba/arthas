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
     * Process request succeeded
     */
    SUCCEEDED,

    /**
     * Process request failed
     */
    FAILED,

    /**
     * Request is refused
     */
    REFUSED
}

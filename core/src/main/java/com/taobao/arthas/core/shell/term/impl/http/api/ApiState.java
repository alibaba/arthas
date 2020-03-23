package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * Http API response state
 * @author gongdewei 2020-03-19
 */
public enum ApiState {
    /** accepted */
    SCHEDULED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    REFUSED
}

package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * Http Api exception
 * @author gongdewei 2020-03-19
 */
public class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

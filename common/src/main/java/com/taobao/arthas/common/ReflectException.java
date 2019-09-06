package com.taobao.arthas.common;

public class ReflectException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private Throwable cause;

    public ReflectException(Throwable cause) {
        super(cause.getClass().getName() + "-->" + cause.getMessage());
        this.cause = cause;
    }

    public Throwable getCause() {
        return this.cause;
    }
}
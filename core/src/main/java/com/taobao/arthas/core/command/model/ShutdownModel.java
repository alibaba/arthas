package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/6/22
 */
public class ShutdownModel extends ResultModel {

    private boolean graceful;

    private String message;

    public ShutdownModel(boolean graceful, String message) {
        this.graceful = graceful;
        this.message = message;
    }

    @Override
    public String getType() {
        return "shutdown";
    }

    public boolean isGraceful() {
        return graceful;
    }

    public String getMessage() {
        return message;
    }
}

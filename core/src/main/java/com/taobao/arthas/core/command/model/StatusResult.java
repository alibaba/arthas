package com.taobao.arthas.core.command.model;

public class StatusResult extends ResultModel {
    private int statusCode;
    private String message;

    public StatusResult() {
    }

    public StatusResult(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ResultModel setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ResultModel setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String getType() {
        return "status";
    }

}

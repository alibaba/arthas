package com.taobao.arthas.core.command.result;

public class StatusResult extends ExecResult {
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

    public ExecResult setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ExecResult setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String getType() {
        return "status";
    }

}

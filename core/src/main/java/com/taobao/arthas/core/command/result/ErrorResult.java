package com.taobao.arthas.core.command.result;

import com.taobao.arthas.core.shell.command.CommandProcess;

public class ErrorResult extends ExecResult {
    private int statusCode;
    private String message;

    public ErrorResult(int statusCode, String message) {
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
        return "error";
    }

    @Override
    protected void write(CommandProcess process) {
        writeln(process, message);
    }
}

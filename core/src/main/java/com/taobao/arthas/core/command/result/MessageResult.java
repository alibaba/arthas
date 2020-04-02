package com.taobao.arthas.core.command.result;

/**
 * @author gongdewei 2020/4/2
 */
public class MessageResult extends ExecResult {
    private String message;

    public MessageResult() {
    }

    public MessageResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return "message";
    }
}

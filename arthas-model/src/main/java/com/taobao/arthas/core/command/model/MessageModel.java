package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/2
 */
public class MessageModel extends ResultModel {
    private String message;

    public MessageModel() {
    }

    public MessageModel(String message) {
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

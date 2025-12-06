package com.taobao.arthas.core.command.model;

public class StatusModel extends ResultModel {

    private int statusCode;
    private String message;

    public StatusModel(int statusCode) {
        this.statusCode = statusCode;
    }

    public StatusModel(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }


    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return "status";
    }

}

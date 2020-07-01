package com.taobao.arthas.core.command.model;

public class StatusModel extends ResultModel {
    public static StatusModel success() {
        return new StatusModel(0);
    }

    public static StatusModel failure(int statusCode, String message) {
        if (statusCode == 0) {
            throw new IllegalArgumentException("failure status code cannot be 0");
        }
        return new StatusModel(statusCode, message);
    }

    private int statusCode;
    private String message;

    public StatusModel() {
    }

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

    public StatusModel setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public StatusModel setMessage(String message) {
        this.message = message;
        return this;
    }

    public StatusModel setStatus(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
        return this;
    }

    public StatusModel setStatus(int statusCode) {
        return this.setStatus(statusCode, null);
    }

    @Override
    public String getType() {
        return "status";
    }

}

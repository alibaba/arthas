package com.alibaba.arthas.channel.server.api;

/**
 * Http Api exception
 * @author gongdewei 2020-03-19
 */
public class ApiResponse<T> {
    private String agentId;
    private String requestId;
    private ApiStatus status;
    private String message;
    private String sessionId;
    // private String consumerId;
    private T result;

    public String getAgentId() {
        return agentId;
    }

    public ApiResponse<T> setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public String getRequestId() {
        return requestId;
    }

    public ApiResponse<T> setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public ApiStatus getStatus() {
        return status;
    }

    public ApiResponse<T> setStatus(ApiStatus status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ApiResponse<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ApiResponse<T> setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

//    public String getConsumerId() {
//        return consumerId;
//    }
//
//    public ApiResponse<T> setConsumerId(String consumerId) {
//        this.consumerId = consumerId;
//        return this;
//    }

    public T getResult() {
        return result;
    }

    public ApiResponse<T> setResult(T result) {
        this.result = result;
        return this;
    }
}

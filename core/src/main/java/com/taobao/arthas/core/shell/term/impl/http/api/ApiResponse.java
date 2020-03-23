package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * Http Api exception
 * @author gongdewei 2020-03-19
 */
public class ApiResponse<T> {
    private String requestId;
    private ApiState state;
    private String message;
    private String sessionId;
    private String consumerId;
    private String jobId;
    private T body;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ApiState getState() {
        return state;
    }

    public void setState(ApiState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

}

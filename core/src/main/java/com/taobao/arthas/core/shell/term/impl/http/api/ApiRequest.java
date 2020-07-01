package com.taobao.arthas.core.shell.term.impl.http.api;

import java.util.Map;

/**
 * Http Api request
 *
 * @author gongdewei 2020-03-19
 */
public class ApiRequest {
    private String action;
    private String command;
    private String requestId;
    private String sessionId;
    private String consumerId;
    private Integer timeout;
    private Map<String, Object> options;

    @Override
    public String toString() {
        return "ApiRequest{" +
                "action='" + action + '\'' +
                ", command='" + command + '\'' +
                ", requestId='" + requestId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", consumerId='" + consumerId + '\'' +
                ", timeout=" + timeout +
                ", options=" + options +
                '}';
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}

package com.taobao.arthas.core.shell.term.impl.http.api;

import java.util.Map;

/**
 * Http Api exception
 * @author gongdewei 2020-03-19
 */
public class ApiRequest {
    private String action;
    private boolean sync;
    private String command;
    private String requestId;
    private String sessionId;
    private Map<String, Object> options;

    @Override
    public String toString() {
        return "ApiRequest{" +
                "action='" + action + '\'' +
                ", sync=" + sync +
                ", command='" + command + '\'' +
                ", requestId='" + requestId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", options=" + options +
                '}';
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
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
}

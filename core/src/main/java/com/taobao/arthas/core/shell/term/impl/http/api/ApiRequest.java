package com.taobao.arthas.core.shell.term.impl.http.api;


/**
 * Http Api request
 *
 * @author gongdewei 2020-03-19
 */
public class ApiRequest {
    private String action;
    private String command;
    private String requestId;
    private String agentId;
    private String sessionId;
    private String consumerId;
    private Integer execTimeout;

    @Override
    public String toString() {
        return "ApiRequest{" +
                "action='" + action + '\'' +
                (command != null ? ", command='" + command + '\'' : "") +
                (requestId != null ? ", requestId='" + requestId + '\'' : "") +
                (agentId != null ? ", agentId='" + agentId + '\'' : "") +
                (sessionId != null ? ", sessionId='" + sessionId + '\'' : "") +
                (consumerId != null ? ", consumerId='" + consumerId + '\'' : "") +
                (execTimeout != null ? ", execTimeout=" + execTimeout : "") +
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
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

    public Integer getExecTimeout() {
        return execTimeout;
    }

    public void setExecTimeout(Integer execTimeout) {
        this.execTimeout = execTimeout;
    }
}

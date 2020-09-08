package com.alibaba.arthas.channel.server.api;

import java.util.Map;

/**
 * Http Api request
 *
 * @author gongdewei 2020-03-19
 */
public class ApiRequest {
    private String action;
    private String command;
    private String sessionId;
    private String consumerId;
    private Integer execTimeout;

    @Override
    public String toString() {
        return "ApiRequest{" +
                "action='" + action + '\'' +
                ", command='" + command + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", consumerId='" + consumerId + '\'' +
                ", timeout=" + execTimeout +
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

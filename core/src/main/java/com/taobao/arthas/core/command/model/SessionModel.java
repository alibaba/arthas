package com.taobao.arthas.core.command.model;

/**
 * Session command result model
 *
 * @author gongdewei 2020/03/27
 */
public class SessionModel extends ResultModel {

    private long javaPid;
    private String sessionId;
    private String agentId;
    private String tunnelServer;
    private String statUrl;

    private boolean tunnelConnected;

    @Override
    public String getType() {
        return "session";
    }

    public long getJavaPid() {
        return javaPid;
    }

    public void setJavaPid(long javaPid) {
        this.javaPid = javaPid;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getTunnelServer() {
        return tunnelServer;
    }

    public void setTunnelServer(String tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    public String getStatUrl() {
        return statUrl;
    }

    public void setStatUrl(String statUrl) {
        this.statUrl = statUrl;
    }

    public boolean isTunnelConnected() {
        return tunnelConnected;
    }

    public void setTunnelConnected(boolean tunnelConnected) {
        this.tunnelConnected = tunnelConnected;
    }
}

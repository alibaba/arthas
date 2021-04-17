package com.alibaba.arthas.tunnel.server.admin.arthas;

/**
 *
 * @author Naah 2021-04-16
 *
 */
public class ArthasAgent {
    private String appName;
    private String agentId;
    private String serviceIp;
    private String servicePort;
    private String serviceId;

    public ArthasAgent(String appName, String agentId, String serviceIp, String servicePort) {
        this.appName = appName;
        this.agentId = agentId;
        this.serviceIp = serviceIp;
        this.servicePort = servicePort;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getServiceIp() {
        return serviceIp;
    }

    public void setServiceIp(String serviceIp) {
        this.serviceIp = serviceIp;
    }

    public String getServicePort() {
        return servicePort;
    }

    public void setServicePort(String servicePort) {
        this.servicePort = servicePort;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}

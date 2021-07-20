package com.alibaba.arthas.tunnel.server;

/**
 * @author hengyunabc 2020-10-30
 *
 */
public class AgentClusterInfo {
    /**
     * agent本身以哪个ip连接到 tunnel server
     */
    private String host;
    private int port;
    private String arthasVersion;

    /**
     * agent 连接到的 tunnel server 的ip
     */
    private String clientConnectHost;

    public AgentClusterInfo() {

    }

    public AgentClusterInfo(AgentInfo agentInfo, String clientConnectHost) {
        this.host = agentInfo.getHost();
        this.port = agentInfo.getPort();
        this.arthasVersion = agentInfo.getArthasVersion();
        this.clientConnectHost = clientConnectHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getArthasVersion() {
        return arthasVersion;
    }

    public void setArthasVersion(String arthasVersion) {
        this.arthasVersion = arthasVersion;
    }

    public String getClientConnectHost() {
        return clientConnectHost;
    }

    public void setClientConnectHost(String clientConnectHost) {
        this.clientConnectHost = clientConnectHost;
    }

}

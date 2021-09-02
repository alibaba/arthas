package com.alibaba.arthas.channel.server.model;

import java.util.List;

/**
 *
 * Arthas agent info
 * @author gongdewei 2020/8/10
 */
public class AgentVO {

    private String agentId;

    private String agentVersion;

    private String agentStatus;

    private String hostname;

    private String ip;

    private String osVersion;

    private String appName;

    private String channelServer;

    private String classPath;

    private String channelVersion;

    private List<String> channelFeatures;

    private String desc;

    private long heartbeatTime;

    private long createdTime;

    private long modifiedTime;


    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getAgentStatus() {
        return agentStatus;
    }

    public void setAgentStatus(String agentStatus) {
        this.agentStatus = agentStatus;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getChannelServer() {
        return channelServer;
    }

    public void setChannelServer(String channelServer) {
        this.channelServer = channelServer;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public String getChannelVersion() {
        return channelVersion;
    }

    public void setChannelVersion(String channelVersion) {
        this.channelVersion = channelVersion;
    }

    public List<String> getChannelFeatures() {
        return channelFeatures;
    }

    public void setChannelFeatures(List<String> channelFeatures) {
        this.channelFeatures = channelFeatures;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(long heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

}

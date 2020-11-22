package com.alibaba.arthas.tunnel.server.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;

/**
 * 保存agentId连接到哪个具体的 tunnel server，集群部署时使用
 * 
 * @author hengyunabc 2020-10-27
 *
 */
public interface TunnelClusterStore {
    public void addAgent(String agentId, AgentClusterInfo info, long expire, TimeUnit timeUnit);

    public AgentClusterInfo findAgent(String agentId);

    public void removeAgent(String agentId);

    public Collection<String> allAgentIds();

    public Map<String, AgentClusterInfo> agentInfo(String appName);
}

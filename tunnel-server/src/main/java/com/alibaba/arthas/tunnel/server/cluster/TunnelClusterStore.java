package com.alibaba.arthas.tunnel.server;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

/**
 * 保存agentId连接到哪个具体的 tunnel server，集群部署时使用
 * 
 * @author hengyunabc 2020-10-27
 *
 */
public interface TunnelClusterStore {
    public void addHost(String agentId, String host, long expire, TimeUnit timeUnit);

    public String findHost(String agentId);

    public void removeAgent(String agentId);

    public Collection<String> allAgentIds();

    public Collection<Pair<String, String>> agentInfo(String appName);
}

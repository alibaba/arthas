package com.alibaba.arthas.tunnel.server.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;

/**
 * 隧道集群存储接口
 * 用于在集群环境下保存Agent连接到哪个具体的Tunnel Server的信息
 * 该接口定义了Agent集群信息的增删改查操作
 *
 * @author hengyunabc 2020-10-27
 *
 */
public interface TunnelClusterStore {
    /**
     * 添加Agent的集群信息到存储中
     *
     * @param agentId Agent的唯一标识
     * @param info Agent的集群信息对象
     * @param expire 过期时间数值
     * @param timeUnit 过期时间单位
     */
    public void addAgent(String agentId, AgentClusterInfo info, long expire, TimeUnit timeUnit);

    /**
     * 根据Agent ID查找Agent的集群信息
     *
     * @param agentId Agent的唯一标识
     * @return Agent的集群信息对象，如果不存在则返回null
     */
    public AgentClusterInfo findAgent(String agentId);

    /**
     * 从存储中移除指定Agent的信息
     *
     * @param agentId 要移除的Agent的唯一标识
     */
    public void removeAgent(String agentId);

    /**
     * 获取存储中所有Agent ID的集合
     *
     * @return 所有Agent ID的集合
     */
    public Collection<String> allAgentIds();

    /**
     * 根据应用名称查询该应用下所有Agent的信息
     *
     * @param appName 应用名称
     * @return Map集合，key为agentId，value为对应的Agent集群信息
     */
    public Map<String, AgentClusterInfo> agentInfo(String appName);
}

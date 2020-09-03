package com.alibaba.arthas.channel.server.service;

import com.alibaba.arthas.channel.proto.AgentStatus;

/**
 * @author gongdewei 2020/8/19
 */
public interface AgentBizSerivce {

    void heartbeat(String agentId, String agentStatus, String agentVersion);

    void cleanOutdatedAgents();

    void compareAndUpdateAgentStatus(String agentId, AgentStatus expectedStatus, AgentStatus newStatus);
}

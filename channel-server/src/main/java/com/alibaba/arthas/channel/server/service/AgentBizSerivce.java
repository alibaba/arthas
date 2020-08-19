package com.alibaba.arthas.channel.server.service;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author gongdewei 2020/8/19
 */
public interface AgentBizSerivce {

    void heartbeat(String agentId, String agentStatus, String agentVersion);

    @Scheduled
    void cleanOutdatedAgents();
}

package com.alibaba.arthas.channel.server.service;

import com.alibaba.arthas.channel.server.model.AgentVO;

import java.util.List;

/**
 * @author gongdewei 2020/8/10
 */
public interface AgentManageService {

    List<AgentVO> listAgents();

    AgentVO findAgentById(String agentId);

    void addAgent(AgentVO agentVO);

    void updateAgent(AgentVO agentVO);

    void removeAgentById(String agentId);

    void heartbeat(String agentId, String agentStatus, String agentVersion);

}

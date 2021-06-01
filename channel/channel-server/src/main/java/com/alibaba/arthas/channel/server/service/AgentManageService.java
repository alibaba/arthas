package com.alibaba.arthas.channel.server.service;

import com.alibaba.arthas.channel.server.model.AgentVO;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * @author gongdewei 2020/8/10
 */
public interface AgentManageService {

    Mono<List<AgentVO>> listAgents();

    Mono<Optional<AgentVO>> findAgentById(String agentId);

    void addAgent(AgentVO agentVO);

    void updateAgent(AgentVO agentVO);

    void removeAgentById(String agentId);

}

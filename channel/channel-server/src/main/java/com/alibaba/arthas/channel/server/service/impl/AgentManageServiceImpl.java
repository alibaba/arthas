package com.alibaba.arthas.channel.server.service.impl;

import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gongdewei 2020/8/10
 */

public class AgentManageServiceImpl implements AgentManageService {

    private Map<String, AgentVO> agentStorage = new ConcurrentHashMap<String, AgentVO>();

    @Override
    public Mono<List<AgentVO>> listAgents() {
        return Mono.just(new ArrayList<AgentVO>(agentStorage.values()));
    }

    @Override
    public Mono<Optional<AgentVO>> findAgentById(String agentId) {
        return Mono.just(Optional.ofNullable(agentStorage.get(agentId)));
    }

    @Override
    public void addAgent(AgentVO agentVO) {
        agentStorage.put(agentVO.getAgentId(), agentVO);
    }

    @Override
    public void updateAgent(AgentVO agentVO) {
        agentStorage.put(agentVO.getAgentId(), agentVO);
    }

    @Override
    public void removeAgentById(String agentId) {
        agentStorage.remove(agentId);
    }


}

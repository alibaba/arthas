package com.alibaba.arthas.channel.server.service.impl;

import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentBizSerivce;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author gongdewei 2020/8/19
 */
public class AgentBizServiceImpl implements AgentBizSerivce {

    private static final Logger logger = LoggerFactory.getLogger(AgentBizServiceImpl.class);

    @Autowired
    private AgentManageService agentManageService;

    @Override
    public void heartbeat(String agentId, String agentStatus, String agentVersion) {
        agentManageService.findAgentById(agentId).flatMap(optionalAgentVO -> {
            if (optionalAgentVO.isPresent()) {
                AgentVO agentVO = optionalAgentVO.get();
                logger.debug("Agent heartbeat, agentId: {}, agentStatus: {}, agentVersion: {}", agentId, agentStatus, agentVersion);
                if (!StringUtils.equals(agentStatus, agentVO.getAgentStatus())) {
                    logger.info("Agent heartbeat mark agent status as {}, agentId: {}", agentStatus, agentId);
                }
                agentVO.setAgentStatus(agentStatus);
                agentVO.setAgentVersion(agentVersion);
                agentVO.setHeartbeatTime(System.currentTimeMillis());
                //agentVO.setModifiedTime(agentVO.getHeartbeatTime());
                agentManageService.updateAgent(agentVO);
            }
            return Mono.empty();
        }).subscribe();
    }

    @Override
    @Scheduled(fixedDelayString = "5000")
    public void cleanOutdatedAgents() {
        long now = System.currentTimeMillis();
        Mono<List<AgentVO>> agentsMono = agentManageService.listAgents();
        agentsMono.doOnSuccess(new Consumer<List<AgentVO>>() {
            @Override
            public void accept(List<AgentVO> agents) {
                for (AgentVO agent : agents) {
                    long heartbeatDelay = now - agent.getHeartbeatTime();
                    if (heartbeatDelay > 60000) {
                        logger.info("clean up dead agent: {}, heartbeat delay: {}", agent.getAgentId(), heartbeatDelay);
                        agentManageService.removeAgentById(agent.getAgentId());
                    } else if (heartbeatDelay > 30000) {
                        if (!AgentStatus.DOWN.name().equals(agent.getAgentStatus())) {
                            logger.info("Mark agent status as DOWN, agentId: {}, heartbeat delay: {}", agent.getAgentId(), heartbeatDelay);
                            agent.setAgentStatus(AgentStatus.DOWN.name());
                            agentManageService.updateAgent(agent);
                        }
                    } else if (heartbeatDelay > 15000) {
                        if (!AgentStatus.OUT_OF_SERVICE.name().equals(agent.getAgentStatus())) {
                            logger.info("Mark agent status as OUT_OF_SERVICE, agentId: {}, heartbeat delay: {}", agent.getAgentId(), heartbeatDelay);
                            agent.setAgentStatus(AgentStatus.OUT_OF_SERVICE.name());
                            agentManageService.updateAgent(agent);
                        }
                    }
                }
            }
        }).subscribe();

    }

    @Override
    public void compareAndUpdateAgentStatus(String agentId, AgentStatus expectedStatus, AgentStatus newStatus) {
        agentManageService.findAgentById(agentId).flatMap(optionalAgentVO -> {
            if (optionalAgentVO.isPresent()) {
                AgentVO agentVO = optionalAgentVO.get();
                if (expectedStatus.name().equals(agentVO.getAgentStatus())) {
                    agentVO.setAgentStatus(newStatus.name());
                    agentManageService.updateAgent(agentVO);
                }
            }
            return Mono.empty();
        }).subscribe();
    }
}

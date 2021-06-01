package com.alibaba.arthas.channel.server.service.impl;

import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentBizSerivce;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

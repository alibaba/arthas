package com.alibaba.arthas.channel.server.service.impl;

import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentBizSerivce;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * @author gongdewei 2020/8/19
 */
public class AgentBizServiceImpl implements AgentBizSerivce {

    private static final Logger logger = LoggerFactory.getLogger(AgentBizServiceImpl.class);

    @Autowired
    private AgentManageService agentManageService;

    @Override
    public void heartbeat(String agentId, String agentStatus, String agentVersion) {
        AgentVO agentVO = agentManageService.findAgentById(agentId);
        if (agentVO != null) {
            agentVO.setAgentStatus(agentStatus);
            agentVO.setAgentVersion(agentVersion);
            agentVO.setHeartbeatTime(System.currentTimeMillis());
            //agentVO.setModifiedTime(agentVO.getHeartbeatTime());
            agentManageService.updateAgent(agentVO);
        }
    }

    @Override
    @Scheduled(fixedDelayString = "5000")
    public void cleanOutdatedAgents() {
        long now = System.currentTimeMillis();
        List<AgentVO> agents = agentManageService.listAgents();
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


}

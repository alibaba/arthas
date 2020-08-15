package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;

import java.util.UUID;

/**
 * @author gongdewei 2020/8/14
 */
public class TestAgentServiceImpl implements AgentService {

    private AgentInfo agentInfo;

    @Override
    public AgentInfo getAgentInfo() {
        if (agentInfo == null) {
            agentInfo = AgentInfo.newBuilder()
                    .setAgentId(generateRandomId())
                    .setAgentVersion("1.0.0")
                    .setHostname("localhost")
                    .setIp("127.0.0.1")
                    .setAgentStatus(AgentStatus.UP)
                    .build();
        }
        return agentInfo;
    }

    @Override
    public void updateAgentStatus(AgentStatus agentStatus) {
        AgentInfo.Builder agentInfoBuilder = this.getAgentInfo().toBuilder();
        agentInfoBuilder.setAgentStatus(agentStatus);
        this.agentInfo = agentInfoBuilder.build();
    }

    private String generateRandomId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}

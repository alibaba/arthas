package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.AgentInfo;

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
                    .build();
        }
        return agentInfo;
    }

    private String generateRandomId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}

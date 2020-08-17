package com.taobao.arthas.core.channel;

import com.alibaba.arthas.channel.client.AgentInfoService;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.util.ArthasBanner;

/**
 * @author gongdewei 2020/8/15
 */
public class AgentInfoServiceImpl implements AgentInfoService {

    private AgentInfo agentInfo;

    private Configure configure;

    public AgentInfoServiceImpl(Configure configure) {
        this.configure = configure;
    }

    @Override
    public AgentInfo getAgentInfo() {
        if (agentInfo == null) {
            agentInfo = AgentInfo.newBuilder()
                    .setAgentId(configure.getAgentId())
                    .setAgentVersion(ArthasBanner.version())
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

}

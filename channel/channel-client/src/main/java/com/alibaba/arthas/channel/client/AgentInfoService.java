package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;

/**
 * @author gongdewei 2020/8/14
 */
public interface AgentInfoService {

    AgentInfo getAgentInfo();

    void updateAgentStatus(AgentStatus agentStatus);

}

package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.AgentInfo;

/**
 * @author gongdewei 2020/8/14
 */
public interface AgentService {

    AgentInfo getAgentInfo();

}

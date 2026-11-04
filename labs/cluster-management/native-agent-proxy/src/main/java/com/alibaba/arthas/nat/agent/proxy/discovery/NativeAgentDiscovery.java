package com.alibaba.arthas.nat.agent.proxy.discovery;

import java.util.Map;

/**
 * @description: NativeAgentDiscovery
 * @author：flzjkl
 * @date: 2024-09-19 7:22
 */
public interface NativeAgentDiscovery {

    /**
     *
     * @param address register address
     * @return Map<String, String> k: native agent client id ,v: http port + ws port
     */
    Map<String, String> findNativeAgent(String address);

}

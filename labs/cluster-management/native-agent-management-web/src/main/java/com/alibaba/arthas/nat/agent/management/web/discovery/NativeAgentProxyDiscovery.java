package com.alibaba.arthas.nat.agent.management.web.discovery;

import java.util.List;

/**
 * @description: NativeAgentProyDiscovery
 * @author：flzjkl
 * @date: 2024-09-19 7:22
 */
public interface NativeAgentProxyDiscovery {

    /**
     * list native agent proxy address
     * @param address register address
     * @return native agent proxy address
     */
    List<String> listNativeAgentProxy(String address);
}

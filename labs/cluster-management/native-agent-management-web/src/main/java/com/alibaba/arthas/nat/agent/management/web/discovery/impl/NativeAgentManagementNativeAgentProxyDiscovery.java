package com.alibaba.arthas.nat.agent.management.web.discovery.impl;

import com.alibaba.arthas.nat.agent.management.web.discovery.NativeAgentProxyDiscovery;

import java.util.Arrays;
import java.util.List;

/**
 * @description: NativeAgentManagementNativeAgentProxyDiscovery(Σ(っ °Д °;)っ 好长的类名)
 * @author：flzjkl
 * @date: 2024-10-29 20:59
 */
public class NativeAgentManagementNativeAgentProxyDiscovery implements NativeAgentProxyDiscovery {

    public static String proxyAddress;

    @Override
    public List<String> listNativeAgentProxy(String address) {
        return Arrays.asList(proxyAddress);
    }

}

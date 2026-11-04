package com.alibaba.arthas.nat.agent.proxy.registry;

/**
 * @description: NativeAgentProxyRegistry
 * @author：flzjkl
 * @date: 2024-10-20 10:31
 */
public interface NativeAgentProxyRegistry {

    /**
     *
     * @param address registry address
     * @param k native agent proxy ip
     * @param v port
     */
    void register(String address, String k, String v);

}

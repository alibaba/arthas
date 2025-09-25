package com.alibaba.arthas.nat.agent.registry;

/**
 * @description: Native agent client registry interface, easy to extend to other registry implementations
 * @author：flzjkl
 * @date: 2024-09-15 16:21
 */
public interface NativeAgentRegistry {

    /**
     * Register native agent address to registry
     *
     * @param address registry address
     * @param k       native agent ip
     * @param v       http port + ws port
     */
    void registerNativeAgent(String address, String k, String v);

}

package com.alibaba.arthas.nat.agent.client.cluster;

/**
 * @description: Native agent client registry interface, easy to extend to other registry implementations
 * @author：flzjkl
 * @date: 2024-09-15 16:21
 */
public interface NativeAgentClientRegistry {

    /**
     * Register native agent address to registry
     *
     * @param address registry address
     * @param k       native agent client ip
     * @param v       http port + ws port
     */
    void registerNativeAgentClient(String address, String k, String v);

}

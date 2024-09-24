package com.alibaba.arthas.nat.agent.server.cluster;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @description: NativeAgentClientRegistryFactoryTest
 * @author：flzjkl
 * @date: 2024-09-23 20:53
 */
public class NativeAgentDiscoveryFactoryTest {

    private static final String FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.server.NativeAgentDiscoveryFactory";
    private static final String EMPTY_FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.server.Empty";

    @Test
    public void testGetFactoryInstance () {
        NativeAgentDiscoveryFactory nativeAgentDiscoveryFactory = NativeAgentDiscoveryFactory.getNativeAgentDiscoveryFactory();
        assertNotNull(nativeAgentDiscoveryFactory, "NativeAgentDiscoveryFactory should not be null");
    }

    @Test
    public void testReadConfigInfo() {
        NativeAgentDiscoveryFactory nativeAgentDiscoveryFactory = NativeAgentDiscoveryFactory.getNativeAgentDiscoveryFactory();
        assertNotNull(nativeAgentDiscoveryFactory, "NativeAgentDiscoveryFactory should not be null");
        // File does not exist
        Map<String, String> doesExistFileConfig = nativeAgentDiscoveryFactory.readConfigInfo("");
        assertTrue(doesExistFileConfig == null || doesExistFileConfig.size() == 0, "DoNotHaveFileConfig should be null");

        // Empty file
        Map<String, String> emptyConfig = nativeAgentDiscoveryFactory.readConfigInfo(EMPTY_FILE_PATH);
        assertTrue(emptyConfig == null || emptyConfig.size() == 0, "EmptyConfig should be null");

        // Common file
        Map<String, String> commonFileConfig = nativeAgentDiscoveryFactory.readConfigInfo(FILE_PATH);
        assertNotNull(commonFileConfig, "CommonFileConfig should not be null");

        // Judge Value
        assertTrue(commonFileConfig.containsKey("zookeeper"), "The content read is incorrect");
        assertTrue(commonFileConfig.containsKey("etcd"), "The content read is incorrect");
        assertTrue(!commonFileConfig.containsKey("aaaa"), "The content read is incorrect");
        assertTrue(!commonFileConfig.containsKey("2222"), "The content read is incorrect");
        String zkPath = "com.alibaba.arthas.nat.agent.server.cluster.ZookeeperNativeAgentDiscovery";
        String etcdPath = "com.alibaba.arthas.nat.agent.server.cluster.EtcdNativeAgentDiscovery";
        assertTrue(zkPath.equals(commonFileConfig.get("zookeeper")), "The content read is incorrect");
        assertTrue(etcdPath.equals(commonFileConfig.get("etcd")), "The content read is incorrect");
    }

}

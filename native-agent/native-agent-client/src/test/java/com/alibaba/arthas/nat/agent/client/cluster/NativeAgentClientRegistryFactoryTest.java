package com.alibaba.arthas.nat.agent.client.cluster;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: NativeAgentClientRegistryFactoryTest
 * @author：flzjkl
 * @date: 2024-09-23 20:53
 */
public class NativeAgentClientRegistryFactoryTest {

    private static final String FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.client.NativeAgentClientRegistryFactory";
    private static final String EMPTY_FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.client.Empty";

    @Test
    public void testGetFactoryInstance () {
        NativeAgentClientRegistryFactory nativeAgentClientRegisterFactory = NativeAgentClientRegistryFactory.getNativeAgentClientRegisterFactory();
        assertNotNull(nativeAgentClientRegisterFactory, "NativeAgentClientRegisterFactory should not be null");
    }

    @Test
    public void testReadConfigInfo() {
        NativeAgentClientRegistryFactory nativeAgentClientRegisterFactory = NativeAgentClientRegistryFactory.getNativeAgentClientRegisterFactory();
        assertNotNull(nativeAgentClientRegisterFactory, "NativeAgentClientRegisterFactory should not be null");

        // File does not exist
        Map<String, String> doesExistFileConfig = nativeAgentClientRegisterFactory.readConfigInfo("");
        assertTrue(doesExistFileConfig == null || doesExistFileConfig.size() == 0, "DoNotHaveFileConfig should be null");

        // Empty file
        Map<String, String> emptyConfig = nativeAgentClientRegisterFactory.readConfigInfo(EMPTY_FILE_PATH);
        assertTrue(emptyConfig == null || emptyConfig.size() == 0, "EmptyConfig should be null");

        // Common file
        Map<String, String> commonFileConfig = nativeAgentClientRegisterFactory.readConfigInfo(FILE_PATH);
        assertNotNull(commonFileConfig, "CommonFileConfig should not be null");

        // Judge Value
        assertTrue(commonFileConfig.containsKey("zookeeper"), "The content read is incorrect");
        assertTrue(commonFileConfig.containsKey("etcd"), "The content read is incorrect");
        assertTrue(!commonFileConfig.containsKey("aaaa"), "The content read is incorrect");
        assertTrue(!commonFileConfig.containsKey("2222"), "The content read is incorrect");
        String zkPath = "com.alibaba.arthas.nat.agent.client.cluster.ZookeeperNativeAgentClientRegistry";
        String etcdPath = "com.alibaba.arthas.nat.agent.client.cluster.EtcdNativeAgentClientRegistry";
        assertTrue(zkPath.equals(commonFileConfig.get("zookeeper")), "The content read is incorrect");
        assertTrue(etcdPath.equals(commonFileConfig.get("etcd")), "The content read is incorrect");
    }

}

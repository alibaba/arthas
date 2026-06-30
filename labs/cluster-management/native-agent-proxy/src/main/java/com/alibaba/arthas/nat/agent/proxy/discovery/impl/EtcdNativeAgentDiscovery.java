package com.alibaba.arthas.nat.agent.proxy.discovery.impl;

import com.alibaba.arthas.nat.agent.common.constants.NativeAgentConstants;
import com.alibaba.arthas.nat.agent.proxy.discovery.NativeAgentDiscovery;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: EtcdNativeAgentDiscovery implements NativeAgentDiscovery
 * @author：flzjkl
 * @date: 2024-09-15 9:19
 */
public class EtcdNativeAgentDiscovery implements NativeAgentDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(EtcdNativeAgentDiscovery.class);

    @Override
    public Map<String, String> findNativeAgent(String address) {
        // Create kv client
        Client client = null;
        KV kvClient = null;
        Map<String, String> nativeAgentMap = null;
        try {
            client = Client.builder().endpoints("http://" + address).build();
            kvClient = client.getKVClient();

            // Get value by prefix /native-agent
            GetResponse getResponse = null;
            try {
                ByteSequence prefix = ByteSequence.from(NativeAgentConstants.NATIVE_AGENT_KEY, StandardCharsets.UTF_8);
                GetOption option = GetOption.newBuilder().isPrefix(true).build();
                getResponse = kvClient.get(prefix, option).get();
            } catch (Exception e) {
                logger.error("get value failed with prefix" + NativeAgentConstants.NATIVE_AGENT_KEY);
                throw new RuntimeException(e);
            }

            // Build Map
            List<KeyValue> kvs = getResponse.getKvs();
            nativeAgentMap = new ConcurrentHashMap<>(kvs.size());
            for (KeyValue kv : kvs) {
                String keyStr = kv.getKey().toString(StandardCharsets.UTF_8);
                if (keyStr.startsWith(NativeAgentConstants.NATIVE_AGENT_PROXY_KEY)) {
                    continue;
                }
                String[] split = keyStr.split("/");
                nativeAgentMap.put(split[2], kv.getValue().toString(StandardCharsets.UTF_8));
            }
        } finally {
            if (kvClient != null) {
                kvClient.close();
            }
            if (client != null) {
                client.close();
            }
        }
        return nativeAgentMap;
    }

}

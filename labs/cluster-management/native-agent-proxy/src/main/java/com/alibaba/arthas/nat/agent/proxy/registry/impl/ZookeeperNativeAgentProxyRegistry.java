package com.alibaba.arthas.nat.agent.proxy.registry.impl;

import com.alibaba.arthas.nat.agent.proxy.registry.NativeAgentProxyRegistry;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description: Zookeeper native agent proxy register implements NativeAgentProxyRegistry
 * @author：flzjkl
 * @date: 2024-10-20 18:21
 */
public class ZookeeperNativeAgentProxyRegistry implements NativeAgentProxyRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperNativeAgentProxyRegistry.class);
    private static CountDownLatch latch = new CountDownLatch(1);
    private static final int SESSION_TIMEOUT = 15000;
    private static final String NATIVE_AGENT_PROXY_KEY = "/native-agent-proxy";


    @Override
    public void register(String address, String k, String v) {
        // Create zookeeper client
        ZooKeeper zk = null;
        AtomicBoolean createResult = new AtomicBoolean(false);
        try {
            zk = new ZooKeeper(address, SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                    createResult.compareAndSet(false, true);
                }
            });
            latch.await();
        } catch (Exception e) {
            logger.error("Create zookeeper client failed");
            throw new RuntimeException(e);
        } finally {
            latch.countDown();
        }

        if (!createResult.get()) {
            throw new RuntimeException("Create zookeeper client failed");
        }

        try {
            // Create a service node. If the parent node does not exist, create the parent node first
            if (zk.exists(NATIVE_AGENT_PROXY_KEY, false) == null) {
                zk.create(NATIVE_AGENT_PROXY_KEY, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            // The EPHEMERAL mode is used to create child nodes, which means that the nodes are automatically removed at the end of the session
            String path = zk.create(NATIVE_AGENT_PROXY_KEY + "/" + k, v.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            logger.info("native agent proxy registered at: " + path);
        } catch (KeeperException | InterruptedException e) {
            logger.error("Register native agent proxy failed");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}

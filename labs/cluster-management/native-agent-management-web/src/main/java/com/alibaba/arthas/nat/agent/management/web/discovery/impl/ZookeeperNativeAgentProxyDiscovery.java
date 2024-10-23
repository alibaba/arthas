package com.alibaba.arthas.nat.agent.management.web.discovery.impl;

import com.alibaba.arthas.nat.agent.common.constants.NativeAgentConstants;
import com.alibaba.arthas.nat.agent.management.web.discovery.NativeAgentProxyDiscovery;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @description: ZookeeperNativeAgentProxyDiscovery implements NativeAgentProxyDiscovery
 * @author：flzjkl
 * @date: 2024-07-24 20:33
 */
public class ZookeeperNativeAgentProxyDiscovery implements NativeAgentProxyDiscovery {

    private static final int SESSION_TIMEOUT = 20000;
    private static final CountDownLatch connectedSemaphore = new CountDownLatch(1);

    @Override
    public List<String> listNativeAgentProxy(String address) {
        if (address == null || "".equals(address)) {
            return null;
        }

        // Wait for connection to be established
        try {
            ZooKeeper zooKeeper = new ZooKeeper(address, SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectedSemaphore.countDown();
                }
            });
            connectedSemaphore.await();

            // Gets a list of all children of the parent node
            List<String> children = zooKeeper.getChildren(NativeAgentConstants.NATIVE_AGENT_PROXY_KEY, false);
            if (children == null || children.size() == 0) {
                return children;
            }

            zooKeeper.close();
            return children;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.alibaba.arthas.nat.agent.server.cluster;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @description: ZookeeperNativeAgentDiscovery implements NativeAgentDiscovery
 * @author：flzjkl
 * @date: 2024-07-24 20:33
 */
public class ZookeeperNativeAgentDiscovery implements NativeAgentDiscovery {

    private static final String PARENT_PATH = "/native-agent-client";
    private static final int SESSION_TIMEOUT = 20000;
    private static final CountDownLatch connectedSemaphore = new CountDownLatch(1);

    @Override
    public Map<String, String> findNativeAgent(String address) {
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
            List<String> children = zooKeeper.getChildren(PARENT_PATH, false);

            // Get the data of the child node
            Map<String, String> res = new ConcurrentHashMap<>(children.size());
            for (String child : children) {
                String childPath = PARENT_PATH + "/" + child;
                byte[] data = zooKeeper.getData(childPath, false, new Stat());
                String dataStr = new String(data);

                res.put(child, dataStr);
            }

            zooKeeper.close();
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

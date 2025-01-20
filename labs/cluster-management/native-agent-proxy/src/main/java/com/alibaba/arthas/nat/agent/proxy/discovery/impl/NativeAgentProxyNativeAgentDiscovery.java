package com.alibaba.arthas.nat.agent.proxy.discovery.impl;

import com.alibaba.arthas.nat.agent.proxy.discovery.NativeAgentDiscovery;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description: NativeAgentProxyNativeAgentDiscovery
 * @author：flzjkl
 * @date: 2024-10-28 21:07
 */
public class NativeAgentProxyNativeAgentDiscovery implements NativeAgentDiscovery {

    /**
     * key: native agent ip : http port : ws port , value: expiration time
     */
    private static Map<String, LocalDateTime> nativeAgentMap = new ConcurrentHashMap<>();
    private final static int INITIAL_DELAY_SECONDS = 5;
    private final static int PERIOD_SECONDS = 5;

    public static void storageNativeAgent(String address, LocalDateTime expirationTime) {
        nativeAgentMap.put(address, expirationTime);
    }

    public static void nativeAgentCheckScheduled () {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            LocalDateTime now = LocalDateTime.now();
            nativeAgentMap.forEach((key, expirationTime) ->{
                if (now.isAfter(expirationTime)) {
                    nativeAgentMap.remove(key);
                }
            });
        };
        scheduler.scheduleAtFixedRate(task, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }


    @Override
    public Map<String, String> findNativeAgent(String address) {
        Map<String, String> res = new HashMap<>();
        for (String key : nativeAgentMap.keySet()) {
            String[] split = key.split("@");
            res.put(split[0], split[1]);
        }
        return res;
    }
}

package com.alibaba.arthas.nat.agent.management.web.discovery.impl;

import com.alibaba.arthas.nat.agent.management.web.discovery.NativeAgentProxyDiscovery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description: NativeAgentManagementNativeAgentProxyDiscovery(Σ(っ °Д °;)っ 好长的类名)
 * @author：flzjkl
 * @date: 2024-10-29 20:59
 */
public class NativeAgentManagementNativeAgentProxyDiscovery implements NativeAgentProxyDiscovery {

    /**
     * key: native agent ip : http port : ws port , value: expiration time
     */
    private static Map<String, LocalDateTime> nativeAgentProxyMap = new ConcurrentHashMap<>();
    private final static int INITIAL_DELAY_SECONDS = 5;
    private final static int PERIOD_SECONDS = 5;

    public static void storageNativeAgent(String address, LocalDateTime expirationTime) {
        nativeAgentProxyMap.put(address, expirationTime);
    }

    public static void nativeAgentProxyCheckScheduled () {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            LocalDateTime now = LocalDateTime.now();
            nativeAgentProxyMap.forEach((key, expirationTime) ->{
                if (now.isAfter(expirationTime)) {
                    nativeAgentProxyMap.remove(key);
                }
            });
        };
        scheduler.scheduleAtFixedRate(task, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public List<String> listNativeAgentProxy(String address) {
        return new ArrayList<>(nativeAgentProxyMap.keySet());
    }
}

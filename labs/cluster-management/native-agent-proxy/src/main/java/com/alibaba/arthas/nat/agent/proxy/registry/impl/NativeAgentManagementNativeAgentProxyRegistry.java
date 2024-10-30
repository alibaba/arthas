package com.alibaba.arthas.nat.agent.proxy.registry.impl;

import com.alibaba.arthas.nat.agent.common.utils.OkHttpUtil;
import com.alibaba.arthas.nat.agent.proxy.registry.NativeAgentProxyRegistry;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description: NativeAgentManagementNativeAgentProxyRegistry(...好长的类名)
 * @author：flzjkl
 * @date: 2024-10-29 20:26
 */
public class NativeAgentManagementNativeAgentProxyRegistry implements NativeAgentProxyRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NativeAgentManagementNativeAgentProxyRegistry.class);
    private final int INITIAL_DELAY_SECONDS = 5;
    private final int PERIOD_SECONDS = 5;
    private final int TIME_OUT_SECONDS = 15;

    @Override
    public void register(String address, String k, String v) {
        registerProxy(address, k, v);
        logger.info("register to native agent management success, native agent proxy address:{}", k);
        sendHeadBeat(address, k, v);
    }

    private void sendHeadBeat(String address, String k, String v) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            registerProxy(address, k, v);
        };
        scheduler.scheduleAtFixedRate(task, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void registerProxy (String address, String k, String v) {
        try {
            String url = "http://" + address + "/api/native-agent-proxy";
            LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(TIME_OUT_SECONDS);
            String jsonBody = "{" +
                    "\"operation\": \"register\"," +
                    "\"nativeAgentProxyAddress\":\""+ k +"\", " +
                    "\"expirationTime\": \"" + expirationTime+ "\"}";
            Response response = OkHttpUtil.postAndResponse(url, jsonBody);
            if (response.code() != 200) {
                throw new RuntimeException("Register failed! response code: " + response.code());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

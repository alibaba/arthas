package com.alibaba.arthas.nat.agent.registry.impl;

import com.alibaba.arthas.nat.agent.common.utils.OkHttpUtil;
import com.alibaba.arthas.nat.agent.registry.NativeAgentRegistry;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description: NativeAgentProxyNativeAgentRegistry
 * @author：flzjkl
 * @date: 2024-10-27 9:48
 */
public class NativeAgentProxyNativeAgentRegistry implements NativeAgentRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NativeAgentProxyNativeAgentRegistry.class);
    private final int INITIAL_DELAY_SECONDS = 5;
    private final int PERIOD_SECONDS = 5;
    private final int TIME_OUT_SECONDS = 15;

    @Override
    public void registerNativeAgent(String address, String k, String v) {
        register(address, k, v);
        logger.info("register to native agent proxy success, native agent address:{}", k);
        sendHeadBeat(address, k, v);
    }

    private void sendHeadBeat(String address, String k, String v) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            register(address, k, v);
        };
        scheduler.scheduleAtFixedRate(task, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void register (String address, String k, String v) {
        try {
            String url = "http://" + address + "/api/native-agent-proxy";
            LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(TIME_OUT_SECONDS);
            String jsonBody = "{" +
                    "\"operation\": \"register\"," +
                    "\"nativeAgentAddress\":\""+ k +"@"+ v +"\", " +
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

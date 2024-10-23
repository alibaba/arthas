package com.alibaba.arthas.nat.agent.management.web.server.http;

import com.alibaba.arthas.nat.agent.management.web.discovery.NativeAgentProxyDiscovery;
import com.alibaba.arthas.nat.agent.management.web.factory.NativeAgentProxyDiscoveryFactory;
import com.alibaba.arthas.nat.agent.management.web.server.NativeAgentManagementWebBootstrap;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @description: HttpNativeAgentProxyHandler
 * @author：flzjkl
 * @date: 2024-10-21 7:01
 */
public class HttpNativeAgentProxyHandler {

    public FullHttpResponse handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String content = request.content().toString(StandardCharsets.UTF_8);
        Map<String, Object> bodyMap = JSON.parseObject(content, new TypeReference<Map<String, Object>>() {
        });
        String operation = (String) bodyMap.get("operation");

        if ("findAvailableProxyAddress".equals(operation)) {
            return responseFindAvailableProxyAddress(ctx, request);
        }

        return null;
    }


    public FullHttpResponse responseFindAvailableProxyAddress(ChannelHandlerContext ctx, FullHttpRequest request) {
        String availableProxyAddress = findAvailableProxyAddress();
        if (availableProxyAddress == null || "".equals(availableProxyAddress)) {
            return null;
        }
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                request.getProtocolVersion(),
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(availableProxyAddress, StandardCharsets.UTF_8)
        );
        return response;
    }


    public String findAvailableProxyAddress() {
        // Find in address register
        NativeAgentProxyDiscoveryFactory proxyDiscoveryFactory = NativeAgentProxyDiscoveryFactory.getNativeAgentProxyDiscoveryFactory();
        NativeAgentProxyDiscovery proxyDiscovery = proxyDiscoveryFactory.getNativeAgentProxyDiscovery(NativeAgentManagementWebBootstrap.registrationType);
        List<String> proxyList = proxyDiscovery.listNativeAgentProxy(NativeAgentManagementWebBootstrap.registrationAddress);
        if (proxyList == null || proxyList.size() == 0) {
            return null;
        }
        // Return a random index of proxy address, like 127.0.0.1:2233
        Random random = new Random();
        int randomIndex = random.nextInt(proxyList.size());
        return proxyList.get(randomIndex);
    }


}

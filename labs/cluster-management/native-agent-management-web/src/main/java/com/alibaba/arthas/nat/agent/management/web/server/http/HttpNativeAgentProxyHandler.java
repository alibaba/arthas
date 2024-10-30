package com.alibaba.arthas.nat.agent.management.web.server.http;

import com.alibaba.arthas.nat.agent.management.web.discovery.NativeAgentProxyDiscovery;
import com.alibaba.arthas.nat.agent.management.web.discovery.impl.NativeAgentManagementNativeAgentProxyDiscovery;
import com.alibaba.arthas.nat.agent.management.web.factory.NativeAgentProxyDiscoveryFactory;
import com.alibaba.arthas.nat.agent.management.web.server.NativeAgentManagementWebBootstrap;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        if ("register".equals(operation)) {
            String addressInfo = (String) bodyMap.get("nativeAgentProxyAddress");
            String expirationTimeStr = (String) bodyMap.get("expirationTime");
            return doRegisterNativeAgentProxy(request, addressInfo, expirationTimeStr);
        }

        return null;
    }

    private FullHttpResponse doRegisterNativeAgentProxy(FullHttpRequest request, String addressInfo, String expirationTimeStr) {
        LocalDateTime expirationTime = LocalDateTime.parse(expirationTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        NativeAgentManagementNativeAgentProxyDiscovery.storageNativeAgent(addressInfo, expirationTime);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                request.getProtocolVersion(),
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer("success", StandardCharsets.UTF_8)
        );
        fillCorsHead(response);
        return response;
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
        fillCorsHead(response);
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


    private void fillCorsHead(FullHttpResponse fullHttpResponse) {
        // 设置跨域响应头
        fullHttpResponse.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        fullHttpResponse.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        fullHttpResponse.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "X-Requested-With, Content-Type, Authorization");

        // 设置其他必要的头部
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, fullHttpResponse.content().readableBytes());
    }

}

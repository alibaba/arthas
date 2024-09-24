package com.alibaba.arthas.nat.agent.server.server.http;

import com.alibaba.arthas.nat.agent.server.cluster.NativeAgentDiscovery;
import com.alibaba.arthas.nat.agent.server.cluster.NativeAgentDiscoveryFactory;
import com.alibaba.arthas.nat.agent.server.dto.NativeAgentInfoDTO;
import com.alibaba.arthas.nat.agent.server.server.NativeAgentServerBootstrap;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: HttpNativeAgentHandler
 * @author：flzjkl
 * @date: 2024-08-01 7:32
 */
public class HttpNativeAgentHandler {

    private Map<Long, String> localCache = new ConcurrentHashMap<>();

    public FullHttpResponse handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String content = request.content().toString(StandardCharsets.UTF_8);
        Map<String, Object> bodyMap = JSON.parseObject(content, new TypeReference<Map<String, Object>>() {
        });
        String operation = (String) bodyMap.get("operation");

        if ("listNativeAgent".equals(operation)) {
            return doListNativeAgent(ctx, request);
        }
        return null;
    }

    private FullHttpResponse doListNativeAgent(ChannelHandlerContext ctx, FullHttpRequest request) {
        NativeAgentDiscoveryFactory nativeAgentDiscoveryFactory = NativeAgentDiscoveryFactory.getNativeAgentDiscoveryFactory();
        NativeAgentDiscovery nativeAgentDiscovery = nativeAgentDiscoveryFactory.getNativeAgentDiscovery(NativeAgentServerBootstrap.registrationType);
        Map<String, String> nativeAgentMap = nativeAgentDiscovery.findNativeAgent(NativeAgentServerBootstrap.registrationAddress);

        List<NativeAgentInfoDTO> nativeAgentInfoList = new ArrayList<>();

        for (Map.Entry<String, String> entry : nativeAgentMap.entrySet()) {
            String nativeAgentIp = entry.getKey();
            String value = entry.getValue();
            String[] split = value.split(":");
            nativeAgentInfoList.add(new NativeAgentInfoDTO(nativeAgentIp, Integer.valueOf(split[0]), Integer.valueOf(split[1])));
        }

        String nativeAgentInfoStr = JSON.toJSONString(nativeAgentInfoList);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                request.getProtocolVersion(),
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(nativeAgentInfoStr, StandardCharsets.UTF_8)
        );

        return response;
    }



}

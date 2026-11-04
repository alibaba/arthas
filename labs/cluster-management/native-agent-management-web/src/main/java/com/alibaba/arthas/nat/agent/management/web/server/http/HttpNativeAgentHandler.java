package com.alibaba.arthas.nat.agent.management.web.server.http;

import com.alibaba.arthas.nat.agent.common.utils.OkHttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @description: HttpNativeAgentHandler
 * @author：flzjkl
 * @date: 2024-08-01 7:32
 */
public class HttpNativeAgentHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpNativeAgentHandler.class);

    private static HttpNativeAgentProxyHandler httpNativeAgentProxyHandler = new HttpNativeAgentProxyHandler();

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
        // 1、Find native agent proxy address
        String address = httpNativeAgentProxyHandler.findAvailableProxyAddress();
        if (address == null || "".equals(address)) {
            return null;
        }
        // 2、Send Http request to native agent proxy to get native agent list
        String resStr = null;
        try {
            String url = "http://" + address + "/api/native-agent-proxy";
            String jsonBody = "{\"operation\":\"listNativeAgent\"}";
            resStr = OkHttpUtil.post(url, jsonBody);
        } catch (IOException e) {
            logger.error("Send http to native agent proxy failed");
            throw new RuntimeException(e);
        }
        if (resStr == null) {
            return null;
        }
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                request.getProtocolVersion(),
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(resStr, StandardCharsets.UTF_8)
        );

        return response;
    }

}

package com.alibaba.arthas.nat.agent.proxy.server.handler.http;

import com.alibaba.arthas.nat.agent.common.handler.HttpOptionRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.net.URI;

/**
 * @description: Native Agent Proxy HttpRequestHandler
 * @author：flzjkl
 * @date: 2024-10-20 11:26
 */
public class HttpRequestHandler {

    private static HttpNativeAgentHandler httpNativeAgentHandler = new HttpNativeAgentHandler();
    private static HttpOptionRequestHandler httpOptionRequestHandler = new HttpOptionRequestHandler();

    public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String path = new URI(request.uri()).getPath();
        HttpMethod method = request.method();
        FullHttpResponse resp = null;

        if (HttpMethod.OPTIONS.equals(method)) {
            resp = httpOptionRequestHandler.handleOptionsRequest(ctx, request);
        }

        if (HttpMethod.POST.equals(method)) {
            if ("/api/native-agent-proxy".equals(path)) {
                resp = httpNativeAgentHandler.handle(ctx, request);
            }
        }

        if (resp == null) {
            resp = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.NOT_FOUND);
            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        }

        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }


}

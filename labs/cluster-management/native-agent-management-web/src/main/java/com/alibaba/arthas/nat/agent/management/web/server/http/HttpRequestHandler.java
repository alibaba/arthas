package com.alibaba.arthas.nat.agent.management.web.server.http;

import com.alibaba.arthas.nat.agent.common.handler.HttpOptionRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @description: HttpRequestHandler
 * @author：flzjkl
 * @date: 2024-07-20 10:09
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private HttpNativeAgentHandler httpNativeAgentHandler = new HttpNativeAgentHandler();
    private HttpNativeAgentProxyHandler httpNativeAgentProxyHandler = new HttpNativeAgentProxyHandler();

    private HttpOptionRequestHandler httpOptionRequestHandler = new HttpOptionRequestHandler();

    private HttpResourcesHandler httpResourcesHandler = new HttpResourcesHandler();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String path = new URI(request.uri()).getPath();
        HttpMethod method = request.method();
        FullHttpResponse resp = null;

        if (HttpMethod.GET.equals(method)) {
            if ("/".equals(path)) {
                path = "/index.html";
            }
            resp = httpResourcesHandler.handlerResources(request, path);
        }

        if (HttpMethod.OPTIONS.equals(method)) {
            resp = httpOptionRequestHandler.handleOptionsRequest(ctx, request);
        }

        if (HttpMethod.POST.equals(method)) {
            if ("/api/native-agent".equals(path)) {
                resp = httpNativeAgentHandler.handle(ctx, request);
            }
            if ("/api/native-agent-proxy".equals(path)) {
                resp = httpNativeAgentProxyHandler.handle(ctx, request);
            }
        }

        if (resp == null) {
            resp = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.NOT_FOUND);
            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        }

        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
            ctx.close();
        }
    }
}

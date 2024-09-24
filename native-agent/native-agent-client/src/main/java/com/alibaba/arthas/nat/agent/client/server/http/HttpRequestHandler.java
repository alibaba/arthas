package com.alibaba.arthas.nat.agent.client.server.http;

import com.alibaba.arthas.nat.agent.common.handler.HttpOptionRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

/**
 * @description: HttpRequestHandler
 * @author：flzjkl
 * @date: 2024-07-20 10:09
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private HttpNativeAgentHandler httpNativeAgentHandler = new HttpNativeAgentHandler();
    private HttpOptionRequestHandler httpOptionRequestHandler = new HttpOptionRequestHandler();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        HttpMethod method = request.method();
        FullHttpResponse resp = null;

        if (HttpMethod.OPTIONS.equals(method)) {
            resp = httpOptionRequestHandler.handleOptionsRequest(ctx, request);
        }

        if (HttpMethod.POST.equals(method)) {
            if ("/api/native-agent".equals(uri)) {
                resp = httpNativeAgentHandler.handle(ctx, request);
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

package com.alibaba.arthas.nat.agent.proxy.server.handler;

import com.alibaba.arthas.nat.agent.proxy.server.handler.http.HttpRequestHandler;
import com.alibaba.arthas.nat.agent.proxy.server.handler.ws.WsRequestHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * @description: RequestHandler
 * @author：flzjkl
 * @date: 2024-10-19 9:34
 */
public class RequestHandler extends SimpleChannelInboundHandler<Object> {

    private Channel outboundChannel;

    private static HttpRequestHandler httpRequestHandler = new HttpRequestHandler();
    private static WsRequestHandler wsRequestHandler = new WsRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest && !isWebSocketUpgrade((FullHttpRequest) msg)) {
            httpRequestHandler.handleHttpRequest(ctx, (FullHttpRequest) msg);
        }
        if (msg instanceof FullHttpRequest && isWebSocketUpgrade((FullHttpRequest) msg)) {
            wsRequestHandler.handleWebSocketUpgrade(ctx, (FullHttpRequest) msg);
        }
        if (msg instanceof WebSocketFrame) {
            wsRequestHandler.handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private boolean isWebSocketUpgrade(FullHttpRequest request) {
        return "Upgrade".equalsIgnoreCase(request.headers().get(HttpHeaderNames.CONNECTION)) &&
                "WebSocket".equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE));
    }


}

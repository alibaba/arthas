package com.alibaba.arthas.nat.agent.proxy.server.handler.ws;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * @description: hello world
 * @author：flzjkl
 * @date: 2024-10-20 20:05
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final Channel inboundChannel;

    public WebSocketClientHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof WebSocketFrame) {
            inboundChannel.writeAndFlush(((WebSocketFrame) msg).retain());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        inboundChannel.close();
    }

}
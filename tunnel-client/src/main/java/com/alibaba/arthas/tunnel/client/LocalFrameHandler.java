package com.alibaba.arthas.tunnel.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class LocalFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final static Logger logger = LoggerFactory.getLogger(LocalFrameHandler.class);
    private ChannelPromise handshakeFuture;

    public LocalFrameHandler() {
    }

    public ChannelPromise handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof ClientHandshakeStateEvent) {
            if (evt.equals(ClientHandshakeStateEvent.HANDSHAKE_COMPLETE)) {
                handshakeFuture.setSuccess();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("LocalFrameHandler error", cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {

    }
}

package com.alibaba.arthas.tunnel.client;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @author hengyunabc 2019-08-28
 */
public class ForwardClientSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(ForwardClientSocketClientHandler.class);

    private ChannelPromise handshakeFuture;
    private final URI localServerURI;

    public ForwardClientSocketClientHandler(URI localServerURI) {
        this.localServerURI = localServerURI;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("WebSocket Client disconnected!");
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) {
        if (evt.equals(ClientHandshakeStateEvent.HANDSHAKE_COMPLETE)) {
            try {
                connectLocalServer(ctx);
            } catch (Throwable e) {
                logger.error("ForwardClientSocketClientHandler connect local arthas server error", e);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void connectLocalServer(final ChannelHandlerContext ctx) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        logger.info("ForwardClientSocketClientHandler star connect local arthas server");
        WebSocketClientHandshaker newHandshaker = WebSocketClientHandshakerFactory.newHandshaker(localServerURI,
                WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
        final WebSocketClientProtocolHandler websocketClientHandler = new WebSocketClientProtocolHandler(
                newHandshaker);
        final LocalFrameHandler localFrameHandler = new LocalFrameHandler();

        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), websocketClientHandler,
                                localFrameHandler);
                    }
                });

        Channel localChannel = b.connect(localServerURI.getHost(), localServerURI.getPort()).sync().channel();
        this.handshakeFuture = localFrameHandler.handshakeFuture();
        handshakeFuture.addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        ChannelPipeline pipeline = future.channel().pipeline();
                        pipeline.remove(localFrameHandler);
                        pipeline.addLast(new RelayHandler(ctx.channel()));
                    }
                });

        handshakeFuture.sync();
        ctx.pipeline().remove(ForwardClientSocketClientHandler.this);
        ctx.pipeline().addLast(new RelayHandler(localChannel));
        logger.info("ForwardClientSocketClientHandler connect local arthas server success");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("ForwardClientSocketClient channel: {}" , ctx.channel(), cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}

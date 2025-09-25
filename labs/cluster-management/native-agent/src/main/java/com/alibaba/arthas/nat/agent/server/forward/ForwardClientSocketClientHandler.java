package com.alibaba.arthas.nat.agent.server.forward;


import com.alibaba.arthas.nat.agent.common.constants.NativeAgentConstants;
import com.taobao.arthas.common.ArthasConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * @description: Forward the ws request to arthas server
 * @author：flzjkl
 * @date: 2024-09-07 8:34
 */
public class ForwardClientSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(ForwardClientSocketClientHandler.class);

    private ChannelPromise handshakeFuture;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("WebSocket Client disconnected!");
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) {
        if (evt.equals(WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE)) {
            try {
                connectLocalServer(ctx);
            } catch (Throwable e) {
                logger.error("ForwardClientSocketClientHandler connect local arthas server error", e);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void connectLocalServer(final ChannelHandlerContext ctx) throws InterruptedException, URISyntaxException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        // Create the Bootstrap
        Bootstrap bootstrap = new Bootstrap();
        LocalFrameHandler localFrameHandler = new LocalFrameHandler();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));
                        pipeline.addLast(new WebSocketClientProtocolHandler(
                                WebSocketClientHandshakerFactory.newHandshaker(
                                        new URI("ws://127.0.0.1:" + NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT + "/ws"),
                                        WebSocketVersion.V13, null, false, null
                                )
                        ));
                        pipeline.addLast(localFrameHandler);
                    }
                });

        // Connect to arthas server
        Channel arthasChannel = bootstrap.connect("127.0.0.1", NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT).sync().channel();

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
        ctx.pipeline().addLast(new RelayHandler(arthasChannel));

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        handshakeFuture = null;
    }
}

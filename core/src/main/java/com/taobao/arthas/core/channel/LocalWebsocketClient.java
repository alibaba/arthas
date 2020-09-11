package com.taobao.arthas.core.channel;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.arthas.tunnel.client.LocalFrameHandler;
import com.taobao.arthas.common.ArthasConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.GenericFutureListener;
import io.termd.core.function.Consumer;

import java.net.URI;


public class LocalWebsocketClient {

    private static final Logger logger = LoggerFactory.getLogger(LocalWebsocketClient.class);

    public Channel connectLocalServer(EventLoopGroup group, final Consumer<TextWebSocketFrame> clientHandler) throws Exception {
        try {
            logger.info("connecting to local arthas server ..");
            // 入参URI实际无意义，只为了程序不出错
            WebSocketClientHandshaker newHandshaker = WebSocketClientHandshakerFactory.newHandshaker(new URI("ws://127.0.0.1:8563/ws"),
                    WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
            final WebSocketClientProtocolHandler websocketClientHandler = new WebSocketClientProtocolHandler(
                    newHandshaker);
            final LocalFrameHandler localFrameHandler = new LocalFrameHandler();

            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            b.group(group).channel(LocalChannel.class)
                    .handler(new ChannelInitializer<LocalChannel>() {
                        @Override
                        protected void initChannel(LocalChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), websocketClientHandler,
                                    localFrameHandler);
                        }
                    });

            LocalAddress localAddress = new LocalAddress(ArthasConstants.NETTY_LOCAL_ADDRESS);
            Channel localChannel = b.connect(localAddress).sync().channel();
            // Channel localChannel = b.connect(localServerURI.getHost(), localServerURI.getPort()).sync().channel();
            ChannelPromise handshakeFuture = localFrameHandler.handshakeFuture();
            handshakeFuture.addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    ChannelPipeline pipeline = future.channel().pipeline();
                    pipeline.remove(localFrameHandler);
                    pipeline.addLast(new LocalClientHandler(clientHandler));
                }
            });

            handshakeFuture.sync();
            logger.info("connect to local arthas server success");

            return localChannel;
        } catch (Exception e){
            logger.error("connect to local arthas server error", e);
            throw e;
        }
    }

    private static class LocalClientHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        Consumer<TextWebSocketFrame> clientHandler;

        public LocalClientHandler(Consumer<TextWebSocketFrame> clientHandler) {
            this.clientHandler = clientHandler;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
            clientHandler.accept(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Local websocket client handle error", cause);
            ctx.close();
        }
    }
}

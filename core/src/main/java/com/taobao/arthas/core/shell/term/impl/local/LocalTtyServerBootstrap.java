package com.taobao.arthas.core.shell.term.impl.local;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.CompletableFuture;
import io.termd.core.util.Helper;

/**
 * Convenience class for quickly starting a Netty Tty server.
 *
 */
public class LocalTtyServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(LocalTtyServerBootstrap.class);

    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    private String addr;
    private EventLoopGroup serverGroup;
    private Channel serverChannel;
    private EventExecutorGroup workerGroup;

    public LocalTtyServerBootstrap(EventExecutorGroup workerGroup) {
        this.workerGroup = workerGroup;
        this.addr = "local-tty-server";
    }

    public String getAddr() {
        return addr;
    }

    public LocalTtyServerBootstrap setAddr(String addr) {
        this.addr = addr;
        return this;
    }

    public void start(Consumer<TtyConnection> handler, final Consumer<Throwable> doneHandler) {
        serverGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-LocalWebsocketTtyBootstrap", true));

        ServerBootstrap b = new ServerBootstrap();
        b.group(serverGroup).channel(LocalServerChannel.class).handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(new LocalTtyServerInitializer(channelGroup, handler, workerGroup));

        // Address to bind on / connect to.
        final LocalAddress localAddr = new LocalAddress(addr);

        final ChannelFuture f = b.bind(localAddr);
        f.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if (future.isSuccess()) {
                    serverChannel = f.channel();
                    doneHandler.accept(null);
                } else {
                    doneHandler.accept(future.cause());
                }
            }
        });

    }

    public Channel connect(final Consumer<TextWebSocketFrame> clientHandler) throws InterruptedException {
        final LocalAddress localAddr = new LocalAddress(addr);
        //start client
        Bootstrap cb = new Bootstrap();
        cb.group(serverGroup)
                .channel(LocalChannel.class)
                .handler(new ChannelInitializer<LocalChannel>() {
                    @Override
                    public void initChannel(LocalChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new LoggingHandler(LogLevel.TRACE),
                                new LocalClientHandler(clientHandler));
                    }
                });

        // Start the client.
        return cb.connect(localAddr).sync().channel();
    }

    public CompletableFuture<Void> start(Consumer<TtyConnection> handler) {
        CompletableFuture<Void> fut = new CompletableFuture<Void>();
        start(handler, Helper.startedHandler(fut));
        return fut;
    }

    public void stop(final Consumer<Throwable> doneHandler) {
        if (serverChannel != null) {
            serverChannel.close();
        }

        channelGroup.close().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                try {
                    doneHandler.accept(future.cause());
                } finally {
                    serverGroup.shutdownGracefully();
                }
            }
        });
    }

    public CompletableFuture<Void> stop() {
        CompletableFuture<Void> fut = new CompletableFuture<Void>();
        stop(Helper.stoppedHandler(fut));
        return fut;
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
            logger.error("Local tty client handle error", cause);
            ctx.close();
        }
    }
}



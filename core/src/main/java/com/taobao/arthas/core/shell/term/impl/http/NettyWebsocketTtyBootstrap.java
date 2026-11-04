package com.taobao.arthas.core.shell.term.impl.http;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyWebsocketTtyBootstrap {

    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    private String host;
    private int port;
    private EventLoopGroup group;
    private Channel channel;
    private EventExecutorGroup workerGroup;
    private HttpSessionManager httpSessionManager;

    public NettyWebsocketTtyBootstrap(EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
        this.workerGroup = workerGroup;
        this.host = "localhost";
        this.port = 8080;
        this.httpSessionManager = httpSessionManager;
    }

    public String getHost() {
        return host;
    }

    public NettyWebsocketTtyBootstrap setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public NettyWebsocketTtyBootstrap setPort(int port) {
        this.port = port;
        return this;
    }

    public void start(Consumer<TtyConnection> handler, final Consumer<Throwable> doneHandler) {
        group = new NioEventLoopGroup(new DefaultThreadFactory("arthas-NettyWebsocketTtyBootstrap", true));

        if (this.port > 0) {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new TtyServerInitializer(channelGroup, handler, workerGroup, httpSessionManager));

            final ChannelFuture f = b.bind(host, port);
            f.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        channel = f.channel();
                        doneHandler.accept(null);
                    } else {
                        doneHandler.accept(future.cause());
                    }
                }
            });
        }

        // listen local address in VM communication
        ServerBootstrap b2 = new ServerBootstrap();
        b2.group(group).channel(LocalServerChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new LocalTtyServerInitializer(channelGroup, handler, workerGroup));

        ChannelFuture bindLocalFuture = b2.bind(new LocalAddress(ArthasConstants.NETTY_LOCAL_ADDRESS));
        if (this.port < 0) { // 保证回调doneHandler
            bindLocalFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        doneHandler.accept(null);
                    } else {
                        doneHandler.accept(future.cause());
                    }
                }
            });
        }
    }

    public CompletableFuture<Void> start(Consumer<TtyConnection> handler) {
        CompletableFuture<Void> fut = new CompletableFuture<Void>();
        start(handler, Helper.startedHandler(fut));
        return fut;
    }

    public void stop(final Consumer<Throwable> doneHandler) {
        if (channel != null) {
            channel.close();
        }

        channelGroup.close().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                try {
                    doneHandler.accept(future.cause());
                } finally {
                    group.shutdownGracefully();
                }
            }
        });
    }

    public CompletableFuture<Void> stop() {
        CompletableFuture<Void> fut = new CompletableFuture<Void>();
        stop(Helper.stoppedHandler(fut));
        return fut;
    }
}

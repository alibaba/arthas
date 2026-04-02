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
 * 基于Netty的WebSocket Tty服务器引导类
 *
 * 用于快速启动一个支持WebSocket协议的Tty服务器
 * 支持两种连接方式：
 * 1. 通过网络端口（WebSocket协议）的远程连接
 * 2. 通过本地地址（同一JVM内）的本地连接
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyWebsocketTtyBootstrap {

    // 通道组，用于管理所有活动的连接通道，使用立即执行器
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    // 监听的主机地址
    private String host;

    // 监听的端口号
    private int port;

    // Netty事件循环组，用于处理IO操作
    private EventLoopGroup group;

    // 服务器主通道，保存用于后续关闭
    private Channel channel;

    // 工作线程组，用于执行耗时的业务逻辑处理
    private EventExecutorGroup workerGroup;

    // HTTP会话管理器，用于管理HTTP会话
    private HttpSessionManager httpSessionManager;

    /**
     * 构造函数
     *
     * @param workerGroup 工作线程组，用于执行业务逻辑
     * @param httpSessionManager HTTP会话管理器，用于管理HTTP会话
     */
    public NettyWebsocketTtyBootstrap(EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
        this.workerGroup = workerGroup;

        // 默认监听本地地址
        this.host = "localhost";

        // 默认端口为8080
        this.port = 8080;

        // 保存HTTP会话管理器
        this.httpSessionManager = httpSessionManager;
    }

    /**
     * 获取监听的主机地址
     *
     * @return 主机地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置监听的主机地址
     *
     * @param host 主机地址
     * @return 当前对象，支持链式调用
     */
    public NettyWebsocketTtyBootstrap setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * 获取监听的端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置监听的端口号
     *
     * @param port 端口号
     * @return 当前对象，支持链式调用
     */
    public NettyWebsocketTtyBootstrap setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * 启动服务器（带回调版本）
     *
     * 启动两种服务器：
     * 1. 如果端口号大于0，启动网络服务器监听指定端口
     * 2. 总是启动本地服务器监听本地地址，用于JVM内部通信
     *
     * @param handler Tty连接处理器，处理新建立的Tty连接
     * @param doneHandler 完成后的回调处理器，用于处理启动结果
     */
    public void start(Consumer<TtyConnection> handler, final Consumer<Throwable> doneHandler) {
        // 创建NIO事件循环组，使用守护线程，线程名为"arthas-NettyWebsocketTtyBootstrap"
        group = new NioEventLoopGroup(new DefaultThreadFactory("arthas-NettyWebsocketTtyBootstrap", true));

        // 如果端口号大于0，启动网络服务器
        if (this.port > 0) {
            // 创建网络服务器引导类
            ServerBootstrap b = new ServerBootstrap();

            // 配置网络服务器：
            // 1. 设置事件循环组
            // 2. 设置通道类型为NIO服务器Socket通道
            // 3. 添加日志处理器，记录INFO级别的日志
            // 4. 设置子通道初始化器，配置每个新连接的处理器管道
            b.group(group).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new TtyServerInitializer(channelGroup, handler, workerGroup, httpSessionManager));

            // 绑定主机和端口，启动网络服务器
            final ChannelFuture f = b.bind(host, port);

            // 添加监听器处理绑定结果
            f.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        // 绑定成功，保存主通道引用
                        channel = f.channel();

                        // 回调完成处理器（传入null表示成功）
                        doneHandler.accept(null);
                    } else {
                        // 绑定失败，回调完成处理器并传入异常
                        doneHandler.accept(future.cause());
                    }
                }
            });
        }

        // 监听本地地址，用于同一JVM内的通信
        // 这种方式不需要网络连接，直接通过内存通信，性能更高
        ServerBootstrap b2 = new ServerBootstrap();

        // 配置本地服务器：
        // 1. 设置事件循环组
        // 2. 设置通道类型为本地服务器通道
        // 3. 添加日志处理器
        // 4. 设置子通道初始化器
        b2.group(group).channel(LocalServerChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new LocalTtyServerInitializer(channelGroup, handler, workerGroup));

        // 绑定本地地址
        ChannelFuture bindLocalFuture = b2.bind(new LocalAddress(ArthasConstants.NETTY_LOCAL_ADDRESS));

        // 如果端口号小于0（不启动网络服务器），需要确保回调doneHandler
        if (this.port < 0) { // 保证回调doneHandler
            // 添加监听器处理本地绑定结果
            bindLocalFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        // 绑定成功，回调完成处理器
                        doneHandler.accept(null);
                    } else {
                        // 绑定失败，回调完成处理器并传入异常
                        doneHandler.accept(future.cause());
                    }
                }
            });
        }
    }

    /**
     * 异步启动服务器
     *
     * 创建并启动服务器，返回一个CompletableFuture用于等待启动完成
     *
     * @param handler Tty连接处理器，处理新建立的Tty连接
     * @return CompletableFuture，用于等待启动完成
     */
    public CompletableFuture<Void> start(Consumer<TtyConnection> handler) {
        // 创建一个CompletableFuture用于异步操作
        CompletableFuture<Void> fut = new CompletableFuture<Void>();

        // 调用带回调的start方法，使用Helper工具类创建回调处理器
        start(handler, Helper.startedHandler(fut));

        // 返回Future，调用者可以通过它等待启动完成
        return fut;
    }

    /**
     * 停止服务器（带回调版本）
     *
     * 关闭所有通道，优雅地关闭事件循环组
     *
     * @param doneHandler 完成后的回调处理器，用于处理停止结果
     */
    public void stop(final Consumer<Throwable> doneHandler) {
        // 如果主通道存在，关闭它
        if (channel != null) {
            channel.close();
        }

        // 关闭所有通道，并在关闭完成后调用监听器
        channelGroup.close().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                try {
                    // 调用完成处理器，传入操作过程中可能发生的异常
                    doneHandler.accept(future.cause());
                } finally {
                    // 无论操作成功或失败，都要优雅地关闭事件循环组
                    // 优雅关闭会等待所有已提交的任务完成
                    group.shutdownGracefully();
                }
            }
        });
    }

    /**
     * 异步停止服务器
     *
     * 停止服务器，返回一个CompletableFuture用于等待停止完成
     *
     * @return CompletableFuture，用于等待停止完成
     */
    public CompletableFuture<Void> stop() {
        // 创建一个CompletableFuture用于异步操作
        CompletableFuture<Void> fut = new CompletableFuture<Void>();

        // 调用带回调的stop方法，使用Helper工具类创建回调处理器
        stop(Helper.stoppedHandler(fut));

        // 返回Future，调用者可以通过它等待停止完成
        return fut;
    }
}

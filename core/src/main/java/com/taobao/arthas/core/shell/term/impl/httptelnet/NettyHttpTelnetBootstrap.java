package com.taobao.arthas.core.shell.term.impl.httptelnet;

import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.termd.core.function.Consumer;
import io.termd.core.function.Supplier;
import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.tty.TtyConnection;

/**
 * 基于Netty的HTTP和Telnet协议服务器引导类
 *
 * 支持HTTP和Telnet两种协议的混合使用，能够根据客户端请求自动检测协议类型
 * 提供Tty连接功能，支持通过HTTP或Telnet方式访问Arthas控制台
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-11-05
 */
public class NettyHttpTelnetBootstrap extends TelnetBootstrap {

    // Netty事件循环组，用于处理IO操作
    private EventLoopGroup group;

    // 通道组，用于管理所有活动的连接通道
    private ChannelGroup channelGroup;

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
    public NettyHttpTelnetBootstrap(EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
        this.workerGroup = workerGroup;

        // 创建NIO事件循环组，使用守护线程，线程名为"arthas-NettyHttpTelnetBootstrap"
        this.group = new NioEventLoopGroup(new DefaultThreadFactory("arthas-NettyHttpTelnetBootstrap", true));

        // 创建默认通道组，用于管理所有活动的通道
        this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

        // 保存HTTP会话管理器
        this.httpSessionManager = httpSessionManager;
    }

    /**
     * 设置监听的主机地址
     *
     * @param host 主机地址
     * @return 当前对象，支持链式调用
     */
    public NettyHttpTelnetBootstrap setHost(String host) {
        return (NettyHttpTelnetBootstrap) super.setHost(host);
    }

    /**
     * 设置监听的端口号
     *
     * @param port 端口号
     * @return 当前对象，支持链式调用
     */
    public NettyHttpTelnetBootstrap setPort(int port) {
        return (NettyHttpTelnetBootstrap) super.setPort(port);
    }

    /**
     * 启动服务器（已废弃的方法，不应调用）
     *
     * @param factory Telnet处理器工厂
     * @param doneHandler 完成后的回调处理器
     */
    @Override
    public void start(Supplier<TelnetHandler> factory, Consumer<Throwable> doneHandler) {
        // 忽略此方法，从不调用

    }

    /**
     * 启动HTTP Telnet服务器
     *
     * 创建并启动Netty服务器，配置协议检测处理器，支持HTTP和Telnet两种协议
     *
     * @param handlerFactory Telnet处理器工厂，用于创建Telnet处理器
     * @param factory Tty连接处理器工厂，用于创建Tty连接
     * @param doneHandler 完成后的回调处理器，用于处理启动结果
     */
    public void start(final Supplier<TelnetHandler> handlerFactory, final Consumer<TtyConnection> factory,
                    final Consumer<Throwable> doneHandler) {
        // 创建服务器引导类
        ServerBootstrap boostrap = new ServerBootstrap();

        // 配置服务器参数：
        // 1. 设置事件循环组
        // 2. 设置通道类型为NIO服务器Socket通道
        // 3. 设置连接请求队列大小为100
        // 4. 添加日志处理器，记录INFO级别的日志
        // 5. 设置子通道初始化器，配置每个新连接的处理器管道
        boostrap.group(group).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                // 为每个新连接添加协议检测处理器
                                // 该处理器能够自动检测客户端使用的是HTTP还是Telnet协议
                                ch.pipeline().addLast(new ProtocolDetectHandler(channelGroup, handlerFactory, factory, workerGroup, httpSessionManager));
                            }
                        });

        // 绑定主机和端口，添加监听器处理绑定结果
        boostrap.bind(getHost(), getPort()).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                // 绑定操作完成后的回调
                if (future.isSuccess()) {
                    // 绑定成功，回调完成处理器（传入null表示成功）
                    doneHandler.accept(null);
                } else {
                    // 绑定失败，回调完成处理器并传入异常
                    doneHandler.accept(future.cause());
                }
            }
        });
    }

    /**
     * 停止服务器
     *
     * 关闭所有通道，优雅地关闭事件循环组
     *
     * @param doneHandler 完成后的回调处理器，用于处理停止结果
     */
    @Override
    public void stop(final Consumer<Throwable> doneHandler) {
        // 创建一个监听器适配器，用于处理通道关闭操作
        GenericFutureListener<Future<Object>> adapter = new GenericFutureListener<Future<Object>>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                try {
                    // 调用完成处理器，传入操作过程中可能发生的异常
                    doneHandler.accept(future.cause());
                } finally {
                    // 无论操作成功或失败，都要优雅地关闭事件循环组
                    // 优雅关闭会等待所有已提交的任务完成
                    group.shutdownGracefully();
                }
            }
        };

        // 关闭所有通道，并在关闭完成后调用监听器
        channelGroup.close().addListener(adapter);
    }

}
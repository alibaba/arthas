package com.taobao.arthas.grpc.server;

import com.alibaba.arthas.deps.ch.qos.logback.classic.Level;
import com.alibaba.arthas.deps.ch.qos.logback.classic.LoggerContext;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.Http2Handler;
import com.taobao.arthas.grpc.server.handler.executor.GrpcExecutorFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.lang.invoke.MethodHandles;

/**
 * Arthas gRPC 服务器核心类
 *
 * 该类负责构建和启动基于 Netty 的 gRPC 服务器。
 * 它使用 HTTP/2 协议进行通信，支持 Arthas 诊断命令的远程执行。
 * 服务器采用 Netty 的主从线程池模型，提供高性能的网络通信能力。
 *
 * 主要功能：
 * 1. 初始化并启动 gRPC 服务器
 * 2. 配置 HTTP/2 协议支持
 * 3. 加载和注册 gRPC 服务
 * 4. 处理客户端请求的分发和执行
 *
 * @author: FengYe
 * @date: 2024/7/3 上午12:30
 * @description: ArthasGrpcServer
 */
public class ArthasGrpcServer {

    /**
     * 日志记录器
     * 使用 MethodHandles.lookup() 动态获取当前类名作为日志记录器的名称
     */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * 服务器监听端口号
     * 默认值为 9091，可通过构造函数自定义
     */
    private int port = 9091;

    /**
     * gRPC 服务包名
     * 用于扫描和加载指定包下的 gRPC 服务实现类
     * 如果为 null，则不扫描任何包
     */
    private String grpcServicePackageName;

    /**
     * 构造函数
     *
     * 创建一个新的 Arthas gRPC 服务器实例
     *
     * @param port 服务器监听的端口号
     * @param grpcServicePackageName gRPC 服务所在的包名，用于服务扫描和注册
     */
    public ArthasGrpcServer(int port, String grpcServicePackageName) {
        this.port = port;
        this.grpcServicePackageName = grpcServicePackageName;
    }

    /**
     * 启动 gRPC 服务器
     *
     * 该方法执行以下操作：
     * 1. 创建 Netty 的 Boss 和 Worker 线程组
     * 2. 初始化 gRPC 请求分发器
     * 3. 加载 gRPC 服务实现
     * 4. 配置并启动 Netty 服务器
     * 5. 设置 HTTP/2 协议支持
     *
     * 服务器启动后会一直阻塞运行，直到发生异常或被主动关闭。
     */
    public void start() {
        // 创建 Boss 线程组，负责接收客户端连接
        // 参数 1 表示使用 1 个线程来处理连接请求
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        // 创建 Worker 线程组，负责处理已建立连接的 I/O 操作
        // 参数 10 表示使用 10 个线程来处理网络读写
        EventLoopGroup workerGroup = new NioEventLoopGroup(10);

        // 创建 gRPC 请求分发器，负责将请求路由到对应的服务方法
        GrpcDispatcher grpcDispatcher = new GrpcDispatcher();

        // 加载指定包名下的 gRPC 服务实现类
        grpcDispatcher.loadGrpcService(grpcServicePackageName);

        // 创建 gRPC 执行器工厂，负责为服务调用提供执行线程池
        GrpcExecutorFactory grpcExecutorFactory = new GrpcExecutorFactory();

        // 加载并初始化执行器
        grpcExecutorFactory.loadExecutor(grpcDispatcher);

        try {
            // 创建 Netty 服务器启动引导类
            ServerBootstrap b = new ServerBootstrap();

            // 配置服务器参数
            b.group(bossGroup, workerGroup)  // 设置主从线程组
                    .channel(NioServerSocketChannel.class)  // 使用 NIO 的服务器 Socket 通道
                    .option(ChannelOption.SO_BACKLOG, 1024)  // 设置连接队列大小为 1024
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        /**
                         * 初始化通道的 Pipeline
                         *
                         * @param ch 客户端连接的 Socket 通道
                         */
                        @Override
                        public void initChannel(SocketChannel ch) {
                            // 添加 HTTP/2 帧编解码器，用于处理 HTTP/2 协议帧
                            ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());

                            // 添加自定义的 HTTP/2 处理器，负责处理 gRPC 请求和响应
                            ch.pipeline().addLast(new Http2Handler(grpcDispatcher, grpcExecutorFactory));
                        }
                    });

            // 绑定端口并启动服务器
            // sync() 方法会阻塞，直到服务器绑定成功
            Channel channel = b.bind(port).sync().channel();

            // 记录服务器启动成功的日志
            logger.info("ArthasGrpcServer start successfully on port: {}", port);

            // 阻塞主线程，等待服务器 socket 关闭
            // 这使得服务器能够持续运行，处理客户端请求
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            // 捕获线程中断异常，记录服务器启动错误日志
            logger.error("ArthasGrpcServer start error", e);
        } finally {
            // 无论服务器是否正常启动，都要优雅地关闭线程组
            // 释放所有网络资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

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
 * @author: FengYe
 * @date: 2024/7/3 上午12:30
 * @description: ArthasGrpcServer
 */
public class ArthasGrpcServer {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private int port = 9091;

    private String grpcServicePackageName;

    public ArthasGrpcServer(int port, String grpcServicePackageName) {
        this.port = port;
        this.grpcServicePackageName = grpcServicePackageName;
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(10);

        GrpcDispatcher grpcDispatcher = new GrpcDispatcher();
        grpcDispatcher.loadGrpcService(grpcServicePackageName);
        GrpcExecutorFactory grpcExecutorFactory = new GrpcExecutorFactory();
        grpcExecutorFactory.loadExecutor(grpcDispatcher);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());
                            ch.pipeline().addLast(new Http2Handler(grpcDispatcher, grpcExecutorFactory));
                        }
                    });
            Channel channel = b.bind(port).sync().channel();
            logger.info("ArthasGrpcServer start successfully on port: {}", port);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("ArthasGrpcServer start error", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

package com.taobao.arthas.grpcweb.proxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public final class GrpcWebProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(GrpcWebProxyServer.class);


    private int port;

    private int grpcPort;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Channel channel;


    public GrpcWebProxyServer(int port, int grpcPort) {
        this.port = port;
        this.grpcPort = grpcPort;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
    }

    public void start() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new GrpcWebProxyServerInitializer(grpcPort));
            channel = serverBootstrap.bind(port).sync().channel();

            logger.info("grpc web proxy server started, listening on " + port);
            System.out.println("grpc web proxy server started, listening on " + port);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.info("fail to start grpc web proxy server!");
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void close() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if(workerGroup != null){
            workerGroup.shutdownGracefully();
        }
        logger.info("success to close grpc web proxy server!");
    }

    public int actualPort() {
        int boundPort = ((InetSocketAddress) channel.localAddress()).getPort();
        return boundPort;
    }
}

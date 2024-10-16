package com.alibaba.arthas.nat.agent.server.server;

import com.alibaba.arthas.nat.agent.common.utils.WelcomeUtil;
import com.alibaba.arthas.nat.agent.server.server.http.HttpRequestHandler;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @description: native agent server
 * @author：flzjkl
 * @date: 2024-07-20 9:23
 */

@Name("arthas-native-agent-server")
@Summary("Bootstrap Arthas Native Agent Server")
@Description("EXAMPLES:\n" + "  java -jar native-agent-server.jar  --registration-type etcd --registration-address 161.169.97.114:2379\n"
        + "java -jar native-agent-server.jar  --http-port 3939  --registration-type etcd --registration-address 161.169.97.114:2379\n"
        + "https://arthas.aliyun.com/doc\n")
public class NativeAgentServerBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(NativeAgentServerBootstrap.class);
    private static final int DEFAULT_NATIVE_AGENT_SERVER_PORT = 3939;
    private Integer httpPort;
    public static String registrationType;
    public static String registrationAddress;

    @Option(longName = "http-port")
    @Description("native agent server http port, default 3939")
    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    @Option(longName = "registration-type", required = true)
    @Description("registration type")
    public void setRegistrationType(String registrationType) {
        this.registrationType = registrationType;
    }

    @Option(longName = "registration-address", required = true)
    @Description("registration address")
    public void setRegistrationAddress(String registrationAddress) {
        this.registrationAddress = registrationAddress;
    }

    public static void main(String[] args) {
        // Print welcome message
        WelcomeUtil.printServerWelcomeMsg();

        // Startup parameter analysis
        logger.info("read input config...");
        NativeAgentServerBootstrap nativeAgentServerBootstrap = new NativeAgentServerBootstrap();
        CLI cli = CLIConfigurator.define(NativeAgentServerBootstrap.class);
        CommandLine commandLine = cli.parse(Arrays.asList(args));
        try {
            CLIConfigurator.inject(commandLine, nativeAgentServerBootstrap);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("read input success!");

        // Start the http server
        logger.info("start the http server... httPort:{}", nativeAgentServerBootstrap.getHttpPort());
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));
                            ch.pipeline().addLast(new HttpRequestHandler());
                        }
                    });
            ChannelFuture f = b.bind(nativeAgentServerBootstrap.getHttpPortOrDefault()).sync();
            logger.info("start the http server success! htt port:{}", nativeAgentServerBootstrap.getHttpPortOrDefault());
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("The native agent server fails to start, http port{}", nativeAgentServerBootstrap.getHttpPortOrDefault());
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            logger.info("shutdown native agent server");
        }
    }

    public int getHttpPortOrDefault() {
        if (this.httpPort == null) {
            return DEFAULT_NATIVE_AGENT_SERVER_PORT;
        } else {
            return this.httpPort;
        }
    }

    public String getRegistrationType() {
        return registrationType;
    }

    public String getRegistrationAddress() {
        return registrationAddress;
    }

    public Integer getHttpPort() {
        return httpPort;
    }
}

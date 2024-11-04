package com.alibaba.arthas.nat.agent.management.web.server;

import com.alibaba.arthas.nat.agent.common.constants.NativeAgentConstants;
import com.alibaba.arthas.nat.agent.common.utils.WelcomeUtil;
import com.alibaba.arthas.nat.agent.management.web.discovery.impl.NativeAgentManagementNativeAgentProxyDiscovery;
import com.alibaba.arthas.nat.agent.management.web.server.http.HttpRequestHandler;

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
 * @description: native agent management web
 * @author：flzjkl
 * @date: 2024-07-20 9:23
 */
@Name("arthas-native-agent-management-web")
@Summary("Bootstrap Arthas Native Management Web")
@Description("EXAMPLES:\n" + "java -jar native-agent-management-web.jar --proxy-address 161.169.97.114:2233\n"
        + "java -jar native-agent-management-web.jar  --registration-type etcd --registration-address 161.169.97.114:2379\n"
        + "java -jar native-agent-management-web.jar  --port 3939  --registration-type etcd --registration-address 161.169.97.114:2379\n"
        + "https://arthas.aliyun.com/doc\n")
public class NativeAgentManagementWebBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(NativeAgentManagementWebBootstrap.class);
    private static final int DEFAULT_NATIVE_AGENT_MANAGEMENT_WEB_PORT = 3939;
    private Integer port;
    private String proxyAddress;
    public static String registrationType;
    public static String registrationAddress;

    @Option(longName = "port")
    @Description("native agent management port, default 3939")
    public void setPort(Integer port) {
        this.port = port;
    }

    @Option(longName = "proxy-address")
    @Description("native agent proxy address")
    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    @Option(longName = "registration-type")
    @Description("registration type")
    public void setRegistrationType(String registrationType) {
        this.registrationType = registrationType;
    }

    @Option(longName = "registration-address")
    @Description("registration address")
    public void setRegistrationAddress(String registrationAddress) {
        this.registrationAddress = registrationAddress;
    }

    public static void main(String[] args) {
        // Print welcome message
        WelcomeUtil.printManagementWebWelcomeMsg();

        // Startup parameter analysis
        logger.info("read input config...");
        NativeAgentManagementWebBootstrap nativeAgentManagementWebBootstrap = new NativeAgentManagementWebBootstrap();
        CLI cli = CLIConfigurator.define(NativeAgentManagementWebBootstrap.class);
        CommandLine commandLine = cli.parse(Arrays.asList(args));
        try {
            CLIConfigurator.inject(commandLine, nativeAgentManagementWebBootstrap);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("read input success!");

        logger.info("check bootstrap params ...");
        boolean checkBootstrapParamsRes = checkBootstrapParams(nativeAgentManagementWebBootstrap);
        if (!checkBootstrapParamsRes) {
            throw new RuntimeException("Failed to verify the bootstrap parameters. " +
                    "Please read the documentation and check the parameters you entered");
        }
        if (nativeAgentManagementWebBootstrap.getRegistrationType() == null
                && nativeAgentManagementWebBootstrap.getRegistrationAddress() == null
                && nativeAgentManagementWebBootstrap.getProxyAddress() != null) {
            nativeAgentManagementWebBootstrap.setRegistrationType("native-agent-management");
            nativeAgentManagementWebBootstrap.setRegistrationAddress("127.0.0,1:" + nativeAgentManagementWebBootstrap.getPortOrDefault());
            NativeAgentManagementNativeAgentProxyDiscovery.proxyAddress = nativeAgentManagementWebBootstrap.getProxyAddress();
        }
        // Start the http server
        logger.info("start the http server... httPort:{}", nativeAgentManagementWebBootstrap.getPortOrDefault());
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
                            ch.pipeline().addLast(new HttpObjectAggregator(NativeAgentConstants.MAX_HTTP_CONTENT_LENGTH));
                            ch.pipeline().addLast(new HttpRequestHandler());
                        }
                    });
            ChannelFuture f = b.bind(nativeAgentManagementWebBootstrap.getPortOrDefault()).sync();
            logger.info("start the http server success! htt port:{}", nativeAgentManagementWebBootstrap.getPortOrDefault());
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("The native agent server fails to start, http port{}", nativeAgentManagementWebBootstrap.getPortOrDefault());
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            logger.info("shutdown native agent server");
        }
    }

    private static boolean checkBootstrapParams(NativeAgentManagementWebBootstrap managementBootstrap) {
        String address = managementBootstrap.getRegistrationAddress();
        String type = managementBootstrap.getRegistrationType();
        String proxyAddress = managementBootstrap.getProxyAddress();
        // single
        if (address == null && type == null && proxyAddress != null) {
            return true;
        }
        // cluster
        if (address != null && type != null && proxyAddress == null) {
            return true;
        }
        return false;
    }

    public int getPortOrDefault() {
        if (this.port == null) {
            return DEFAULT_NATIVE_AGENT_MANAGEMENT_WEB_PORT;
        } else {
            return this.port;
        }
    }

    public String getRegistrationType() {
        return registrationType;
    }

    public String getRegistrationAddress() {
        return registrationAddress;
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public Integer getPort() {
        return port;
    }
}

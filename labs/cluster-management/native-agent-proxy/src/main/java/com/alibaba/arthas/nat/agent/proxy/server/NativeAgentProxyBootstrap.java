package com.alibaba.arthas.nat.agent.proxy.server;

import com.alibaba.arthas.nat.agent.common.constants.NativeAgentConstants;
import com.alibaba.arthas.nat.agent.common.utils.WelcomeUtil;
import com.alibaba.arthas.nat.agent.proxy.factory.NativeAgentProxyRegistryFactory;
import com.alibaba.arthas.nat.agent.proxy.registry.NativeAgentProxyRegistry;
import com.alibaba.arthas.nat.agent.proxy.server.handler.RequestHandler;
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
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @description: Native Agent Proxy Bootstrap
 * @author：flzjkl
 * @date: 2024-10-19 8:54
 */
@Name("arthas-native-agent-proxy")
@Summary("Bootstrap Arthas Native Agent Proxy")
@Description("EXAMPLES:\n" + "java -jar native-agent-proxy.jar --ip 151.159.27.114 --management-registration-type etcd --management-registration-address 161.169.97.114:2379 --agent-registration-type etcd --agent-registration-address 161.169.97.114:2379\n"
        + "java -jar native-agent-proxy.jar --ip 151.159.27.114 --port 2233 --management-registration-type etcd --management-registration-address 161.169.97.114:2379 --agent-registration-type etcd --agent-registration-address 161.169.97.114:2379\n"
        + "https://arthas.aliyun.com/doc\n")
public class NativeAgentProxyBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(NativeAgentProxyBootstrap.class);
    private static final int DEFAULT_NATIVE_AGENT_PROXY_PORT = 2233;

    private String ip;
    private Integer port;

    public static String managementRegistrationType;
    public static String agentRegistrationType;
    public static String managementRegistrationAddress;
    public static String agentRegistrationAddress;

    @Option(longName = "port")
    @Description("native agent proxy http/ws port, default 2233")
    public void setPort(Integer port) {
        this.port = port;
    }

    @Option(longName = "ip", required = true)
    @Description("ip")
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Option(longName = "management-registration-type", required = true)
    @Description("management registration type")
    public void setManagementRegistrationType(String managementRegistrationType) {
        this.managementRegistrationType = managementRegistrationType;
    }

    @Option(longName = "agent-registration-type", required = true)
    @Description("agent registration type")
    public void setAgentRegistrationType(String agentRegistrationType) {
        this.agentRegistrationType = agentRegistrationType;
    }

    @Option(longName = "management-registration-address", required = true)
    @Description("management registration address")
    public void setManagementRegistrationAddress(String managementRegistrationAddress) {
        this.managementRegistrationAddress = managementRegistrationAddress;
    }

    @Option(longName = "agent-registration-address", required = true)
    @Description("agent registration address")
    public void setAgentRegistrationAddress(String agentRegistrationAddress) {
        this.agentRegistrationAddress = agentRegistrationAddress;
    }

    public static void main(String[] args) {
        // Print welcome message
        WelcomeUtil.printProxyWelcomeMsg();

        // Startup parameter analysis
        logger.info("read input config...");
        NativeAgentProxyBootstrap nativeAgentProxyBootstrap = new NativeAgentProxyBootstrap();
        CLI cli = CLIConfigurator.define(NativeAgentProxyBootstrap.class);
        CommandLine commandLine = cli.parse(Arrays.asList(args));
        try {
            CLIConfigurator.inject(commandLine, nativeAgentProxyBootstrap);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("read input success!");


        // Register native agent proxy
        try {
            logger.info("register native agent proxy...");
            NativeAgentProxyRegistryFactory registerFactory = NativeAgentProxyRegistryFactory.getNativeAgentProxyRegistryFactory();
            NativeAgentProxyRegistry proxyRegistry = registerFactory.getNativeAgentProxyRegistry(nativeAgentProxyBootstrap.getManagementRegistrationType());
            String registerAddress = nativeAgentProxyBootstrap.getIp() + ":" + nativeAgentProxyBootstrap.getPortOrDefault();
            proxyRegistry.register(nativeAgentProxyBootstrap.getManagementRegistrationAddress()
                    , registerAddress
                    , registerAddress);
            logger.info("register native agent client success!");
        } catch (Exception e) {
            logger.error("register native agent client failed!");
            e.printStackTrace();
            System.exit(1);
        }

        // Start the http/ws server
        logger.info("start the server... port:{}", nativeAgentProxyBootstrap.getPortOrDefault());
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
                            ch.pipeline().addLast(new RequestHandler());
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                        }
                    });
            ChannelFuture f = b.bind(nativeAgentProxyBootstrap.getPortOrDefault()).sync();
            logger.info("start the http server success! port:{}", nativeAgentProxyBootstrap.getPortOrDefault());
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("The native agent proxy fails to start, port{}",nativeAgentProxyBootstrap.getPortOrDefault());
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            logger.info("shutdown native agent proxy");
        }

    }

    public int getPortOrDefault() {
        if (this.port == null) {
            return DEFAULT_NATIVE_AGENT_PROXY_PORT;
        } else {
            return this.port;
        }
    }

    public String getAgentRegistrationType() {
        return agentRegistrationType;
    }

    public String getManagementRegistrationType() {
        return managementRegistrationType;
    }

    public String getManagementRegistrationAddress() {
        return managementRegistrationAddress;
    }

    public String getAgentRegistrationAddress() {
        return agentRegistrationAddress;
    }

    public String getIp() {
        return ip;
    }
}

package com.alibaba.arthas.tunnel.server;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * 
 * @author hengyunabc 2019-08-09
 *
 */
public class TunnelServer {
    private final static Logger logger = LoggerFactory.getLogger(TunnelServer.class);

    private boolean ssl;
    private String host;
    private int port;

    private Map<String, AgentInfo> agentInfoMap = new ConcurrentHashMap<String, AgentInfo>();

    private Map<String, ClientConnectionInfo> clientConnectionInfoMap = new ConcurrentHashMap<String, ClientConnectionInfo>();

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-TunnelServer-boss", true));
    private EventLoopGroup workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-TunnelServer-worker", true));

    private Channel channel;

    public void start() throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (ssl) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new TunnelSocketServerInitializer(this, sslCtx));

        if (StringUtils.isBlank(host)) {
            channel = b.bind(port).sync().channel();
        } else {
            channel = b.bind(host, port).sync().channel();
        }

        logger.info("Tunnel server listen at {}:{}", host, port);

        workerGroup.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                agentInfoMap.entrySet().removeIf(e -> !e.getValue().getChannelHandlerContext().channel().isActive());
                clientConnectionInfoMap.entrySet()
                        .removeIf(e -> !e.getValue().getChannelHandlerContext().channel().isActive());
            }

        }, 60, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public Optional<AgentInfo> findAgent(String id) {
        return Optional.ofNullable(this.agentInfoMap.get(id));
    }

    public void addAgent(String id, AgentInfo agentInfo) {
        agentInfoMap.put(id, agentInfo);
    }

    public AgentInfo removeAgent(String id) {
        return agentInfoMap.remove(id);
    }

    public Optional<ClientConnectionInfo> findClientConnection(String id) {
        return Optional.ofNullable(this.clientConnectionInfoMap.get(id));
    }

    public void addClientConnectionInfo(String id, ClientConnectionInfo clientConnectionInfo) {
        clientConnectionInfoMap.put(id, clientConnectionInfo);
    }

    public ClientConnectionInfo removeClientConnectionInfo(String id) {
        return this.clientConnectionInfoMap.remove(id);
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Map<String, AgentInfo> getAgentInfoMap() {
        return agentInfoMap;
    }

    public void setAgentInfoMap(Map<String, AgentInfo> agentInfoMap) {
        this.agentInfoMap = agentInfoMap;
    }

    public Map<String, ClientConnectionInfo> getClientConnectionInfoMap() {
        return clientConnectionInfoMap;
    }

    public void setClientConnectionInfoMap(Map<String, ClientConnectionInfo> clientConnectionInfoMap) {
        this.clientConnectionInfoMap = clientConnectionInfoMap;
    }
}

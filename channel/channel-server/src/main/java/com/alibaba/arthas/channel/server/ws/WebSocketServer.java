package com.alibaba.arthas.channel.server.ws;

import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * @author gongdewei 2020/9/2
 */
public class WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private boolean ssl;
    private String host;
    private int port;

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-WebSocketServer-boss", true));
    private EventLoopGroup workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-WebSocketServer-worker", true));

    private Channel channel;

    @Autowired
    private AgentManageService agentManageService;

    @Autowired
    private ApiActionDelegateService apiActionDelegateService;

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
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(new WebSocketServerInitializer(this, sslCtx));

        if (StringUtils.isBlank(host)) {
            channel = b.bind(port).sync().channel();
        } else {
            channel = b.bind(host, port).sync().channel();
        }

        logger.info("WebSocket server listen at {}:{}", host, port);

    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        logger.info("WebSocket server is down");
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

    public AgentManageService getAgentManageService() {
        return agentManageService;
    }

    public ApiActionDelegateService getApiActionDelegateService() {
        return apiActionDelegateService;
    }
}

package com.alibaba.arthas.tunnel.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * 
 * @author hengyunabc 2019-08-28
 *
 */
public class ForwardClient {
    private final static Logger logger = LoggerFactory.getLogger(ForwardClient.class);
    private URI tunnelServerURI;
    private URI localServerURI;

    private EventLoopGroup group;
    private Channel channel;

    public ForwardClient(URI tunnelServerURI, URI localServerURI, EventLoopGroup group) {
        this.tunnelServerURI = tunnelServerURI;
        this.localServerURI = localServerURI;
        this.group = group;
    }

    public void start() throws URISyntaxException, SSLException, InterruptedException {
        String scheme = tunnelServerURI.getScheme() == null ? "ws" : tunnelServerURI.getScheme();
        final String host = tunnelServerURI.getHost() == null ? "127.0.0.1" : tunnelServerURI.getHost();
        final int port;
        if (tunnelServerURI.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = tunnelServerURI.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            logger.error("Only WS(S) is supported, uri: {}", tunnelServerURI);
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // connect to local server
        WebSocketClientHandshaker newHandshaker = WebSocketClientHandshakerFactory.newHandshaker(tunnelServerURI,
                WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
        final WebSocketClientProtocolHandler websocketClientHandler = new WebSocketClientProtocolHandler(newHandshaker);

        final ForwardClientSocketClientHandler forwardClientSocketClientHandler = new ForwardClientSocketClientHandler(
                localServerURI);

        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                }
                p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), websocketClientHandler,
                        forwardClientSocketClientHandler);
            }
        });

        channel = b.connect(tunnelServerURI.getHost(), port).sync().channel();
        logger.info("forward client connect to server success, uri: " + tunnelServerURI);
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

}

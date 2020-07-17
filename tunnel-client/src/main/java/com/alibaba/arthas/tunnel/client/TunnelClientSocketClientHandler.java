
package com.alibaba.arthas.tunnel.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * 
 * @author hengyunabc 2019-08-28
 *
 */
public class TunnelClientSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final static Logger logger = LoggerFactory.getLogger(TunnelClientSocketClientHandler.class);

    private final TunnelClient tunnelClient;
    private ChannelPromise registerPromise;

    public TunnelClientSocketClientHandler(TunnelClient tunnelClient) {
        this.tunnelClient = tunnelClient;
    }

    public ChannelFuture registerFuture() {
        return registerPromise;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        registerPromise = ctx.newPromise();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String text = textFrame.text();

            logger.info("receive TextWebSocketFrame: {}", text);

            QueryStringDecoder queryDecoder = new QueryStringDecoder(text);
            Map<String, List<String>> parameters = queryDecoder.parameters();
            List<String> methodList = parameters.get("method");
            String method = null;
            if (methodList != null && !methodList.isEmpty()) {
                method = methodList.get(0);
            }

            if ("agentRegister".equals(method)) {
                List<String> idList = parameters.get("id");
                if (idList != null && !idList.isEmpty()) {
                    this.tunnelClient.setId(idList.get(0));
                }
                registerPromise.setSuccess();
            }

            if ("startTunnel".equals(method)) {
                QueryStringEncoder queryEncoder = new QueryStringEncoder(this.tunnelClient.getTunnelServerUrl());
                queryEncoder.addParam("method", "openTunnel");
                queryEncoder.addParam("clientConnectionId", parameters.get("clientConnectionId").get(0));
                queryEncoder.addParam("id", parameters.get("id").get(0));

                final URI forwardUri = queryEncoder.toUri();

                logger.info("start ForwardClient, uri: {}", forwardUri);
                try {
                    ForwardClient forwardClient = new ForwardClient(forwardUri,
                            new URI(tunnelClient.getLocalServerUrl()));
                    forwardClient.start();
                } catch (Throwable e) {
                    logger.error("start ForwardClient error, forwardUri: {}, localServerUri: {}", forwardUri,
                            tunnelClient.getLocalServerUrl(), e);
                }
            }

        }
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                logger.error("try to reconnect to tunnel server, uri: {}", tunnelClient.getTunnelServerUrl());
                try {
                    tunnelClient.connect(true);
                } catch (Throwable e) {
                    logger.error("reconnect error", e);
                }
            }
        }, tunnelClient.getReconnectDelay(), TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!registerPromise.isDone()) {
            registerPromise.setFailure(cause);
        }
        ctx.fireExceptionCaught(cause);
    }
}

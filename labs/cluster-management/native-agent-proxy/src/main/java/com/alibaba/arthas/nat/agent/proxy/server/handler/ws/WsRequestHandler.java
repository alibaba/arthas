package com.alibaba.arthas.nat.agent.proxy.server.handler.ws;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: hello world
 * @author：flzjkl
 * @date: 2024-10-20 11:26
 */
public class WsRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(WsRequestHandler.class);
    private final ConcurrentHashMap<Channel, Channel> channelMappings = new ConcurrentHashMap<>();

    public void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            closeOutboundChannel(ctx.channel());
            ctx.close();
            return;
        }

        Channel outboundChannel = channelMappings.get(ctx.channel());
        if (outboundChannel == null || !outboundChannel.isActive()) {
            connectToDestinationServer(ctx, frame);
        } else {
            forwardWebSocketFrame(frame, outboundChannel);
        }
    }

    private void connectToDestinationServer(ChannelHandlerContext ctx, WebSocketFrame frame) {
        String nativeAgentAddress = (String) ctx.channel().attr(AttributeKey.valueOf("nativeAgentAddress")).get();
        Bootstrap b = new Bootstrap();
        b.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpClientCodec());
                        p.addLast(new HttpObjectAggregator(65536));
                        p.addLast(new WebSocketClientProtocolHandler(
                                WebSocketClientHandshakerFactory.newHandshaker(
                                        URI.create("ws://"+ nativeAgentAddress +"/ws"),
                                        WebSocketVersion.V13, null, false, new DefaultHttpHeaders())));
                        p.addLast(new WebSocketClientHandler(ctx.channel()));
                    }
                });
        String[] addressSplit = nativeAgentAddress.split(":");
        ChannelFuture f = b.connect(addressSplit[0], Integer.parseInt(addressSplit[1]));
        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Channel outboundChannel = future.channel();
                channelMappings.put(ctx.channel(), outboundChannel);
                forwardWebSocketFrame(frame, outboundChannel);
            } else {
                logger.error("Failed to connect to destination server", future.cause());
                ctx.close();
            }
        });
    }

    private void forwardWebSocketFrame(WebSocketFrame frame, Channel outboundChannel) {
        if (outboundChannel != null && outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(frame.retain()).addListener(future -> {
                if (!future.isSuccess()) {
                    logger.error("Failed to forward WebSocket frame", future.cause());
                }
            });
        } else {
            logger.warn("Outbound channel is not active. Cannot forward frame.");
        }
    }

    private void closeOutboundChannel(Channel inboundChannel) {
        Channel outboundChannel = channelMappings.remove(inboundChannel);
        if (outboundChannel != null) {
            logger.info("Closing outbound channel");
            outboundChannel.close();
        }
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Channel inactive, closing outbound channel");
        closeOutboundChannel(ctx.channel());
    }

    public void handleWebSocketUpgrade(ChannelHandlerContext ctx, FullHttpRequest request) {
        URI uri = null;
        try {
            uri = new URI(request.uri());
        } catch (URISyntaxException e) {
            // 处理异常
            return;
        }

        Map<String, String> params = parseQueryString(uri.getQuery());

        String nativeAgentAddress = params.get("nativeAgentAddress");

        if (nativeAgentAddress != null) {
            ctx.channel().attr(AttributeKey.valueOf("nativeAgentAddress")).set(nativeAgentAddress);
        }

        request.setUri(uri.getPath());

        ctx.fireChannelRead(request.retain());
    }

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // 处理异常
                }
            }
        }
        return params;
    }
}

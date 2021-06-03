package com.alibaba.arthas.tunnel.server;

import com.taobao.arthas.common.ArthasConstants;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;

/**
 * 
 * @author hengyunabc 2019-08-27
 *
 */
public class TunnelSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    private TunnelServer tunnelServer;

    public TunnelSocketServerInitializer(TunnelServer tunnelServer, SslContext sslCtx) {
        this.sslCtx = sslCtx;
        this.tunnelServer = tunnelServer;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));
        pipeline.addLast(new WebSocketServerCompressionHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(tunnelServer.getPath(), null, true, ArthasConstants.MAX_HTTP_CONTENT_LENGTH, false, true, 10000L));

        pipeline.addLast(new TunnelSocketFrameHandler(tunnelServer));
    }
}

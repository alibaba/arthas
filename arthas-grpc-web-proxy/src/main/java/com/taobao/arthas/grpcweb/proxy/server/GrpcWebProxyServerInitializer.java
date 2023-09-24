package com.taobao.arthas.grpcweb.proxy.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class GrpcWebProxyServerInitializer extends ChannelInitializer<SocketChannel> {

    private int grpcPort;

    public GrpcWebProxyServerInitializer(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new GrpcWebProxyHandler(grpcPort));
    }
}

package com.taobao.arthas.grpcweb.proxy.server;

import com.taobao.arthas.grpcweb.proxy.GrpcServiceConnectionManager;
import com.taobao.arthas.grpcweb.proxy.GrpcWebRequestHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class GrpcWebProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());
    private GrpcWebRequestHandler requestHandler;

    private static GrpcServiceConnectionManager manager;

    public GrpcWebProxyHandler(int grpcPort) {
        manager = new GrpcServiceConnectionManager(grpcPort);
        requestHandler = new GrpcWebRequestHandler(manager);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        logger.debug("http request: {} ", request);

        send100Continue(ctx);
        requestHandler.handle(ctx, request);
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("grpc web proxy handler error", cause);
        ctx.close();
    }

}

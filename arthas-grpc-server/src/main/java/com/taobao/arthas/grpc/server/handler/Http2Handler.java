package com.taobao.arthas.grpc.server.handler;/**
 * @author: 風楪
 * @date: 2024/7/7 下午9:58
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: FengYe
 * @date: 2024/7/7 下午9:58
 * @description: Http2Handler
 */
public class Http2Handler extends SimpleChannelInboundHandler<Http2Frame> {

    private GrpcDispatcher grpcDispatcher;

    /**
     * 暂存收到的所有请求的数据
     */
    private ConcurrentHashMap<Integer, GrpcRequest> dataBuffer = new ConcurrentHashMap<>();

    private static final String HEADER_PATH = ":path";

    public Http2Handler(GrpcDispatcher grpcDispatcher) {
        this.grpcDispatcher = grpcDispatcher;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) throws IOException {
        if (frame instanceof Http2HeadersFrame) {
            handleGrpcRequest((Http2HeadersFrame) frame, ctx);
        } else if (frame instanceof Http2DataFrame) {
            handleGrpcData((Http2DataFrame) frame, ctx);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void handleGrpcRequest(Http2HeadersFrame headersFrame, ChannelHandlerContext ctx) {
        int id = headersFrame.stream().id();
        String path = headersFrame.headers().get(HEADER_PATH).toString();
        // 去掉前面的斜杠，然后按斜杠分割
        String[] parts = path.substring(1).split("/");
        GrpcRequest grpcRequest = new GrpcRequest(headersFrame.stream().id(), parts[0], parts[1]);
        grpcDispatcher.checkGrpcStream(grpcRequest);
        dataBuffer.put(id, grpcRequest);
        System.out.println("Received headers: " + headersFrame.headers());
    }

    private void handleGrpcData(Http2DataFrame dataFrame, ChannelHandlerContext ctx) throws IOException {
        GrpcRequest grpcRequest = dataBuffer.get(dataFrame.stream().id());
        grpcRequest.writeData(dataFrame.content());

        if (grpcRequest.isStream()) {
            // 流式调用，即刻响应
            try {
                GrpcResponse response = grpcDispatcher.execute(grpcRequest);
                grpcRequest.clearData();

                // 针对第一个响应发送 header
                if (grpcRequest.isStreamFirstData()) {
                    ctx.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndHeader()).stream(dataFrame.stream()));
                    grpcRequest.setStreamFirstData(false);
                }

                ctx.writeAndFlush(new DefaultHttp2DataFrame(response.getResponseData()).stream(dataFrame.stream()));

                if (dataFrame.isEndStream()) {
                    ctx.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndStreamHeader(), true).stream(dataFrame.stream()));
                }
            } catch (Throwable e) {
                processError(ctx, e);
            }
        } else {
            // 非流式调用，等到 endStream 再响应
            if (dataFrame.isEndStream()) {
                try {
                    GrpcResponse response = grpcDispatcher.execute(grpcRequest);
                    ctx.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndHeader()).stream(dataFrame.stream()));
                    ctx.writeAndFlush(new DefaultHttp2DataFrame(response.getResponseData()).stream(dataFrame.stream()));
                    ctx.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndStreamHeader(), true).stream(dataFrame.stream()));
                } catch (Throwable e) {
                    processError(ctx, e);
                }
            }
        }
    }

    private void processError(ChannelHandlerContext ctx, Throwable e) {
        //todo
        ctx.writeAndFlush(e.getMessage());
    }
}
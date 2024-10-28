package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2DataFrame;

/**
 * @author: FengYe
 * @date: 2024/10/24 01:50
 * @description: GrpcProcessor
 */
public interface GrpcExecutor {
    GrpcInvokeTypeEnum supportGrpcType();

    void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable;
}

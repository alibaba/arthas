package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;

/**
 * @author: FengYe
 * @date: 2024/10/24 01:52
 * @description: BiStreamProcessor
 */
public class BiStreamExecutor extends AbstractGrpcExecutor {

    public BiStreamExecutor(GrpcDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public GrpcInvokeTypeEnum supportGrpcType() {
        return GrpcInvokeTypeEnum.BI_STREAM;
    }

    @Override
    public void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable {
        // todo 下面是迁移过来的，后面改掉
        // 流式调用，即刻响应

        GrpcResponse response = new GrpcResponse();
        byte[] bytes = request.readData();
        while (bytes != null) {
            response = dispatcher.doUnaryExecute(request.getService(), request.getMethod(), bytes);

            // 针对第一个响应发送 header
            if (request.isStreamFirstData()) {
                context.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndHeader()).stream(frame.stream()));
                request.setStreamFirstData(false);
            }
            context.writeAndFlush(new DefaultHttp2DataFrame(response.getResponseData()).stream(frame.stream()));

            bytes = request.readData();
        }

        request.clearData();

        if (frame.isEndStream()) {
            context.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndStreamHeader(), true).stream(frame.stream()));
        }
    }
}

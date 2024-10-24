package com.taobao.arthas.grpc.server.handler.executor;

import arthas.grpc.unittest.ArthasUnittest;
import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;

import java.util.Observer;

/**
 * @author: FengYe
 * @date: 2024/10/24 01:51
 * @description: UnaryProcessor
 */
public class ClientStreamExecutor extends AbstractGrpcExecutor {

    public ClientStreamExecutor(GrpcDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public GrpcInvokeTypeEnum supportGrpcType() {
        return GrpcInvokeTypeEnum.CLIENT_STREAM;
    }

    @Override
    public void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable {
        StreamObserver<GrpcResponse> responseObserver = new StreamObserver<GrpcResponse>() {
            @Override
            public void onNext(GrpcResponse res) {
                context.writeAndFlush(new DefaultHttp2HeadersFrame(res.getEndHeader()).stream(frame.stream()));
                context.writeAndFlush(new DefaultHttp2DataFrame(res.getResponseData()).stream(frame.stream()));
            }

            @Override
            public void onCompleted() {
                context.writeAndFlush(new DefaultHttp2HeadersFrame(GrpcResponse.getDefaultEndStreamHeader(), true).stream(frame.stream()));
            }
        };
        StreamObserver<GrpcRequest> requestObserver = dispatcher.clientStreamExecute(request, responseObserver);

        requestObserver.onNext(request);
        if (frame.isEndStream()) {
            requestObserver.onCompleted();
        }
    }
}

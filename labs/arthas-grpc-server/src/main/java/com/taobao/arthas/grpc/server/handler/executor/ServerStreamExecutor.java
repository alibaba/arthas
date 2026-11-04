package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: FengYe
 * @date: 2024/10/24 01:51
 * @description: UnaryProcessor
 */
public class ServerStreamExecutor extends AbstractGrpcExecutor {

    public ServerStreamExecutor(GrpcDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public GrpcInvokeTypeEnum supportGrpcType() {
        return GrpcInvokeTypeEnum.SERVER_STREAM;
    }

    @Override
    public void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable {
        StreamObserver<GrpcResponse> responseObserver = new StreamObserver<GrpcResponse>() {
            AtomicBoolean sendHeader = new AtomicBoolean(false);

            @Override
            public void onNext(GrpcResponse res) {
                // 控制流只能响应一次header
                if (!sendHeader.get()) {
                    sendHeader.compareAndSet(false, true);
                    context.writeAndFlush(new DefaultHttp2HeadersFrame(res.getEndHeader()).stream(frame.stream()));
                }
                context.writeAndFlush(new DefaultHttp2DataFrame(res.getResponseData()).stream(frame.stream()));
            }

            @Override
            public void onCompleted() {
                context.writeAndFlush(new DefaultHttp2HeadersFrame(GrpcResponse.getDefaultEndStreamHeader(), true).stream(frame.stream()));
            }
        };
        try {
            dispatcher.serverStreamExecute(request, responseObserver);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

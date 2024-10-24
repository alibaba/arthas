package com.taobao.arthas.grpc.server.service.impl;

import arthas.grpc.unittest.ArthasUnittest;
import com.google.protobuf.InvalidProtocolBufferException;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import com.taobao.arthas.grpc.server.service.ArthasSampleService;
import com.taobao.arthas.grpc.server.utils.ByteUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@GrpcService("arthas.grpc.unittest.ArthasUnittestService")
public class ArthasSampleServiceImpl implements ArthasSampleService {

    private AtomicInteger sum = new AtomicInteger(0);

    @Override
    @GrpcMethod("trace")
    public ArthasUnittest.ArthasUnittestResponse trace(ArthasUnittest.ArthasUnittestRequest request) {
        try {
            Thread.sleep(50000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();
        builder.setMessage(request.getMessage());
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "watch", grpcType = GrpcInvokeTypeEnum.BI_STREAM)
    public ArthasUnittest.ArthasUnittestResponse watch(ArthasUnittest.ArthasUnittestRequest request) {
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();
        builder.setMessage(request.getMessage());
        return builder.build();
    }

    @Override
    public StreamObserver<GrpcRequest> clientStreamSum(StreamObserver<GrpcResponse> observer) {
        return new StreamObserver<GrpcRequest>() {
            @Override
            public void onNext(GrpcRequest req) {
                try {
                    ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.parseFrom(ByteUtil.getBytes(req.getByteData()));
                    sum.addAndGet(request.getNum());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleted() {
                ArthasUnittest.ArthasUnittestResponse response = ArthasUnittest.ArthasUnittestResponse.newBuilder()
                        .setNum(sum.get())
                        .build();
                GrpcResponse grpcResponse = new GrpcResponse();
                grpcResponse.writeResponseData(response);
                observer.onNext(grpcResponse);
                observer.onCompleted();
            }
        };
    }
}
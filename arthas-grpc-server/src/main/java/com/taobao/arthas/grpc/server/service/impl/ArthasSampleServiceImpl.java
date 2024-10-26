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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@GrpcService("arthas.grpc.unittest.ArthasUnittestService")
public class ArthasSampleServiceImpl implements ArthasSampleService {

    private ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();

    @Override
    @GrpcMethod(value = "unary")
    public ArthasUnittest.ArthasUnittestResponse unary(ArthasUnittest.ArthasUnittestRequest command) {
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "unaryAddSum")
    public ArthasUnittest.ArthasUnittestResponse unaryAddSum(ArthasUnittest.ArthasUnittestRequest command) {
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        map.merge(command.getId(), command.getNum(), Integer::sum);
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "unaryGetSum")
    public ArthasUnittest.ArthasUnittestResponse unaryGetSum(ArthasUnittest.ArthasUnittestRequest command) {
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        Integer sum = map.getOrDefault(command.getId(), 0);
        builder.setNum(sum);
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "clientStreamSum", grpcType = GrpcInvokeTypeEnum.CLIENT_STREAM)
    public StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> clientStreamSum(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer) {
        return new StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>>() {
            AtomicInteger sum = new AtomicInteger(0);

            @Override
            public void onNext(GrpcRequest<ArthasUnittest.ArthasUnittestRequest> req) {
                try {
                    byte[] bytes = req.readData();
                    while (bytes != null && bytes.length != 0) {
                        ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.parseFrom(bytes);
                        sum.addAndGet(request.getNum());
                        bytes = req.readData();
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleted() {
                ArthasUnittest.ArthasUnittestResponse response = ArthasUnittest.ArthasUnittestResponse.newBuilder()
                        .setNum(sum.get())
                        .build();
                GrpcResponse<ArthasUnittest.ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();
                grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");
                grpcResponse.setMethod("clientStreamSum");
                grpcResponse.writeResponseData(response);
                observer.onNext(grpcResponse);
                observer.onCompleted();
            }
        };
    }

    @Override
    @GrpcMethod(value = "serverStream", grpcType = GrpcInvokeTypeEnum.SERVER_STREAM)
    public void serverStream(ArthasUnittest.ArthasUnittestRequest request, StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer) {

        for (int i = 0; i < 5; i++) {
            ArthasUnittest.ArthasUnittestResponse response = ArthasUnittest.ArthasUnittestResponse.newBuilder()
                    .setMessage("Server response " + i + " to " + request.getMessage())
                    .build();
            GrpcResponse<ArthasUnittest.ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();
            grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");
            grpcResponse.setMethod("serverStream");
            grpcResponse.writeResponseData(response);
            observer.onNext(grpcResponse);
        }
        observer.onCompleted();
    }

    @Override
    @GrpcMethod(value = "biStream", grpcType = GrpcInvokeTypeEnum.BI_STREAM)
    public StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> biStream(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer) {
        return new StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>>() {
            @Override
            public void onNext(GrpcRequest<ArthasUnittest.ArthasUnittestRequest> req) {
                try {
                    byte[] bytes = req.readData();
                    while (bytes != null && bytes.length != 0) {
                        GrpcResponse<ArthasUnittest.ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();
                        grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");
                        grpcResponse.setMethod("biStream");
                        grpcResponse.writeResponseData(ArthasUnittest.ArthasUnittestResponse.parseFrom(bytes));
                        observer.onNext(grpcResponse);
                        bytes = req.readData();
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }
        };
    }
}
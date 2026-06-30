package unittest.grpc.service.impl;

import arthas.grpc.unittest.ArthasUnittest;
import arthas.grpc.unittest.ArthasUnittest.ArthasUnittestRequest;
import arthas.grpc.unittest.ArthasUnittest.ArthasUnittestResponse;
import com.google.protobuf.InvalidProtocolBufferException;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import unittest.grpc.service.ArthasUnittestService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@GrpcService("arthas.grpc.unittest.ArthasUnittestService")
public class ArthasUnittestServiceImpl implements ArthasUnittestService {

    private ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();

    @Override
    @GrpcMethod(value = "unary")
    public ArthasUnittestResponse unary(ArthasUnittestRequest command) {
        ArthasUnittestResponse.Builder builder = ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "unaryAddSum")
    public ArthasUnittestResponse unaryAddSum(ArthasUnittestRequest command) {
        ArthasUnittestResponse.Builder builder = ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        map.merge(command.getId(), command.getNum(), Integer::sum);
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "unaryGetSum")
    public ArthasUnittestResponse unaryGetSum(ArthasUnittestRequest command) {
        ArthasUnittestResponse.Builder builder = ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        Integer sum = map.getOrDefault(command.getId(), 0);
        builder.setNum(sum);
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "clientStreamSum", grpcType = GrpcInvokeTypeEnum.CLIENT_STREAM)
    public StreamObserver<GrpcRequest<ArthasUnittestRequest>> clientStreamSum(StreamObserver<GrpcResponse<ArthasUnittestResponse>> observer) {
        return new StreamObserver<GrpcRequest<ArthasUnittestRequest>>() {
            AtomicInteger sum = new AtomicInteger(0);

            @Override
            public void onNext(GrpcRequest<ArthasUnittestRequest> req) {
                try {
                    byte[] bytes = req.readData();
                    while (bytes != null && bytes.length != 0) {
                        ArthasUnittestRequest request = ArthasUnittestRequest.parseFrom(bytes);
                        sum.addAndGet(request.getNum());
                        bytes = req.readData();
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleted() {
                ArthasUnittestResponse response = ArthasUnittestResponse.newBuilder()
                        .setNum(sum.get())
                        .build();
                GrpcResponse<ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();
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
    public void serverStream(ArthasUnittestRequest request, StreamObserver<GrpcResponse<ArthasUnittestResponse>> observer) {

        for (int i = 0; i < 5; i++) {
            ArthasUnittest.ArthasUnittestResponse response = ArthasUnittest.ArthasUnittestResponse.newBuilder()
                    .setMessage("Server response " + i + " to " + request.getMessage())
                    .build();
            GrpcResponse<ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();
            grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");
            grpcResponse.setMethod("serverStream");
            grpcResponse.writeResponseData(response);
            observer.onNext(grpcResponse);
        }
        observer.onCompleted();
    }

    @Override
    @GrpcMethod(value = "biStream", grpcType = GrpcInvokeTypeEnum.BI_STREAM)
    public StreamObserver<GrpcRequest<ArthasUnittestRequest>> biStream(StreamObserver<GrpcResponse<ArthasUnittestResponse>> observer) {
        return new StreamObserver<GrpcRequest<ArthasUnittestRequest>>() {
            @Override
            public void onNext(GrpcRequest<ArthasUnittestRequest> req) {
                try {
                    byte[] bytes = req.readData();
                    while (bytes != null && bytes.length != 0) {
                        GrpcResponse<ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();
                        grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");
                        grpcResponse.setMethod("biStream");
                        grpcResponse.writeResponseData(ArthasUnittestResponse.parseFrom(bytes));
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
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
import com.taobao.arthas.grpc.server.utils.ByteUtil;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import org.junit.platform.commons.util.CollectionUtils;
import unittest.grpc.service.ArthasSampleService;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@GrpcService("arthas.grpc.unittest.ArthasUnittestService")
public class ArthasSampleServiceImpl implements ArthasSampleService {

    private AtomicInteger sum = new AtomicInteger(0);

    private AtomicInteger num = new AtomicInteger(0);

    private ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();

    @Override
    @GrpcMethod("trace")
    public ArthasUnittestResponse trace(ArthasUnittestRequest command) {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ArthasUnittestResponse.Builder builder = ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        return builder.build();
    }

    @Override
//    @GrpcMethod(value = "watch", grpcType = GrpcInvokeTypeEnum.BI_STREAM)
    public ArthasUnittestResponse watch(ArthasUnittestRequest command) {
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
            @Override
            public void onNext(GrpcRequest<ArthasUnittestRequest> req) {
                try {
                    byte[] bytes = req.readData();
                    while (bytes != null && bytes.length != 0) {
                        ArthasUnittestRequest request = ArthasUnittestRequest.parseFrom(bytes);
                        sum.addAndGet(request.getNum());
                        bytes = req.readData();
//                        System.out.println(num.addAndGet(1));
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
                //todo 待优化
                grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");
                grpcResponse.setMethod("clientStreamSum");
                grpcResponse.writeResponseData(response);
                observer.onNext(grpcResponse);
                observer.onCompleted();
            }
        };
    }
}
package com.taobao.arthas.grpc.server.service;

import arthas.grpc.unittest.ArthasUnittest;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;


/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:42
 * @description: ArthasSampleService
 */
public interface ArthasSampleService {
    ArthasUnittest.ArthasUnittestResponse unary(ArthasUnittest.ArthasUnittestRequest command);

    ArthasUnittest.ArthasUnittestResponse unaryAddSum(ArthasUnittest.ArthasUnittestRequest command);

    ArthasUnittest.ArthasUnittestResponse unaryGetSum(ArthasUnittest.ArthasUnittestRequest command);

    StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> clientStreamSum(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer);

    void serverStream(ArthasUnittest.ArthasUnittestRequest request, StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer);

    StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> biStream(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer);
}

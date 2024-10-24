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
    ArthasUnittest.ArthasUnittestResponse trace(ArthasUnittest.ArthasUnittestRequest request);
    ArthasUnittest.ArthasUnittestResponse watch(ArthasUnittest.ArthasUnittestRequest request);
    StreamObserver<GrpcRequest> clientStreamSum(StreamObserver<GrpcResponse> observer);
}

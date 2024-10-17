package com.taobao.arthas.grpc.server.service.impl;

import arthas.grpc.unittest.ArthasUnittest;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.service.ArthasSampleService;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@GrpcService("arthas.grpc.unittest.ArthasUnittestService")
public class ArthasSampleServiceImpl implements ArthasSampleService {

    @Override
    @GrpcMethod("trace")
    public ArthasUnittest.ArthasUnittestResponse trace(ArthasUnittest.ArthasUnittestRequest command) {
        try {
            Thread.sleep(50000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        return builder.build();
    }

    @Override
    @GrpcMethod(value = "watch", stream = true)
    public ArthasUnittest.ArthasUnittestResponse watch(ArthasUnittest.ArthasUnittestRequest command) {
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();
        builder.setMessage(command.getMessage());
        return builder.build();
    }
}
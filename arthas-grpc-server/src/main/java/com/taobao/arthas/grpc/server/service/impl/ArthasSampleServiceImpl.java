package com.taobao.arthas.grpc.server.service.impl;

import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.service.ArthasSampleService;
import com.taobao.arthas.grpc.server.service.req.ArthasUnittestRequest;
import com.taobao.arthas.grpc.server.service.res.ArthasUnittestResponse;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@GrpcService("arthas.sample.ArthasTempService")
public class ArthasSampleServiceImpl implements ArthasSampleService {

    @Override
    @GrpcMethod("trace")
    public ArthasUnittestResponse trace(ArthasUnittestRequest command) {
        ArthasUnittestResponse arthasUnittestResponse = new ArthasUnittestResponse();
        arthasUnittestResponse.setMessage("trace");
        return arthasUnittestResponse;
    }

    @Override
    @GrpcMethod(value = "watch", stream = true)
    public ArthasUnittestResponse watch(ArthasUnittestRequest command) {
        String message = command.getMessage();
        ArthasUnittestResponse arthasUnittestResponse = new ArthasUnittestResponse();
        arthasUnittestResponse.setMessage(message);
        return arthasUnittestResponse;
    }
}
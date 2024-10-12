package unittest.grpc.service.impl;

import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import unittest.grpc.service.ArthasSampleService;
import unittest.grpc.service.req.ArthasUnittestRequest;
import unittest.grpc.service.res.ArthasUnittestResponse;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@GrpcService("arthas.unittest.ArthasUnittestService")
public class ArthasSampleServiceImpl implements ArthasSampleService {

    @Override
    @GrpcMethod("trace")
    public ArthasUnittestResponse trace(ArthasUnittestRequest command) {
        ArthasUnittestResponse arthasUnittestResponse = new ArthasUnittestResponse();
        arthasUnittestResponse.setMessage(command.getMessage());
        return arthasUnittestResponse;
    }

    @Override
    @GrpcMethod(value = "watch", stream = true)
    public ArthasUnittestResponse watch(ArthasUnittestRequest command) {
        ArthasUnittestResponse arthasUnittestResponse = new ArthasUnittestResponse();
        arthasUnittestResponse.setMessage(command.getMessage());
        return arthasUnittestResponse;
    }
}
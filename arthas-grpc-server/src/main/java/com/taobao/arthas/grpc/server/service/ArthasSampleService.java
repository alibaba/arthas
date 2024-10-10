package com.taobao.arthas.grpc.server.service;

import com.taobao.arthas.grpc.server.service.req.ArthasUnittestRequest;
import com.taobao.arthas.grpc.server.service.res.ArthasUnittestResponse;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:42
 * @description: ArthasSampleService
 */
public interface ArthasSampleService {
    ArthasUnittestResponse trace(ArthasUnittestRequest command);
    ArthasUnittestResponse watch(ArthasUnittestRequest command);
}

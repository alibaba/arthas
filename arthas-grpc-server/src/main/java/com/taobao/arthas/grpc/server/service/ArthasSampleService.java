package com.taobao.arthas.grpc.server.service;/**
 * @author: 風楪
 * @date: 2024/6/30 下午11:42
 */

import com.taobao.arthas.grpc.server.service.req.ArthasSampleRequest;
import com.taobao.arthas.grpc.server.service.res.ArthasSampleResponse;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:42
 * @description: ArthasSampleService
 */
public interface ArthasSampleService {
    ArthasSampleResponse trace(ArthasSampleRequest command);
    ArthasSampleResponse watch(ArthasSampleRequest command);
}

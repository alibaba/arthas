package unittest.grpc.service;

import unittest.grpc.service.req.ArthasUnittestRequest;
import unittest.grpc.service.res.ArthasUnittestResponse;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:42
 * @description: ArthasSampleService
 */
public interface ArthasSampleService {
    ArthasUnittestResponse trace(ArthasUnittestRequest command);
    ArthasUnittestResponse watch(ArthasUnittestRequest command);
}

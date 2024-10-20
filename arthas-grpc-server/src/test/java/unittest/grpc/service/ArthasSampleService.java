package unittest.grpc.service;

import arthas.grpc.unittest.ArthasUnittest;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:42
 * @description: ArthasSampleService
 */
public interface ArthasSampleService {
    ArthasUnittest.ArthasUnittestResponse trace(ArthasUnittest.ArthasUnittestRequest command);
    ArthasUnittest.ArthasUnittestResponse watch(ArthasUnittest.ArthasUnittestRequest command);
    ArthasUnittest.ArthasUnittestResponse addSum(ArthasUnittest.ArthasUnittestRequest command);
    ArthasUnittest.ArthasUnittestResponse getSum(ArthasUnittest.ArthasUnittestRequest command);
}

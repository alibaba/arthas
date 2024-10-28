package unittest.grpc.service;

import arthas.grpc.unittest.ArthasUnittest.ArthasUnittestRequest;
import arthas.grpc.unittest.ArthasUnittest.ArthasUnittestResponse;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:42
 * @description: ArthasSampleService
 */
public interface ArthasUnittestService {
    ArthasUnittestResponse unary(ArthasUnittestRequest command);

    ArthasUnittestResponse unaryAddSum(ArthasUnittestRequest command);

    ArthasUnittestResponse unaryGetSum(ArthasUnittestRequest command);

    StreamObserver<GrpcRequest<ArthasUnittestRequest>> clientStreamSum(StreamObserver<GrpcResponse<ArthasUnittestResponse>> observer);

    void serverStream(ArthasUnittestRequest request, StreamObserver<GrpcResponse<ArthasUnittestResponse>> observer);

    StreamObserver<GrpcRequest<ArthasUnittestRequest>> biStream(StreamObserver<GrpcResponse<ArthasUnittestResponse>> observer);
}

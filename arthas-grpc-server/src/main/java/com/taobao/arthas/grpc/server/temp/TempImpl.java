package com.taobao.arthas.grpc.server.temp;/**
 * @author: 風楪
 * @date: 2024/9/16 01:59
 */


import arthasSample.ArthasSample;
import arthasSample.ArthasTempServiceGrpc;
import io.grpc.stub.StreamObserver;

/**
 * @author: FengYe
 * @date: 2024/9/16 01:59
 * @description: TempImpl
 */
public class TempImpl extends ArthasTempServiceGrpc.ArthasTempServiceImplBase {

    @Override
    public void trace(ArthasSample.ArthasSampleRequest request, StreamObserver<ArthasSample.ArthasSampleResponse> responseObserver) {
        ArthasSample.ArthasSampleResponse.Builder builder = ArthasSample.ArthasSampleResponse.newBuilder();
        builder.setMessage("trace");
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<ArthasSample.ArthasSampleRequest> watch(StreamObserver<ArthasSample.ArthasSampleResponse> responseObserver) {
        return new StreamObserver<ArthasSample.ArthasSampleRequest>() {
            @Override
            public void onNext(ArthasSample.ArthasSampleRequest value) {

                // 回应客户端
                ArthasSample.ArthasSampleResponse response = ArthasSample.ArthasSampleResponse.newBuilder()
                        .setMessage(value.getName())
                        .build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}

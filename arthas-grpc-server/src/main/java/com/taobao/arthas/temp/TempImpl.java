package com.taobao.arthas.temp;/**
 * @author: 風楪
 * @date: 2024/8/13 01:57
 */

import arthasSample.ArthasSample;
import arthasSample.ArthasTempServiceGrpc;
import io.grpc.stub.StreamObserver;

/**
 * @author: FengYe
 * @date: 2024/8/13 01:57
 * @description: TempImpl
 */
public class TempImpl extends ArthasTempServiceGrpc.ArthasTempServiceImplBase {
    @Override
    public void sayHello(ArthasSample.ArthasSampleRequest request, StreamObserver<ArthasSample.ArthasSampleResponse> responseObserver) {
        ArthasSample.ArthasSampleResponse build = ArthasSample.ArthasSampleResponse.newBuilder().setMessage("Hello ArthasSample!").build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }
}

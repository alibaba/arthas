package com.taobao.arthas.grpcweb.demo.service;

import arthas.grpc.api.ArthasService;
import arthas.grpc.api.ObjectServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ObjectService extends ObjectServiceGrpc.ObjectServiceImplBase {

    @Override
    public void get(ArthasService.StringValue request, StreamObserver<ArthasService.StringValue> observerResponse){
        String str = request.getValue();
        ArthasService.StringValue response = ArthasService.StringValue.newBuilder().setValue(str).build();
        observerResponse.onNext(response);
        observerResponse.onCompleted();
    }

}

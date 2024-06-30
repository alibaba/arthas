package com.taobao.arthas.service.impl;/**
 * @author: 風楪
 * @date: 2024/6/30 下午6:42
 */

import helloworld.HelloServiceGrpc;
import helloworld.Test;
import io.grpc.stub.StreamObserver;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午6:42
 * @description: HelloServiceImpl
 */
public class HelloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {
    @Override
    public void sayHello(Test.HelloRequest request, StreamObserver<Test.HelloReply> responseObserver) {
        String name = request.getName();
        System.out.println(name);
        responseObserver.onNext(Test.HelloReply.newBuilder().setMessage(name).build());
        responseObserver.onCompleted();
    }
}

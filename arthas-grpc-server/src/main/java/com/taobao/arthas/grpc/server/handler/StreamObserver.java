package com.taobao.arthas.grpc.server.handler;

/**
 * @author: FengYe
 * @date: 2024/10/24 00:22
 * @description: StreamObserver
 */
public interface StreamObserver<V>  {

    void onNext(V req);

    void onCompleted();
}

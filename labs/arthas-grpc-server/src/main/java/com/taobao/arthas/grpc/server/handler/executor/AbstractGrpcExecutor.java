package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: FengYe
 * @date: 2024/10/24 02:07
 * @description: AbstractGrpcExecutor
 */
public abstract class AbstractGrpcExecutor implements GrpcExecutor{
    protected GrpcDispatcher dispatcher;

    protected ConcurrentHashMap<Integer, StreamObserver<GrpcRequest>> requestStreamObserverMap = new ConcurrentHashMap<>();

    public AbstractGrpcExecutor(GrpcDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}

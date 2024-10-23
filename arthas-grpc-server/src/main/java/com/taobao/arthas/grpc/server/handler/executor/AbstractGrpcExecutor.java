package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;

/**
 * @author: FengYe
 * @date: 2024/10/24 02:07
 * @description: AbstractGrpcExecutor
 */
public abstract class AbstractGrpcExecutor implements GrpcExecutor{
    protected GrpcDispatcher dispatcher;

    public AbstractGrpcExecutor(GrpcDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}

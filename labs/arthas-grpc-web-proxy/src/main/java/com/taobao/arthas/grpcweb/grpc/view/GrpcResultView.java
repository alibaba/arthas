package com.taobao.arthas.grpcweb.grpc.view;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

/**
 * Command result view for grpc client.
 * Note: Result view is a reusable and stateless instance
 *
 * @author xuyang 2023/8/15
 */
public abstract class GrpcResultView<T extends ResultModel> {

    /**
     * formatted printing data to grpc client
     *
     * @param arthasStreamObserver
     */
    public abstract void draw(ArthasStreamObserver arthasStreamObserver, T result);

}

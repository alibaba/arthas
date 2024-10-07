package com.taobao.arthas.grpcweb.grpc.view;

import io.arthas.api.ArthasServices.ResponseBody;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

public class GrpcMessageView extends GrpcResultView<MessageModel> {
    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, MessageModel result) {
        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setJobId(result.getJobId())
                .setType(result.getType())
                .setStringValue(result.getMessage())
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}

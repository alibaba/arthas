package com.taobao.arthas.grpcweb.grpc.view;

import arthas.grpc.api.ArthasService;
import arthas.grpc.api.ArthasService.ResponseBody;
import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

public class GrpcSystemPropertyView extends GrpcResultView<SystemPropertyModel>{

    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, SystemPropertyModel result) {
        ArthasService.StringStringMapValue stringStringMapValue = ArthasService.StringStringMapValue.newBuilder()
                .putAllStringStringMap(result.getProps()).build();
        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setJobId(result.getJobId())
                .setType(result.getType())
                .setStringStringMapValue(stringStringMapValue)
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}

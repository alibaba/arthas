package com.taobao.arthas.grpcweb.grpc.view;

import arthas.grpc.api.ArthasService;
import arthas.grpc.api.ArthasService.ResponseBody;
import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

/**
 * @author xuyang 2023/8/15
 */
public class GrpcPwdView extends GrpcResultView<PwdModel> {


    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, PwdModel result) {
        ArthasService.StringStringMapValue stringStringMapValue = ArthasService.StringStringMapValue.newBuilder()
                .putStringStringMap("workingDir", result.getWorkingDir()).build();
        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setJobId(result.getJobId())
                .setType(result.getType())
                .setStringStringMapValue(stringStringMapValue)
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}

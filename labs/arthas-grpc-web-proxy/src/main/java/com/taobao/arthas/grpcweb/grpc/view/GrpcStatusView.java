package com.taobao.arthas.grpcweb.grpc.view;

import io.arthas.api.ArthasServices.ResponseBody;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

/**
 * @author xuyang 2023/8/15
 */
public class GrpcStatusView extends GrpcResultView<StatusModel> {

    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, StatusModel result) {
        if (result.getMessage() != null) {
            ResponseBody responseBody  = ResponseBody.newBuilder()
                    .setJobId(result.getJobId())
                    .setType(result.getType())
                    .setStringValue(result.getMessage())
                    .build();
            arthasStreamObserver.onNext(responseBody);
        }
    }
}

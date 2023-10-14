package com.taobao.arthas.grpcweb.grpc.view;

import io.arthas.api.ArthasServices.ResponseBody;
import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.command.view.ViewRenderUtil;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

/**
 * Term grpc view for EnhancerModel
 * @author xuyang 2023/8/15
 */
public class GrpcEnhancerView extends GrpcResultView<EnhancerModel> {
    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, EnhancerModel result) {
        if (result.getEffect() != null) {
            String msg = ViewRenderUtil.renderEnhancerAffect(result.getEffect());
            ResponseBody responseBody  = ResponseBody.newBuilder()
                    .setJobId(result.getJobId())
                    .setType(result.getType())
                    .setStringValue(msg)
                    .build();
            arthasStreamObserver.onNext(responseBody);
        }
    }
}

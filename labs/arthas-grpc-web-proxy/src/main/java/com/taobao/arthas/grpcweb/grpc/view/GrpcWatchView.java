package com.taobao.arthas.grpcweb.grpc.view;

import com.taobao.arthas.core.view.ObjectView;
import com.taobao.arthas.grpcweb.grpc.model.WatchRequestModel;
import io.arthas.api.ArthasServices.JavaObject;
import io.arthas.api.ArthasServices.ResponseBody;
import io.arthas.api.ArthasServices.WatchResponse;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.grpcweb.grpc.model.WatchResponseModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

import static com.taobao.arthas.grpcweb.grpc.objectUtils.JavaObjectConverter.toJavaObjectWithExpand;

/**
 * Term view for WatchModel
 *
 * @author xuyang 2023/8/15
 */
public class GrpcWatchView extends GrpcResultView<WatchResponseModel> {

    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, WatchResponseModel model) {
        ObjectVO objectVO = model.getValue();
//        Object obj = objectVO.needExpand() ? new ObjectView(model.getSizeLimit(), objectVO).draw() : objectVO.getObject();
        JavaObject javaObject = toJavaObjectWithExpand(objectVO.getObject(), objectVO.getExpand());
        WatchResponse watchResponse = WatchResponse.newBuilder()
                .setAccessPoint(model.getAccessPoint())
                .setClassName(model.getClassName())
                .setCost(model.getCost())
                .setMethodName(model.getMethodName())
                .setSizeLimit(model.getSizeLimit())
                .setTs(DateUtils.formatDateTime(model.getTs()))
                .setValue(javaObject)
                .build();
        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setJobId(model.getJobId())
                .setResultId(model.getResultId())
                .setType(model.getType())
                .setWatchResponse(watchResponse)
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}

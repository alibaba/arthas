package com.taobao.arthas.grpcweb.grpc.model;

import com.taobao.arthas.core.command.model.WatchModel;

public class WatchResponseModel extends WatchModel {

    private long resultId;

    public long getResultId() {
        return resultId;
    }

    public void setResultId(long resultId) {
        this.resultId = resultId;
    }
}

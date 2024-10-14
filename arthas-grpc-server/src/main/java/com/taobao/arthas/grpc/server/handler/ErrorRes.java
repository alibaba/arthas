package com.taobao.arthas.grpc.server.handler;

import com.taobao.arthas.grpc.server.protobuf.annotation.ProtobufClass;

/**
 * @author: FengYe
 * @date: 2024/9/23 23:58
 * @description: ErrorRes
 */
@ProtobufClass
@Deprecated
public class ErrorRes {
    private String errorMsg;

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}

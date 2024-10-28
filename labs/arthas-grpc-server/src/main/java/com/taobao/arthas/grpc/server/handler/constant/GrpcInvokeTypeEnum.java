package com.taobao.arthas.grpc.server.handler.constant;

/**
 * @author: FengYe
 * @date: 2024/10/24 01:06
 * @description: StreamTypeEnum
 */
public enum GrpcInvokeTypeEnum {
    UNARY,
    SERVER_STREAM,
    CLIENT_STREAM,
    BI_STREAM;
}

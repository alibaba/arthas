package com.taobao.arthas.grpc.server.handler;

import java.util.List;

/**
 * @author: FengYe
 * @date: 2024/9/18 23:12
 * @description: 一个 http2 的 frame 中可能存在多个 grpc 的请求体
 */
public class Http2FrameRequest {

    /**
     * grpc 请求体
     */
    private List<GrpcRequest> grpcRequests;
}

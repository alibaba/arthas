package com.taobao.arthas.grpc.server.handler;

import java.util.List;

/**
 * HTTP/2 帧请求封装类
 *
 * <p>该类用于封装一个 HTTP/2 帧中包含的多个 gRPC 请求体。
 *
 * <p>背景说明：
 * <ul>
 *   <li>HTTP/2 协议支持多路复用，单个连接上可以同时传输多个帧</li>
 *   <li>一个 HTTP/2 帧可能包含多个连续的 gRPC 消息</li>
 *   <li>每个 gRPC 消息都是一个独立的 GrpcRequest 对象</li>
 * </ul>
 *
 * <p>使用场景：
 * <ul>
 *   <li>批量处理：客户端一次性发送多个请求</li>
 *   <li>流式请求：单个帧中包含流式调用的多个数据块</li>
 *   <li>管道化：提高网络利用率，减少往返延迟</li>
 * </ul>
 *
 * @author FengYe
 * @date 2024/9/18 23:12
 * @description 一个 HTTP/2 帧中可能存在多个 gRPC 请求体
 */
public class Http2FrameRequest {

    /**
     * gRPC 请求体列表
     *
     * <p>存储一个 HTTP/2 帧中包含的所有 gRPC 请求。
     * 每个元素都是一个完整的 GrpcRequest 对象，包含：
     * <ul>
     *   <li>流标识符（streamId）</li>
     *   <li>服务名和方法名</li>
     *   <li>请求参数数据</li>
     *   <li>HTTP/2 头部信息</li>
     * </ul>
     *
     * <p>注意：虽然 HTTP/2 协议本身支持在一个帧中传输多个流的数据，
     * 但在实际使用中，通常一个帧对应一个流的数据。该类的存在是为了处理
     * 特殊场景下的多请求聚合。
     */
    private List<GrpcRequest> grpcRequests;

    /**
     * 获取 gRPC 请求体列表
     *
     * @return 包含所有 gRPC 请求的列表
     */
    public List<GrpcRequest> getGrpcRequests() {
        return grpcRequests;
    }

    /**
     * 设置 gRPC 请求体列表
     *
     * @param grpcRequests 包含所有 gRPC 请求的列表
     */
    public void setGrpcRequests(List<GrpcRequest> grpcRequests) {
        this.grpcRequests = grpcRequests;
    }
}

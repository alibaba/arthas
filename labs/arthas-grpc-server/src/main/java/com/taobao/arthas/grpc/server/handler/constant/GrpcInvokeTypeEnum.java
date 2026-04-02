package com.taobao.arthas.grpc.server.handler.constant;

/**
 * gRPC调用类型枚举
 * 定义了gRPC服务器支持的四种不同的调用模式
 *
 * @author: FengYe
 * @date: 2024/10/24 01:06
 * @description: gRPC调用类型枚举，用于区分不同的RPC调用方式
 */
public enum GrpcInvokeTypeEnum {
    /**
     * 一元调用（Unary RPC）
     * 客户端发送一个请求，服务器返回一个响应
     * 这是最简单的RPC调用方式，类似于普通的函数调用
     */
    UNARY,

    /**
     * 服务端流式调用（Server Streaming RPC）
     * 客户端发送一个请求，服务器返回一个响应序列（流）
     * 客户端从返回的流中读取多个响应消息
     */
    SERVER_STREAM,

    /**
     * 客户端流式调用（Client Streaming RPC）
     * 客户端发送一个请求序列（流），服务器返回一个响应
     * 服务器在接收到所有客户端请求后返回一个响应
     */
    CLIENT_STREAM,

    /**
     * 双向流式调用（Bidirectional Streaming RPC）
     * 客户端和服务器都可以通过读写流来发送消息序列
     * 两个流独立运行，客户端和服务器可以按任意顺序读写
     */
    BI_STREAM;
}

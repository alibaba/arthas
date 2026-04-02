package com.taobao.arthas.grpc.server.service;

import arthas.grpc.unittest.ArthasUnittest;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;


/**
 * Arthas 示例 gRPC 服务接口
 *
 * 该接口定义了 Arthas gRPC 服务器提供的各种服务方法。
 * 它支持四种 gRPC 通信模式：
 * 1. 一元调用（Unary）：客户端发送一个请求，服务器返回一个响应
 * 2. 客户端流式调用（Client Streaming）：客户端发送多个请求，服务器返回一个响应
 * 3. 服务器流式调用（Server Streaming）：客户端发送一个请求，服务器返回多个响应
 * 4. 双向流式调用（Bidirectional Streaming）：客户端和服务器都可以发送多个消息
 *
 * 所有方法都使用 Protocol Buffers 定义的消息格式进行通信。
 *
 * @author: FengYe
 * @date: 2024/6/30 下午11:42
 * @description: ArthasSampleService
 */
public interface ArthasSampleService {

    /**
     * 一元调用方法 - 执行简单的命令请求
     *
     * 这是最基本的 gRPC 调用模式。客户端发送一个请求对象，
     * 服务器处理后返回一个响应对象。该方法通常用于执行
     * 单次操作或查询。
     *
     * @param command Arthas 单元测试请求对象，包含要执行的命令和参数
     * @return ArthasUnittestResponse 响应对象，包含命令执行的结果
     */
    ArthasUnittest.ArthasUnittestResponse unary(ArthasUnittest.ArthasUnittestRequest command);

    /**
     * 一元调用方法 - 添加数值到求和器
     *
     * 该方法将请求中的数值添加到服务端的累加器中。
     * 这是一个简单的一元调用，用于演示基本的数据操作。
     *
     * @param command Arthas 单元测试请求对象，包含要累加的数值
     * @return ArthasUnittestResponse 响应对象，包含操作确认信息
     */
    ArthasUnittest.ArthasUnittestResponse unaryAddSum(ArthasUnittest.ArthasUnittestRequest command);

    /**
     * 一元调用方法 - 获取当前求和结果
     *
     * 该方法返回服务端累加器中的当前总和。
     * 与 unaryAddSum 配合使用，演示状态管理功能。
     *
     * @param command Arthas 单元测试请求对象（可能包含查询参数）
     * @return ArthasUnittestResponse 响应对象，包含当前的总和值
     */
    ArthasUnittest.ArthasUnittestResponse unaryGetSum(ArthasUnittest.ArthasUnittestRequest command);

    /**
     * 客户端流式调用 - 客户端流式求和
     *
     * 该方法实现了客户端流式 RPC 模式。客户端可以通过返回的 StreamObserver
     * 连续发送多个请求，服务器会累积所有请求中的数值。
     * 当客户端完成发送后，服务器会返回最终的总和。
     *
     * 使用场景：
     * - 批量数据提交
     * - 数据流上传
     * - 需要客户端持续发送数据的场景
     *
     * @param observer 用于向客户端发送最终响应的流观察者
     * @return StreamObserver 客户端通过此对象发送多个请求
     */
    StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> clientStreamSum(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer);

    /**
     * 服务器流式调用 - 服务器流式推送数据
     *
     * 该方法实现了服务器流式 RPC 模式。客户端发送一个请求后，
     * 服务器可以通过 StreamObserver 连续发送多个响应。
     * 适合用于服务端需要持续推送数据的场景。
     *
     * 使用场景：
     * - 实时监控数据推送
     * - 日志流式传输
     * - 大数据集的分批发送
     *
     * @param request Arthas 单元测试请求对象，指定要订阅的数据流
     * @param observer 用于向客户端发送多个响应的流观察者
     */
    void serverStream(ArthasUnittest.ArthasUnittestRequest request, StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer);

    /**
     * 双向流式调用 - 双向流式通信
     *
     * 该方法实现了双向流式 RPC 模式。客户端和服务器都可以通过
     * StreamObserver 同时发送和接收多个消息。这允许两个方向的
     * 独立通信，实现真正的双向实时交互。
     *
     * 使用场景：
     * - 实时聊天应用
     * - 交互式命令执行
     * - 需要双向通信的任意场景
     *
     * @param observer 用于向客户端发送多个响应的流观察者
     * @return StreamObserver 客户端通过此对象发送多个请求，同时可以接收响应
     */
    StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> biStream(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer);
}

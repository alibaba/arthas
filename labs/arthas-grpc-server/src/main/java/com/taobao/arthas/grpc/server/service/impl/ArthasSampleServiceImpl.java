package com.taobao.arthas.grpc.server.service.impl;

import arthas.grpc.unittest.ArthasUnittest;
import com.google.protobuf.InvalidProtocolBufferException;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import com.taobao.arthas.grpc.server.service.ArthasSampleService;
import com.taobao.arthas.grpc.server.utils.ByteUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Arthas示例服务实现类
 *
 * <p>这是一个用于演示和测试Arthas gRPC服务功能的实现类。
 * 该类实现了所有四种gRPC调用模式：</p>
 * <ul>
 *   <li>一元调用（Unary）：简单的请求-响应模式</li>
 *   <li>客户端流（Client Stream）：客户端发送多个请求，服务端返回一个响应</li>
 *   <li>服务端流（Server Stream）：客户端发送一个请求，服务端返回多个响应</li>
 *   <li>双向流（Bi-Directional Stream）：客户端和服务端可以互相发送多个消息</li>
 * </ul>
 *
 * <p>该实现包含了一些测试方法，用于演示gRPC的各种功能和用法。</p>
 *
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl - Arthas gRPC示例服务实现
 */
@GrpcService("arthas.grpc.unittest.ArthasUnittestService")
public class ArthasSampleServiceImpl implements ArthasSampleService {

    /**
     * 用于存储累加结果的并发Map
     *
     * <p>Key: 请求ID，用于标识不同的累加任务</p>
     * <p>Value: 累加结果，存储当前的总和</p>
     *
     * <p>该Map在unaryAddSum方法中使用，用于演示客户端流式调用时的状态保持。</p>
     */
    private ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();

    /**
     * 一元调用方法 - 简单的请求响应
     *
     * <p>这是最简单的gRPC调用模式：
     * <ol>
     *   <li>客户端发送一个请求</li>
     *   <li>服务端处理并返回一个响应</li>
     *   <li>调用结束</li>
     * </ol></p>
     *
     * <p>该方法直接将请求中的消息原样返回，用于演示基本的gRPC调用流程。</p>
     *
     * @param command gRPC请求对象，包含客户端发送的消息数据
     * @return gRPC响应对象，包含服务端返回的消息数据
     */
    @Override
    @GrpcMethod(value = "unary")
    public ArthasUnittest.ArthasUnittestResponse unary(ArthasUnittest.ArthasUnittestRequest command) {
        // 创建响应构建器
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();

        // 将请求中的消息设置到响应中（原样返回）
        builder.setMessage(command.getMessage());

        // 构建并返回响应对象
        return builder.build();
    }

    /**
     * 一元调用方法 - 累加数值
     *
     * <p>该方法演示了如何在服务端维护状态。
     * 客户端可以通过多次调用该方法，对同一个ID的数值进行累加。</p>
     *
     * <p>使用场景示例：</p>
     * <pre>
     * 第一次调用: id=1, num=10  → map中存入 1:10
     * 第二次调用: id=1, num=20  → map更新为 1:30
     * 第三次调用: id=1, num=15  → map更新为 1:45
     * </pre>
     *
     * @param command gRPC请求对象，包含ID和要累加的数值
     * @return gRPC响应对象，包含确认消息
     */
    @Override
    @GrpcMethod(value = "unaryAddSum")
    public ArthasUnittest.ArthasUnittestResponse unaryAddSum(ArthasUnittest.ArthasUnittestRequest command) {
        // 创建响应构建器
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();

        // 将请求中的消息设置到响应中
        builder.setMessage(command.getMessage());

        // 将请求中的数值累加到Map中对应ID的值上
        // merge方法：如果key不存在则放入新值，如果存在则对旧值和新值执行sum操作
        map.merge(command.getId(), command.getNum(), Integer::sum);

        // 构建并返回响应对象
        return builder.build();
    }

    /**
     * 一元调用方法 - 获取累加结果
     *
     * <p>该方法用于获取指定ID的累加结果。
     * 配合unaryAddSum方法使用，可以查看某个ID的当前累加值。</p>
     *
     * <p>如果指定ID不存在，则返回0作为默认值。</p>
     *
     * @param command gRPC请求对象，包含要查询的ID
     * @return gRPC响应对象，包含查询到的累加结果
     */
    @Override
    @GrpcMethod(value = "unaryGetSum")
    public ArthasUnittest.ArthasUnittestResponse unaryGetSum(ArthasUnittest.ArthasUnittestRequest command) {
        // 创建响应构建器
        ArthasUnittest.ArthasUnittestResponse.Builder builder = ArthasUnittest.ArthasUnittestResponse.newBuilder();

        // 将请求中的消息设置到响应中
        builder.setMessage(command.getMessage());

        // 从Map中获取指定ID的累加结果，如果不存在则返回0
        Integer sum = map.getOrDefault(command.getId(), 0);

        // 将累加结果设置到响应中
        builder.setNum(sum);

        // 构建并返回响应对象
        return builder.build();
    }

    /**
     * 客户端流方法 - 数值求和
     *
     * <p>客户端流调用模式：
     * <ol>
     *   <li>客户端发送多个请求消息（一个流）</li>
     *   <li>服务端接收所有请求后进行处理</li>
     *   <li>服务端返回一个响应</li>
     * </ol></p>
     *
     * <p>该方法实现了客户端流式调用的求和功能：</p>
     * <ul>
     *   <li>客户端可以在一个流中发送多个请求，每个请求包含一个数值</li>
     *   <li>服务端在onNext方法中接收每个请求并累加数值</li>
     *   <li>当客户端调用完成时，服务端在onCompleted方法中返回总和</li>
     * </ul>
     *
     * <p>注意：GrpcRequest中可能包含多个数据块（分片传输），因此需要循环读取所有数据块。</p>
     *
     * @param observer 响应观察者，用于在请求处理完成后发送响应
     * @return 请求观察者，用于接收客户端发送的流式请求
     */
    @Override
    @GrpcMethod(value = "clientStreamSum", grpcType = GrpcInvokeTypeEnum.CLIENT_STREAM)
    public StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> clientStreamSum(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer) {
        // 返回一个匿名实现的请求观察者
        return new StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>>() {
            /**
             * 原子整数，用于线程安全地累加数值
             *
             * <p>由于流式调用可能涉及多线程场景，使用AtomicInteger确保累加操作的原子性。</p>
             */
            AtomicInteger sum = new AtomicInteger(0);

            /**
             * 接收客户端发送的请求消息
             *
             * <p>每当客户端发送一个请求时，该方法会被调用。
             * 方法中会解析请求数据并累加数值。</p>
             *
             * @param req gRPC请求对象，包含一个或多个数据块
             */
            @Override
            public void onNext(GrpcRequest<ArthasUnittest.ArthasUnittestRequest> req) {
                try {
                    // 从请求中读取数据块
                    byte[] bytes = req.readData();

                    // 循环处理所有数据块（处理分片传输的情况）
                    while (bytes != null && bytes.length != 0) {
                        // 将字节数组解析为Protocol Buffer请求对象
                        ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.parseFrom(bytes);

                        // 累加数值到总和中
                        sum.addAndGet(request.getNum());

                        // 继续读取下一个数据块
                        bytes = req.readData();
                    }
                } catch (InvalidProtocolBufferException e) {
                    // Protocol Buffer解析失败，抛出运行时异常
                    throw new RuntimeException(e);
                }
            }

            /**
             * 客户端完成请求发送时的回调
             *
             * <p>当客户端完成所有请求的发送后，该方法会被调用。
             * 此时构建响应并返回给客户端。</p>
             */
            @Override
            public void onCompleted() {
                // 构建响应对象，设置累加的总和
                ArthasUnittest.ArthasUnittestResponse response = ArthasUnittest.ArthasUnittestResponse.newBuilder()
                        .setNum(sum.get())
                        .build();

                // 创建gRPC响应包装对象
                GrpcResponse<ArthasUnittest.ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();

                // 设置服务名
                grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");

                // 设置方法名
                grpcResponse.setMethod("clientStreamSum");

                // 将响应数据写入gRPC响应对象
                grpcResponse.writeResponseData(response);

                // 发送响应给客户端
                observer.onNext(grpcResponse);

                // 通知客户端响应已完成
                observer.onCompleted();
            }
        };
    }

    /**
     * 服务端流方法 - 发送多个响应
     *
     * <p>服务端流调用模式：
     * <ol>
     *   <li>客户端发送一个请求</li>
     *   <li>服务端处理后返回多个响应消息（一个流）</li>
     * </ol></p>
     *
     * <p>该方法演示了服务端流式调用：</p>
     * <ul>
     *   <li>客户端发送一个请求消息</li>
     *   <li>服务端在一个循环中多次调用observer.onNext发送响应</li>
     *   <li>最后调用observer.onCompleted表示流结束</li>
     * </ul>
     *
     * <p>本示例中，服务端会向客户端发送5条响应消息，每条消息包含序号和原始请求的消息内容。</p>
     *
     * @param request gRPC请求对象，包含客户端发送的请求数据
     * @param observer 响应观察者，用于向客户端发送流式响应
     */
    @Override
    @GrpcMethod(value = "serverStream", grpcType = GrpcInvokeTypeEnum.SERVER_STREAM)
    public void serverStream(ArthasUnittest.ArthasUnittestRequest request, StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer) {

        // 循环发送5条响应消息
        for (int i = 0; i < 5; i++) {
            // 构建响应对象，包含序号和原始请求消息
            ArthasUnittest.ArthasUnittestResponse response = ArthasUnittest.ArthasUnittestResponse.newBuilder()
                    .setMessage("Server response " + i + " to " + request.getMessage())
                    .build();

            // 创建gRPC响应包装对象
            GrpcResponse<ArthasUnittest.ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();

            // 设置服务名
            grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");

            // 设置方法名
            grpcResponse.setMethod("serverStream");

            // 将响应数据写入gRPC响应对象
            grpcResponse.writeResponseData(response);

            // 发送响应给客户端（可以多次调用）
            observer.onNext(grpcResponse);
        }

        // 通知客户端响应流已完成
        observer.onCompleted();
    }

    /**
     * 双向流方法 - 客户端和服务端可以互相发送多个消息
     *
     * <p>双向流调用模式：
     * <ol>
     *   <li>客户端和服务端建立连接后，双方都可以发送多个消息</li>
     *   <li>两个流独立运行，可以并发读写</li>
     *   <li>任意一方可以随时结束流</li>
     * </ol></p>
     *
     * <p>该方法实现了双向流式调用：</p>
     * <ul>
     *   <li>客户端可以发送多个请求消息</li>
     *   <li>服务端接收到每个请求后立即返回一个响应</li>
     *   <li>实现了类似"聊天"的交互模式</li>
     * </ul>
     *
     * <p>本示例中，服务端采用"回声"模式：
     * 接收到什么消息，就原样返回什么消息。</p>
     *
     * @param observer 响应观察者，用于向客户端发送流式响应
     * @return 请求观察者，用于接收客户端发送的流式请求
     */
    @Override
    @GrpcMethod(value = "biStream", grpcType = GrpcInvokeTypeEnum.BI_STREAM)
    public StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>> biStream(StreamObserver<GrpcResponse<ArthasUnittest.ArthasUnittestResponse>> observer) {
        // 返回一个匿名实现的请求观察者
        return new StreamObserver<GrpcRequest<ArthasUnittest.ArthasUnittestRequest>>() {
            /**
             * 接收客户端发送的请求消息并立即返回响应
             *
             * <p>在双向流模式中，每个请求都会立即得到响应，
             * 不需要等待所有请求发送完成。</p>
             *
             * @param req gRPC请求对象，包含一个或多个数据块
             */
            @Override
            public void onNext(GrpcRequest<ArthasUnittest.ArthasUnittestRequest> req) {
                try {
                    // 从请求中读取数据块
                    byte[] bytes = req.readData();

                    // 循环处理所有数据块（处理分片传输的情况）
                    while (bytes != null && bytes.length != 0) {
                        // 创建gRPC响应包装对象
                        GrpcResponse<ArthasUnittest.ArthasUnittestResponse> grpcResponse = new GrpcResponse<>();

                        // 设置服务名
                        grpcResponse.setService("arthas.grpc.unittest.ArthasUnittestService");

                        // 设置方法名
                        grpcResponse.setMethod("biStream");

                        // 解析请求并将其作为响应返回（回声模式）
                        grpcResponse.writeResponseData(ArthasUnittest.ArthasUnittestResponse.parseFrom(bytes));

                        // 立即发送响应给客户端
                        observer.onNext(grpcResponse);

                        // 继续读取下一个数据块
                        bytes = req.readData();
                    }
                } catch (InvalidProtocolBufferException e) {
                    // Protocol Buffer解析失败，抛出运行时异常
                    throw new RuntimeException(e);
                }
            }

            /**
             * 客户端完成请求发送时的回调
             *
             * <p>当客户端完成所有消息的发送后，服务端也结束响应流。</p>
             */
            @Override
            public void onCompleted() {
                // 通知客户端响应流已完成
                observer.onCompleted();
            }
        };
    }
}

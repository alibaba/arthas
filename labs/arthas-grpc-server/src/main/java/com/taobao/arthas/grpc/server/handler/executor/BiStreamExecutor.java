package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.StreamObserver;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 双向流gRPC执行器
 *
 * <p>该执行器负责处理gRPC的双向流调用模式（Bi-Directional Streaming）。</p>
 *
 * <p>双向流调用模式特点：</p>
 * <ul>
 *   <li>客户端和服务端可以同时发送和接收消息</li>
 *   <li>两个流独立运行，完全解耦</li>
 *   <li>客户端和服务端可以以任意顺序读写</li>
 *   <li>适用于需要双向实时通信的场景，如聊天应用、实时数据同步等</li>
 * </ul>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>客户端发起双向流调用，发送HTTP/2 HEADERS帧</li>
 *   <li>服务端创建双向流处理器，返回HTTP/2 HEADERS响应帧</li>
 *   <li>客户端可以发送多个DATA帧（请求消息）</li>
 *   <li>服务端接收每个请求后，可以立即返回响应</li>
 *   <li>双方可以独立地结束流</li>
 * </ol>
 *
 * @author: FengYe
 * @date: 2024/10/24 01:52
 * @description: BiStreamProcessor - 双向流gRPC请求处理器
 */
public class BiStreamExecutor extends AbstractGrpcExecutor {

    /**
     * 构造函数
     *
     * <p>创建双向流执行器实例，并初始化gRPC请求分发器。</p>
     *
     * @param dispatcher gRPC请求分发器，用于将双向流请求分发到具体的服务方法
     */
    public BiStreamExecutor(GrpcDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * 获取支持的gRPC调用类型
     *
     * <p>该方法返回该执行器支持的调用模式，即双向流模式。</p>
     *
     * @return 双向流调用类型枚举值（BI_STREAM）
     */
    @Override
    public GrpcInvokeTypeEnum supportGrpcType() {
        return GrpcInvokeTypeEnum.BI_STREAM;
    }

    /**
     * 执行双向流gRPC请求
     *
     * <p>该方法处理双向流调用的核心逻辑：</p>
     *
     * <p>1. 流ID管理：</p>
     * <ul>
     *   <li>每个HTTP/2流都有一个唯一的streamId</li>
     *   <li>使用streamId作为key，在Map中维护对应的StreamObserver</li>
     *   <li>同一流的所有请求都使用同一个StreamObserver处理</li>
     * </ul>
     *
     * <p>2. 响应观察者创建：</p>
     * <ul>
     *   <li>创建响应观察者，用于向客户端发送响应消息</li>
     *   <li>在onNext方法中实现响应帧的发送逻辑</li>
     *   <li>确保HTTP响应头只发送一次（使用AtomicBoolean控制）</li>
     *   <li>在onCompleted方法中发送流结束帧</li>
     * </ul>
     *
     * <p>3. 请求分发：</p>
     * <ul>
     *   <li>调用dispatcher.biStreamExecute方法分发请求到具体的服务方法</li>
     *   <li>返回请求观察者，用于接收客户端的流式请求</li>
     * </ul>
     *
     * <p>4. 流生命周期管理：</p>
     * <ul>
     *   <li>将请求转发给请求观察者处理</li>
     *   <li>检测流结束标志，调用onCompleted方法</li>
     * </ul>
     *
     * @param request gRPC请求对象，包含请求的元数据和流ID
     * @param frame HTTP/2数据帧，包含实际的请求数据和流结束标志
     * @param context Netty通道处理器上下文，用于发送响应帧
     * @throws Throwable 请求处理过程中可能抛出的异常
     */
    @Override
    public void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable {
        // 获取请求的流ID，用于标识和管理这个特定的双向流
        Integer streamId = request.getStreamId();

        // 使用computeIfAbsent方法，确保每个流ID只创建一次请求观察者
        // 如果该流ID已有对应的观察者，则直接使用；否则创建新的观察者
        StreamObserver<GrpcRequest> requestObserver = requestStreamObserverMap.computeIfAbsent(streamId, id->{
            // 创建响应观察者，用于向客户端发送响应消息
            StreamObserver<GrpcResponse> responseObserver = new StreamObserver<GrpcResponse>() {
                /**
                 * 原子布尔值，用于控制HTTP响应头只发送一次
                 *
                 * <p>在双向流中，服务端可能在onNext方法中发送多个响应消息。
                 * 根据HTTP/2协议和gRPC规范，响应头帧（HEADERS）只需要在第一个响应时发送一次，
                 * 后续的响应只需要发送数据帧（DATA）。</p>
                 *
                 * <p>使用AtomicBoolean确保在多线程环境下的线程安全性。</p>
                 */
                AtomicBoolean sendHeader = new AtomicBoolean(false);

                /**
                 * 发送响应消息
                 *
                 * <p>每当服务端需要向客户端发送响应时，该方法会被调用。</p>
                 *
                 * <p>处理流程：</p>
                 * <ol>
                 *   <li>检查是否已经发送过响应头</li>
                 *   <li>如果未发送，则先发送HTTP/2 HEADERS帧（包含gRPC响应头）</li>
                 *   <li>然后发送HTTP/2 DATA帧（包含响应数据）</li>
                 * </ol>
                 *
                 * @param res gRPC响应对象，包含响应数据和元数据
                 */
                @Override
                public void onNext(GrpcResponse res) {
                    // 控制流只能响应一次header
                    // 使用AtomicBoolean的CAS操作确保线程安全
                    if (!sendHeader.get()) {
                        // 将sendHeader从false设置为true
                        sendHeader.compareAndSet(false, true);

                        // 发送HTTP/2 HEADERS帧（响应头）
                        // res.getEndHeader()获取gRPC响应的HTTP头（包含状态码、内容类型等）
                        // frame.stream()指定该响应帧所属的HTTP/2流
                        context.writeAndFlush(new DefaultHttp2HeadersFrame(res.getEndHeader()).stream(frame.stream()));
                    }

                    // 发送HTTP/2 DATA帧（响应数据）
                    // res.getResponseData()获取响应的实际数据内容
                    context.writeAndFlush(new DefaultHttp2DataFrame(res.getResponseData()).stream(frame.stream()));
                }

                /**
                 * 完成响应流
                 *
                 * <p>当服务端不再发送响应消息时，该方法会被调用，
                 * 用于发送流结束标志给客户端。</p>
                 *
                 * <p>根据HTTP/2协议，流结束通过设置HEADERS帧的END_STREAM标志实现。</p>
                 */
                @Override
                public void onCompleted() {
                    // 发送HTTP/2 HEADERS帧，设置END_STREAM标志
                    // GrpcResponse.getDefaultEndStreamHeader()获取默认的流结束头
                    // 第二个参数true表示设置END_STREAM标志，表示流结束
                    context.writeAndFlush(new DefaultHttp2HeadersFrame(GrpcResponse.getDefaultEndStreamHeader(), true).stream(frame.stream()));
                }
            };

            try {
                // 通过分发器执行双向流调用
                // dispatcher会根据请求的服务名和方法名找到对应的实现
                // 并传入响应观察者，返回请求观察者
                return dispatcher.biStreamExecute(request, responseObserver);
            } catch (Throwable e) {
                // 如果分发过程中出现异常，包装为运行时异常抛出
                throw new RuntimeException(e);
            }
        });

        // 将当前请求转发给请求观察者处理
        // 这会触发服务实现类中的onNext方法
        requestObserver.onNext(request);

        // 检查HTTP/2帧的流结束标志
        // frame.isEndStream()返回true表示客户端已结束流
        if (frame.isEndStream()) {
            // 调用请求观察者的完成方法
            // 这会触发服务实现类中的onCompleted方法
            requestObserver.onCompleted();
        }
    }
}

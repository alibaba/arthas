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
 * 服务端流式调用执行器
 * <p>
 * 该类负责处理 gRPC 服务端流式调用（Server Streaming）类型的请求。
 * 在服务端流式调用中，服务端接收到一个请求后，可以返回多个响应消息，
 * 形成一个数据流，直到服务端主动结束流。
 * </p>
 * <p>
 * 典型应用场景：
 * <ul>
 *   <li>数据查询：分批返回大量数据</li>
 *   <li>事件推送：服务端主动推送多个事件</li>
 *   <li>日志订阅：持续推送日志数据</li>
 * </ul>
 * </p>
 * <p>
 * 流式调用的特点：
 * <ul>
 *   <li>客户端发送一个请求</li>
 *   <li>服务端可以发送零个或多个响应</li>
 *   <li>每个响应独立发送，不等待客户端确认</li>
 *   <li>服务端通过 onCompleted() 方法结束流</li>
 * </ul>
 * </p>
 *
 * @author FengYe
 * @date 2024/10/24 01:51
 */
public class ServerStreamExecutor extends AbstractGrpcExecutor {

    /**
     * 构造函数
     *
     * @param dispatcher gRPC 请求分发器，用于执行具体的业务逻辑
     */
    public ServerStreamExecutor(GrpcDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * 返回该执行器支持的 gRPC 调用类型
     *
     * @return 服务端流式调用类型
     */
    @Override
    public GrpcInvokeTypeEnum supportGrpcType() {
        return GrpcInvokeTypeEnum.SERVER_STREAM;
    }

    /**
     * 执行服务端流式调用
     * <p>
     * 该方法创建一个匿名 StreamObserver 实现类，用于处理流式响应的发送。
     * 业务逻辑可以通过 onNext() 方法多次发送响应数据，
     * 最后通过 onCompleted() 方法结束流。
     * </p>
     * <p>
     * 执行流程：
     * <ol>
     *   <li>创建响应观察者，定义如何发送响应数据和结束流</li>
     *   <li>调用分发器的 serverStreamExecute 方法执行业务逻辑</li>
     *   <li>业务逻辑在处理过程中通过 observer.onNext() 发送响应</li>
     *   <li>业务完成后通过 observer.onCompleted() 结束流</li>
     * </ol>
     * </p>
     *
     * @param request gRPC 请求对象，包含服务名、方法名和请求数据
     * @param frame   HTTP/2 数据帧，包含流信息
     * @param context 通道处理器上下文，用于写入响应数据
     * @throws Throwable 如果执行过程中发生异常
     */
    @Override
    public void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable {
        // 创建流式响应观察者，用于在业务逻辑中发送响应数据
        StreamObserver<GrpcResponse> responseObserver = new StreamObserver<GrpcResponse>() {
            // 使用原子布尔变量确保头部帧只发送一次
            // 在流式调用中，头部帧应该在第一个数据帧之前发送
            AtomicBoolean sendHeader = new AtomicBoolean(false);

            /**
             * 发送下一个响应消息
             * <p>
             * 业务逻辑每次要发送响应数据时调用此方法。
             * 首次调用时会先发送 HTTP/2 头部帧，后续调用只发送数据帧。
             * </p>
             *
             * @param res 要发送的响应对象，包含响应数据和元数据
             */
            @Override
            public void onNext(GrpcResponse res) {
                // 控制流只能响应一次 header，使用 CAS 操作确保线程安全
                if (!sendHeader.get()) {
                    // 原子地将标志位从 false 设置为 true
                    sendHeader.compareAndSet(false, true);
                    // 发送 HTTP/2 头部帧，包含响应的元数据信息（如状态码、内容类型等）
                    context.writeAndFlush(new DefaultHttp2HeadersFrame(res.getEndHeader()).stream(frame.stream()));
                }
                // 发送 HTTP/2 数据帧，包含响应的业务数据
                context.writeAndFlush(new DefaultHttp2DataFrame(res.getResponseData()).stream(frame.stream()));
            }

            /**
             * 完成流式响应
             * <p>
             * 业务逻辑处理完成后调用此方法，发送结束流帧。
             * 结束流帧带有 endStream 标志，告知客户端流已结束。
             * </p>
             */
            @Override
            public void onCompleted() {
                // 发送 HTTP/2 结束流帧，第二个参数 true 表示 endStream 标志
                // 这会关闭流的发送方向，通知客户端不再有数据发送
                context.writeAndFlush(new DefaultHttp2HeadersFrame(GrpcResponse.getDefaultEndStreamHeader(), true).stream(frame.stream()));
            }
        };

        try {
            // 调用分发器执行服务端流式调用的业务逻辑
            // 业务逻辑在处理过程中会通过 responseObserver.onNext() 发送响应数据
            // 处理完成后通过 responseObserver.onCompleted() 结束流
            dispatcher.serverStreamExecute(request, responseObserver);
        } catch (Throwable e) {
            // 如果执行过程中发生异常，包装成运行时异常抛出
            throw new RuntimeException(e);
        }
    }
}

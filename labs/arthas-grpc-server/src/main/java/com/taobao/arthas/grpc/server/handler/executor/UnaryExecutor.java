package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.GrpcResponse;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;

/**
 * 一元调用执行器
 * <p>
 * 该类负责处理 gRPC 一元调用（Unary Call）类型的请求。
 * 一元调用是最简单的 gRPC 调用模式，客户端发送一个请求，服务端返回一个响应。
 * </p>
 * <p>
 * 一元调用的特点：
 * <ul>
 *   <li>客户端发送一个请求</li>
 *   <li>服务端返回一个响应</li>
 *   <li>一对一的请求-响应模型</li>
 *   <li>类似于普通的 HTTP 请求</li>
 * </ul>
 * </p>
 * <p>
 * 典型应用场景：
 * <ul>
 *   <li>简单的 CRUD 操作</li>
 *   <li>单个数据的查询</li>
 *   <li>参数化的计算请求</li>
 * </ul>
 * </p>
 *
 * @author FengYe
 * @date 2024/10/24 01:51
 */
public class UnaryExecutor extends AbstractGrpcExecutor {

    /**
     * 构造函数
     *
     * @param dispatcher gRPC 请求分发器，用于执行具体的业务逻辑
     */
    public UnaryExecutor(GrpcDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * 返回该执行器支持的 gRPC 调用类型
     *
     * @return 一元调用类型（UNARY）
     */
    @Override
    public GrpcInvokeTypeEnum supportGrpcType() {
        return GrpcInvokeTypeEnum.UNARY;
    }

    /**
     * 执行一元调用
     * <p>
     * 一元调用需要等待所有请求数据接收完成（即收到 endStream 标志）后，
     * 才执行业务逻辑并返回响应。
     * </p>
     * <p>
     * 执行流程：
     * <ol>
     *   <li>检查数据帧的 endStream 标志，确认所有请求数据已接收完成</li>
     *   <li>调用分发器的 unaryExecute 方法执行业务逻辑，获取响应对象</li>
     *   <li>发送 HTTP/2 头部帧（包含响应元数据）</li>
     *   <li>发送 HTTP/2 数据帧（包含响应业务数据）</li>
     *   <li>发送 HTTP/2 结束流帧（标记响应结束）</li>
     * </ol>
     * </p>
     * <p>
     * HTTP/2 帧发送顺序：
     * <pre>
     * 1. HEADERS 帧（响应头，包含状态码、内容类型等元数据）
     * 2. DATA 帧（响应体，包含实际的业务数据）
     * 3. HEADERS 帧（endStream=true，表示流结束）
     * </pre>
     * </p>
     *
     * @param request gRPC 请求对象，包含服务名、方法名和请求数据
     * @param frame   HTTP/2 数据帧，包含请求数据和流信息
     * @param context 通道处理器上下文，用于写入响应数据
     * @throws Throwable 如果执行过程中发生异常
     */
    @Override
    public void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable {
        // 一元调用需要等到 endStream 标志为 true 时才响应
        // 这确保了所有请求数据都已接收完成
        if (frame.isEndStream()) {
            // 调用分发器执行一元调用的业务逻辑，获取响应对象
            GrpcResponse response = dispatcher.unaryExecute(request);

            // 发送 HTTP/2 头部帧，包含响应的元数据信息
            // 例如：HTTP 状态码（200 OK）、Content-Type（application/grpc+proto）等
            context.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndHeader()).stream(frame.stream()));

            // 发送 HTTP/2 数据帧，包含响应的业务数据
            // 这是实际返回给客户端的响应内容，已经序列化为字节数组
            context.writeAndFlush(new DefaultHttp2DataFrame(response.getResponseData()).stream(frame.stream()));

            // 发送 HTTP/2 结束流帧，第二个参数 true 表示 endStream 标志
            // 这会关闭流的发送方向，通知客户端响应已完整发送
            context.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndStreamHeader(), true).stream(frame.stream()));
        }
    }
}

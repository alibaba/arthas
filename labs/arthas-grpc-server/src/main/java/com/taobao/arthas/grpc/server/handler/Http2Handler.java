package com.taobao.arthas.grpc.server.handler;


import arthas.grpc.common.ArthasGrpc;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.grpc.server.handler.executor.GrpcExecutorFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http2.*;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP/2 帧处理器
 * <p>
 * 该类是 gRPC 服务端的核心处理器，负责处理 HTTP/2 协议的各种帧类型。
 * 继承自 Netty 的 SimpleChannelInboundHandler，专注于 Http2Frame 类型的消息处理。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>处理 HTTP/2 头部帧（HEADERS）：解析 gRPC 请求的元数据信息</li>
 *   <li>处理 HTTP/2 数据帧（DATA）：接收并处理 gRPC 请求的业务数据</li>
 *   <li>处理 HTTP/2 重置帧（RST_STREAM）：处理流的中断和重置</li>
 *   <li>管理请求的生命周期：使用数据缓冲区暂存请求对象</li>
 *   <li>异常处理：捕获并处理处理过程中的异常</li>
 * </ul>
 * </p>
 *
 * @author FengYe
 * @date 2024/7/7 下午9:58
 */
public class Http2Handler extends SimpleChannelInboundHandler<Http2Frame> {

    /**
     * 日志记录器
     * 使用 MethodHandles.lookup() 确保获取到正确的类名用于日志记录
     */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * gRPC 请求分发器
     * 负责将 gRPC 请求分发到对应的处理器进行业务逻辑处理
     */
    private GrpcDispatcher grpcDispatcher;

    /**
     * gRPC 执行器工厂
     * 根据不同的 gRPC 调用类型（一元调用、服务端流等）创建对应的执行器
     */
    private GrpcExecutorFactory grpcExecutorFactory;

    /**
     * 事件执行器组
     * 用于异步处理 gRPC 请求，避免阻塞 I/O 线程
     * 基于 NIO 的事件循环组，提供高效的并发处理能力
     */
    private final EventExecutorGroup executorGroup = new NioEventLoopGroup();

    /**
     * 请求数据缓冲区
     * 使用并发哈希表暂存所有正在处理的请求对象
     * Key: HTTP/2 流的 ID（streamId）
     * Value: 对应的 gRPC 请求对象
     * 用于在接收多个数据帧时，将数据关联到同一个请求对象上
     */
    private ConcurrentHashMap<Integer, GrpcRequest> dataBuffer = new ConcurrentHashMap<>();

    /**
     * HTTP/2 头部中的路径字段名
     * 用于从 HTTP/2 头部帧中提取请求路径，路径格式通常为：/服务名/方法名
     */
    private static final String HEADER_PATH = ":path";

    /**
     * 构造函数
     *
     * @param grpcDispatcher     gRPC 请求分发器，负责将请求分发到对应的业务处理器
     * @param grpcExecutorFactory gRPC 执行器工厂，负责创建不同类型的执行器
     */
    public Http2Handler(GrpcDispatcher grpcDispatcher, GrpcExecutorFactory grpcExecutorFactory) {
        this.grpcDispatcher = grpcDispatcher;
        this.grpcExecutorFactory = grpcExecutorFactory;
    }

    /**
     * 通道读取方法
     * 当通道中有数据可读时调用，该方法将消息传递给父类处理
     *
     * @param ctx 通道处理器上下文，用于与管道中的其他处理器交互
     * @param msg 读取到的消息对象
     * @throws Exception 如果处理过程中发生异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    /**
     * HTTP/2 帧处理方法
     * 这是核心处理方法，根据接收到的帧类型进行分发处理
     *
     * @param ctx    通道处理器上下文，用于写入响应数据
     * @param frame  HTTP/2 帧对象，可能是头部帧、数据帧或重置帧
     * @throws IOException 如果处理数据时发生 I/O 异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) throws IOException {
        // 判断帧类型并进行相应处理
        if (frame instanceof Http2HeadersFrame) {
            // 处理 HTTP/2 头部帧：解析请求的元数据信息（服务名、方法名等）
            handleGrpcRequest((Http2HeadersFrame) frame, ctx);
        } else if (frame instanceof Http2DataFrame) {
            // 处理 HTTP/2 数据帧：接收并处理请求的业务数据
            handleGrpcData((Http2DataFrame) frame, ctx);
        } else if (frame instanceof Http2ResetFrame) {
            // 处理 HTTP/2 重置帧：客户端中断流，清理相关资源
            handleResetStream((Http2ResetFrame) frame, ctx);
        }
    }

    /**
     * 异常捕获方法
     * 当处理过程中发生异常时调用，打印异常堆栈并关闭连接
     *
     * @param ctx   通道处理器上下文
     * @param cause 发生的异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 打印异常堆栈信息，便于问题排查
        cause.printStackTrace();
        // 关闭当前通道，释放资源
        ctx.close();
    }

    /**
     * 处理 gRPC 请求的头部帧
     * <p>
     * 从 HTTP/2 头部帧中提取请求路径信息，解析出服务名和方法名，
     * 创建 GrpcRequest 对象并存入缓冲区，等待后续数据帧到达。
     * </p>
     *
     * @param headersFrame HTTP/2 头部帧，包含请求的元数据信息
     * @param ctx          通道处理器上下文
     */
    private void handleGrpcRequest(Http2HeadersFrame headersFrame, ChannelHandlerContext ctx) {
        // 获取流的唯一标识符
        int id = headersFrame.stream().id();

        // 从头部中提取请求路径，格式如：/com.example.Service/Method
        String path = headersFrame.headers().get(HEADER_PATH).toString();

        // 去掉路径前面的斜杠，然后按斜杠分割
        // 例如："/ServiceA/MethodB" -> ["ServiceA", "MethodB"]
        String[] parts = path.substring(1).split("/");

        // 创建 gRPC 请求对象，包含流ID、服务名和方法名
        GrpcRequest grpcRequest = new GrpcRequest(headersFrame.stream().id(), parts[0], parts[1]);

        // 设置请求的头部信息，便于后续处理时访问
        grpcRequest.setHeaders(headersFrame.headers());

        // 检查并设置 gRPC 调用类型（一元调用、服务端流等）
        GrpcDispatcher.checkGrpcType(grpcRequest);

        // 将请求对象存入缓冲区，等待后续数据帧关联
        dataBuffer.put(id, grpcRequest);

        // 输出接收到的头部信息，便于调试
        System.out.println("Received headers: " + headersFrame.headers());
    }

    /**
     * 处理 gRPC 请求的数据帧
     * <p>
     * 接收 HTTP/2 数据帧，将数据写入对应的 GrpcRequest 对象，
     * 然后异步执行对应的 gRPC 调用处理器。
     * </p>
     *
     * @param dataFrame HTTP/2 数据帧，包含请求的业务数据
     * @param ctx       通道处理器上下文，用于写入响应
     * @throws IOException 如果处理数据时发生 I/O 异常
     */
    private void handleGrpcData(Http2DataFrame dataFrame, ChannelHandlerContext ctx) throws IOException {
        // 获取数据帧所属流的 ID
        int streamId = dataFrame.stream().id();

        // 从缓冲区中获取之前创建的请求对象
        GrpcRequest grpcRequest = dataBuffer.get(streamId);

        // 获取数据帧的内容缓冲区
        ByteBuf content = dataFrame.content();

        // 将数据写入请求对象，请求对象会累加所有接收到的数据
        grpcRequest.writeData(content);

        // 使用异步线程池处理 gRPC 请求，避免阻塞 I/O 线程
        executorGroup.execute(() -> {
            try {
                // 根据请求类型获取对应的执行器，并执行业务逻辑
                grpcExecutorFactory.getExecutor(grpcRequest.getGrpcType()).execute(grpcRequest, dataFrame, ctx);
            } catch (Throwable e) {
                // 记录处理错误日志
                logger.error("handleGrpcData error", e);
                // 处理异常，向客户端返回错误响应
                processError(ctx, e, dataFrame.stream());
            }
        });
    }

    /**
     * 处理流重置请求
     * <p>
     * 当客户端发送 RST_STREAM 帧时调用，表示客户端想要取消或中止当前流。
     * 此时需要从缓冲区中移除对应的请求对象，释放资源。
     * </p>
     *
     * @param resetFrame HTTP/2 重置帧，包含要重置的流信息
     * @param ctx        通道处理器上下文
     */
    private void handleResetStream(Http2ResetFrame resetFrame, ChannelHandlerContext ctx) {
        // 获取要重置的流 ID
        int id = resetFrame.stream().id();

        // 输出调试信息
        System.out.println("handleResetStream");

        // 从缓冲区中移除对应的请求对象，释放内存
        dataBuffer.remove(id);
    }

    /**
     * 处理异常情况，向客户端返回错误响应
     * <p>
     * 当 gRPC 调用过程中发生异常时，构建错误响应并发送给客户端。
     * 按照 HTTP/2 协议规范，依次发送：头部帧、数据帧、结束流帧。
     * </p>
     *
     * @param ctx    通道处理器上下文，用于写入响应
     * @param e      发生的异常对象
     * @param stream 发生错误的 HTTP/2 流
     */
    private void processError(ChannelHandlerContext ctx, Throwable e, Http2FrameStream stream) {
        // 创建响应对象
        GrpcResponse response = new GrpcResponse();

        // 构建 gRPC 错误响应消息
        ArthasGrpc.ErrorRes.Builder builder = ArthasGrpc.ErrorRes.newBuilder();
        ArthasGrpc.ErrorRes errorRes = builder.setErrorMsg(Optional.ofNullable(e.getMessage()).orElse("")).build();

        // 设置响应的类型为错误响应
        response.setClazz(ArthasGrpc.ErrorRes.class);

        // 将错误消息序列化到响应对象中
        response.writeResponseData(errorRes);

        // 发送响应头部帧，包含响应的元数据信息
        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndHeader()).stream(stream));

        // 发送响应数据帧，包含错误消息的序列化数据
        ctx.writeAndFlush(new DefaultHttp2DataFrame(response.getResponseData()).stream(stream));

        // 发送结束流帧，标记响应结束（endStream 标志为 true）
        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(response.getEndStreamHeader(), true).stream(stream));
    }
}
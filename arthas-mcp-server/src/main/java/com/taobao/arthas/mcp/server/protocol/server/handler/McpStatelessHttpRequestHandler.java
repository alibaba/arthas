/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpStatelessServerHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.util.Assert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 无状态MCP协议的HTTP请求处理器。
 *
 * 该处理器实现了MCP（Model Context Protocol）的无状态传输模式。
 * 无状态模式的特点：
 * - 不维护客户端会话状态
 * - 每个请求独立处理，请求之间没有关联
 * - 适合高并发、可水平扩展的场景
 * - 使用标准的HTTP POST请求发送JSON-RPC消息
 *
 * 处理流程：
 * 1. 验证请求URI是否匹配MCP端点
 * 2. 检查服务器是否正在关闭
 * 3. 验证HTTP方法（仅支持POST）
 * 4. 从请求中提取认证信息
 * 5. 反序列化JSON-RPC消息
 * 6. 根据消息类型（请求或通知）委托给相应的处理器
 * 7. 序列化响应并返回给客户端
 *
 */
public class McpStatelessHttpRequestHandler {

    /**
     * 日志记录器，用于记录处理器的运行状态和错误信息
     */
    private static final Logger logger = LoggerFactory.getLogger(McpStatelessHttpRequestHandler.class);

    /**
     * UTF-8字符编码常量
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * JSON媒体类型常量，用于Content-Type和Accept头
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * 服务器发送事件（SSE）媒体类型常量
     */
    public static final String TEXT_EVENT_STREAM = "text/event-stream";

    /**
     * HTTP Accept请求头名称常量
     */
    public static final String ACCEPT = "Accept";

    /**
     * 发送错误响应失败的日志消息模板
     */
    private static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";

    /**
     * JSON对象映射器，用于序列化和反序列化MCP协议消息
     */
    private final ObjectMapper objectMapper;

    /**
     * MCP端点路径，例如 "/mcp"
     */
    private final String mcpEndpoint;

    /**
     * 无状态MCP服务器处理器，负责处理业务逻辑
     */
    private McpStatelessServerHandler mcpHandler;

    /**
     * 传输上下文提取器，从HTTP请求中提取MCP传输所需的上下文信息
     */
    private final McpTransportContextExtractor<FullHttpRequest> contextExtractor;

    /**
     * 原子布尔标志，指示服务器是否正在关闭过程中
     * 使用AtomicBoolean确保线程安全
     */
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    /**
     * 构造新的McpStatelessHttpRequestHandler实例。
     *
     * @param objectMapper JSON对象映射器，用于JSON序列化/反序列化
     * @param mcpEndpoint 客户端发送JSON-RPC消息的端点URI路径
     * @param contextExtractor 从请求中提取传输上下文的提取器
     * @throws IllegalArgumentException 如果任何参数为null
     */
    public McpStatelessHttpRequestHandler(ObjectMapper objectMapper, String mcpEndpoint,
                                         McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
        Assert.notNull(objectMapper, "objectMapper must not be null");
        Assert.notNull(mcpEndpoint, "mcpEndpoint must not be null");
        Assert.notNull(contextExtractor, "contextExtractor must not be null");

        this.objectMapper = objectMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.contextExtractor = contextExtractor;
    }

    /**
     * 设置MCP服务器处理器。
     *
     * 该处理器负责实际处理MCP协议请求的业务逻辑。
     *
     * @param mcpHandler 无状态MCP服务器处理器实例
     */
    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        this.mcpHandler = mcpHandler;
    }

    /**
     * 启动处理器的优雅关闭流程。
     *
     * 优雅关闭会：
     * 1. 设置关闭标志，阻止新请求被处理
     * 2. 立即返回，不等待正在处理的请求完成
     *
     * 注意：在无状态模式下，由于不维护连接状态，关闭操作相对简单。
     *
     * @return CompletableFuture，在关闭启动时完成
     */
    public CompletableFuture<Void> closeGracefully() {
        return CompletableFuture.supplyAsync(() -> {
            // 设置关闭标志，后续请求将被拒绝
            this.isClosing.set(true);
            return null;
        });
    }

    /**
     * 处理传入的HTTP请求。
     *
     * 该方法执行以下操作：
     * 1. 验证请求URI是否匹配配置的MCP端点
     * 2. 检查服务器是否正在关闭
     * 3. 验证HTTP方法（仅支持POST，不支持GET）
     * 4. 将POST请求委托给handlePostRequest方法处理
     *
     * @param ctx Netty通道处理上下文，用于写入响应
     * @param request 完整的HTTP请求对象
     * @throws Exception 如果处理过程中发生严重错误
     */
    protected void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 获取请求URI并验证是否匹配MCP端点
        String uri = request.uri();
        if (!uri.endsWith(mcpEndpoint)) {
            // 端点不匹配，返回404错误
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Endpoint not found"));
            return;
        }

        // 检查服务器是否正在关闭过程中
        if (isClosing.get()) {
            // 服务器正在关闭，返回503服务不可用错误
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, new McpError("Server is shutting down"));
            return;
        }

        // 获取HTTP方法并验证
        HttpMethod method = request.method();
        if (method == HttpMethod.GET) {
            // 无状态传输不支持GET方法，返回405方法不允许错误
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("GET method not allowed for stateless transport"));
        } else if (method == HttpMethod.POST) {
            // POST方法，继续处理请求
            handlePostRequest(ctx, request);
        } else {
            // 其他HTTP方法不被支持，返回405错误
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("Only POST method is supported"));
        }
    }

    /**
     * 处理客户端的POST请求，包含传入的JSON-RPC消息。
     *
     * 该方法执行以下操作：
     * 1. 从请求中提取传输上下文和认证信息
     * 2. 验证Accept头是否包含必需的媒体类型
     * 3. 反序列化JSON-RPC消息体
     * 4. 根据消息类型（请求或通知）路由到相应的处理逻辑
     * 5. 序列化响应并发送回客户端
     *
     * @param ctx Netty通道处理上下文，用于写入响应
     * @param request HTTP POST请求对象
     */
    private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 使用上下文提取器从请求中提取传输上下文
        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        // 从Netty上下文中提取认证主体信息
        Object authSubject = McpAuthExtractor.extractAuthSubjectFromContext(ctx);
        // 将认证主体存入传输上下文
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);

        // 从 HTTP header 中提取 User ID
        String userId = McpAuthExtractor.extractUserIdFromRequest(request);
        // 将用户ID存入传输上下文
        transportContext.put(McpAuthExtractor.MCP_USER_ID_KEY, userId);

        // 获取并验证Accept头
        String accept = request.headers().get(ACCEPT);
        // Accept头必须同时包含application/json和text/event-stream
        if (accept == null || !(accept.contains(APPLICATION_JSON) && accept.contains(TEXT_EVENT_STREAM))) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                    new McpError("Both application/json and text/event-stream required in Accept header"));
            return;
        }

        try {
            // 从请求体中读取字节内容
            ByteBuf content = request.content();
            // 将字节内容转换为UTF-8字符串
            String body = content.toString(CharsetUtil.UTF_8);

            // 反序列化JSON-RPC消息
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            // 根据消息类型进行分支处理
            if (message instanceof McpSchema.JSONRPCRequest) {
                // 处理JSON-RPC请求（需要响应）
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                try {
                    // 委托给MCP处理器处理请求
                    this.mcpHandler.handleRequest(transportContext, jsonrpcRequest)
                            .thenAccept(jsonrpcResponse -> {
                                try {
                                    // 创建HTTP响应，状态码200 OK
                                    FullHttpResponse response = new DefaultFullHttpResponse(
                                            HttpVersion.HTTP_1_1,
                                            HttpResponseStatus.OK,
                                            Unpooled.copiedBuffer(objectMapper.writeValueAsString(jsonrpcResponse), CharsetUtil.UTF_8)
                                    );

                                    // 设置响应头
                                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON);
                                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

                                    // 发送响应并关闭连接
                                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                } catch (Exception e) {
                                    // 响应序列化失败
                                    logger.error("Failed to serialize response: {}", e.getMessage());
                                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                            new McpError("Failed to serialize response: " + e.getMessage()));
                                }
                            })
                            .exceptionally(e -> {
                                // 请求处理失败
                                logger.error("Failed to handle request: {}", e.getMessage());
                                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                        new McpError("Failed to handle request: " + e.getMessage()));
                                return null;
                            });
                } catch (Exception e) {
                    // 请求处理异常
                    logger.error("Failed to handle request: {}", e.getMessage());
                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            new McpError("Failed to handle request: " + e.getMessage()));
                }
            } else if (message instanceof McpSchema.JSONRPCNotification) {
                // 处理JSON-RPC通知（不需要响应）
                McpSchema.JSONRPCNotification jsonrpcNotification = (McpSchema.JSONRPCNotification) message;
                try {
                    // 委托给MCP处理器处理通知
                    this.mcpHandler.handleNotification(transportContext, jsonrpcNotification)
                            .thenRun(() -> {
                                // 创建HTTP响应，状态码202 Accepted
                                FullHttpResponse response = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.ACCEPTED
                                );
                                // 设置响应头，通知已接受但无内容返回
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                                // 发送响应并关闭连接
                                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                            })
                            .exceptionally(e -> {
                                // 通知处理失败
                                logger.error("Failed to handle notification: {}", e.getMessage());
                                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                        new McpError("Failed to handle notification: " + e.getMessage()));
                                return null;
                            });
                } catch (Exception e) {
                    // 通知处理异常
                    logger.error("Failed to handle notification: {}", e.getMessage());
                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            new McpError("Failed to handle notification: " + e.getMessage()));
                }
            } else {
                // 消息类型既不是请求也不是通知，返回错误
                sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                        new McpError("The server accepts either requests or notifications"));
            }
        } catch (IllegalArgumentException | IOException e) {
            // 消息反序列化失败
            logger.error("Failed to deserialize message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError("Invalid message format"));
        } catch (Exception e) {
            // 未预期的错误
            logger.error("Unexpected error handling message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    new McpError("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * 向客户端发送错误响应。
     *
     * 该方法将MCPError对象序列化为JSON格式，并通过HTTP响应返回给客户端。
     * 如果序列化失败，则返回一个简单的500内部服务器错误响应。
     *
     * 响应处理流程：
     * 1. 将McpError对象序列化为JSON字符串
     * 2. 创建HTTP响应对象，包含错误状态码和JSON错误体
     * 3. 设置必要的响应头（Content-Type、Content-Length、CORS）
     * 4. 发送响应并关闭连接
     *
     * @param ctx Netty通道处理上下文，用于写入响应
     * @param status HTTP响应状态码（如400、404、500等）
     * @param mcpError MCP错误对象，包含错误详情
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, McpError mcpError) {
        try {
            // 将MCP错误对象序列化为JSON字符串
            String jsonError = objectMapper.writeValueAsString(mcpError);
            // 将JSON字符串转换为字节缓冲区
            ByteBuf content = Unpooled.copiedBuffer(jsonError, CharsetUtil.UTF_8);

            // 创建完整的HTTP响应对象
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status,
                    content
            );

            // 设置响应头
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

            // 发送响应并关闭连接
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            // 序列化失败，记录错误并发送简单的500错误响应
            logger.error(FAILED_TO_SEND_ERROR_RESPONSE, e.getMessage());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR
            );
            // 设置空内容的响应头
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            // 发送响应并关闭连接
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

}

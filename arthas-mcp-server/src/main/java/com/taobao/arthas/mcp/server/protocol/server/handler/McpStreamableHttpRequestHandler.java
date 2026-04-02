/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.spec.HttpHeaders;
import com.taobao.arthas.mcp.server.protocol.spec.*;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.KeepAliveScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;

/**
 * 模型上下文协议（MCP）可流式传输层的服务器端实现，使用 HTTP 和服务器发送事件（SSE）通过 Netty 实现。
 * 该实现提供了 Netty 操作和 MCP 传输接口之间的桥梁。
 *
 * @see McpStreamableServerTransportProvider
 */
public class McpStreamableHttpRequestHandler {

    // 日志记录器，用于记录该类的运行时信息和错误
    private static final Logger logger = LoggerFactory.getLogger(McpStreamableHttpRequestHandler.class);

    /**
     * 通过 SSE 连接发送的 JSON-RPC 消息的事件类型。
     * SSE 协议中每个消息都有一个关联的事件类型标识符。
     */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /**
     * 响应中请求者接受的媒体类型的 HTTP 头名称。
     * 用于检查客户端是否接受特定的响应格式（如 SSE 或 JSON）。
     */
    private static final String ACCEPT = "Accept";

    // UTF-8 字符编码常量
    public static final String UTF_8 = "UTF-8";

    // JSON 媒体类型常量，用于 JSON-RPC 消息的 Content-Type
    public static final String APPLICATION_JSON = "application/json";

    // 服务器发送事件（SSE）的媒体类型常量，用于流式响应的 Content-Type
    public static final String TEXT_EVENT_STREAM = "text/event-stream";

    // 发送错误响应失败时的日志消息模板
    private static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";

    /**
     * 客户端应发送其 JSON-RPC 消息的端点 URI。
     * 默认值为 "/mcp"，这是 MCP 协议的标准端点路径。
     */
    private final String mcpEndpoint;

    /**
     * 标志位，指示是否禁止在端点上使用 DELETE 请求。
     * 当设置为 true 时，服务器将拒绝 DELETE 方法的请求。
     */
    private final boolean disallowDelete;

    // Jackson ObjectMapper，用于 JSON 消息的序列化和反序列化
    private final ObjectMapper objectMapper;

    // MCP 可流式服务器会话工厂，用于创建新的客户端会话
    private McpStreamableServerSession.Factory sessionFactory;

    /**
     * 活动客户端会话的映射表，以 mcp-session-id 为键。
     * 该集合存储所有当前活动的会话实例，支持并发访问。
     */
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();

    // MCP 传输上下文提取器，从 HTTP 请求中提取传输上下文信息
    private McpTransportContextExtractor<FullHttpRequest> contextExtractor;

    /**
     * 标志位，指示传输层是否正在关闭。
     * 当设置为 true 时，服务器将拒绝新的连接请求并开始优雅关闭现有会话。
     */
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    /**
     * 保活调度器，用于管理会话的心跳 ping。
     * 如果设置了 keepAliveInterval 则激活，默认禁用。
     * 该调度器定期向所有活动会话发送 ping 消息以保持连接活跃。
     */
    private KeepAliveScheduler keepAliveScheduler;

    /**
     * 构造一个新的 McpStreamableHttpRequestHandler 实例。
     *
     * @param objectMapper 用于 JSON 消息序列化/反序列化的 ObjectMapper
     * @param mcpEndpoint 客户端通过 HTTP 发送 JSON-RPC 消息的端点 URI
     * @param disallowDelete 是否禁止在端点上使用 DELETE 请求
     * @param contextExtractor 从请求中提取传输上下文的提取器
     * @param keepAliveInterval 保活 ping 的间隔（null 表示禁用）
     * @throws IllegalArgumentException 如果任何参数为 null
     */
    public McpStreamableHttpRequestHandler(ObjectMapper objectMapper, String mcpEndpoint,
                                           boolean disallowDelete, McpTransportContextExtractor<FullHttpRequest> contextExtractor,
                                           Duration keepAliveInterval) {
        // 保存传入的配置参数
        this.objectMapper = objectMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.disallowDelete = disallowDelete;
        this.contextExtractor = contextExtractor;

        // 如果指定了保活间隔，则创建并启动保活调度器
        if (keepAliveInterval != null) {
            // 创建保活调度器构建器，提供会话供应函数
            this.keepAliveScheduler = KeepAliveScheduler
                    .builder(() -> this.isClosing.get() ? Collections.emptyList() : this.sessions.values())
                    .initialDelay(keepAliveInterval)  // 设置初始延迟
                    .interval(keepAliveInterval)      // 设置执行间隔
                    .build();

            // 启动保活调度器
            this.keepAliveScheduler.start();
            logger.debug("Keep-alive scheduler started with interval: {}ms", keepAliveInterval.toMillis());
        }
    }

    /**
     * 设置 MCP 可流式服务器会话工厂。
     * 该工厂用于在客户端初始化时创建新的会话实例。
     *
     * @param sessionFactory 会话工厂实例
     */
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * 向所有连接的客户端广播通知消息。
     * 该方法会并行地向所有活动会话发送通知，不会因为单个会话失败而影响其他会话。
     *
     * @param method 通知的方法名称
     * @param params 通知的参数对象
     * @return CompletableFuture，当广播尝试完成时完成
     */
    public CompletableFuture<Void> notifyClients(String method, Object params) {
        // 如果没有活动会话，直接返回已完成的 Future
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return CompletableFuture.completedFuture(null);
        }

        // 记录广播消息的调试信息
        logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());

        // 异步执行广播操作，使用并行流提高效率
        return CompletableFuture.runAsync(() -> {
            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    // 向单个会话发送通知
                    session.sendNotification(method, params);
                } catch (Exception e) {
                    // 记录发送失败错误，但继续处理其他会话
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            });
        });
    }

    /**
     * 启动优雅关闭流程。
     * 该方法会关闭所有活动会话并清理资源，不会中断正在处理的请求。
     *
     * @return CompletableFuture，当所有清理操作完成时完成
     */
    public CompletableFuture<Void> closeGracefully() {
        return CompletableFuture.runAsync(() -> {
            // 设置关闭标志，阻止新会话的创建
            this.isClosing.set(true);
            logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());

            // 优雅地关闭所有活动会话，使用并行流提高效率
            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    // 调用会话的优雅关闭方法
                    session.closeGracefully();
                } catch (Exception e) {
                    // 记录关闭失败错误，但继续关闭其他会话
                    logger.error("Failed to close session {}: {}", session.getId(), e.getMessage());
                }
            });

            // 清空会话映射表
            this.sessions.clear();
            logger.debug("Graceful shutdown completed");

            // 如果保活调度器正在运行，则关闭它
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    /**
     * 处理传入的 HTTP 请求。
     * 该方法根据请求方法和 URI 路由到相应的处理方法。
     *
     * @param ctx Netty 通道处理器上下文，用于写入响应
     * @param request 完整的 HTTP 请求对象
     * @throws Exception 如果处理过程中发生错误
     */
    protected void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 获取请求的 URI
        String uri = request.uri();
        // 检查 URI 是否以配置的 MCP 端点结尾
        if (!uri.endsWith(mcpEndpoint)) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Endpoint not found"));
            return;
        }

        // 检查服务器是否正在关闭
        if (isClosing.get()) {
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, new McpError("Server is shutting down"));
            return;
        }

        // 获取 HTTP 请求方法
        HttpMethod method = request.method();
        // 根据请求方法路由到相应的处理方法
        if (method == HttpMethod.GET) {
            // GET 请求用于建立 SSE 连接
            handleGetRequest(ctx, request);
        } else if (method == HttpMethod.POST) {
            // POST 请求用于发送 JSON-RPC 消息
            handlePostRequest(ctx, request);
        } else if (method == HttpMethod.DELETE) {
            // DELETE 请求用于删除会话
            handleDeleteRequest(ctx, request);
        } else {
            // 不支持的 HTTP 方法
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("Method not allowed"));
        }
    }

    /**
     * 处理 GET 请求以建立 SSE 连接和消息回放。
     * SSE（Server-Sent Events）连接用于服务器向客户端推送消息。
     *
     * @param ctx Netty 通道处理器上下文
     * @param request HTTP GET 请求
     */
    private void handleGetRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // TODO 支持 last-event-id #3118
        // MCP 客户端在 SSE 断线重连时，可能会带上 last-event-id 尝试做消息回放。
        // Arthas MCP Server 不支持基于 last-event-id 的恢复逻辑：直接返回 404，
        // 让客户端触发完整重置并重新走 Initialize 握手申请新的会话。
        if (request.headers().get(HttpHeaders.LAST_EVENT_ID) != null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND,
                    new McpError("Session not found, please re-initialize"));
            return;
        }

        // 收集请求验证错误
        List<String> badRequestErrors = new ArrayList<>();

        // 验证 Accept 头是否包含 text/event-stream
        String accept = request.headers().get(ACCEPT);
        if (accept == null || !accept.contains(TEXT_EVENT_STREAM)) {
            badRequestErrors.add("text/event-stream required in Accept header");
        }

        // 验证是否提供了会话 ID
        String sessionId = request.headers().get(HttpHeaders.MCP_SESSION_ID);
        if (sessionId == null || sessionId.trim().isEmpty()) {
            badRequestErrors.add("Session ID required in mcp-session-id header");
        }

        // 如果有验证错误，返回 400 错误
        if (!badRequestErrors.isEmpty()) {
            String combinedMessage = String.join("; ", badRequestErrors);
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError(combinedMessage));
            return;
        }

        // 查找会话
        McpStreamableServerSession session = this.sessions.get(sessionId);
        if (session == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Session not found"));
            return;
        }

        logger.debug("Handling GET request for session: {}", sessionId);

        // 从请求中提取传输上下文
        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        // 从 Netty 上下文中提取认证主体
        Object authSubject = McpAuthExtractor.extractAuthSubjectFromContext(ctx);
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);

        // 从 HTTP header 中提取 User ID
        String userId = McpAuthExtractor.extractUserIdFromRequest(request);
        transportContext.put(McpAuthExtractor.MCP_USER_ID_KEY, userId);

        try {
            // 设置 SSE 响应头
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_EVENT_STREAM);  // 设置内容类型为 SSE
            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");          // 禁用缓存
            response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");           // 保持连接
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");   // 允许跨域
            response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);  // 分块传输

            // 写入响应头
            ctx.writeAndFlush(response);

            // 创建 Netty 会话传输对象
            NettyStreamableMcpSessionTransport sessionTransport = new NettyStreamableMcpSessionTransport(
                    sessionId, ctx);

            // 检查是否为消息回放请求（虽然前面已经返回 404，这里保留逻辑）
            String lastEventId = request.headers().get(HttpHeaders.LAST_EVENT_ID);
            if (lastEventId != null) {
                try {
                    // 从最后事件 ID 开始回放消息
                    try {
                        session.replay(lastEventId).forEach(message -> {
                            try {
                                // 发送回放消息
                                sessionTransport.sendMessage(message).join();
                            } catch (Exception e) {
                                logger.error("Failed to replay message: {}", e.getMessage());
                                ctx.close();
                            }
                        });
                    } catch (Exception e) {
                        logger.error("Failed to replay messages: {}", e.getMessage());
                        ctx.close();
                    }
                } catch (Exception e) {
                    logger.error("Failed to replay messages: {}", e.getMessage());
                    ctx.close();
                }
            } else {
                // 建立新的监听流
                McpStreamableServerSession.McpStreamableServerSessionStream listeningStream = session
                        .listeningStream(sessionTransport);

                // 处理通道关闭事件
                ctx.channel().closeFuture().addListener(future -> {
                    logger.debug("SSE connection closed for session: {}", sessionId);
                    // 关闭监听流
                    listeningStream.close();
                });
            }
        } catch (Exception e) {
            logger.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, new McpError("Internal server error"));
        }
    }

    /**
     * 处理来自客户端的 JSON-RPC 消息的 POST 请求。
     * POST 请求用于发送初始化请求、通知、响应和新的请求。
     *
     * @param ctx Netty 通道处理器上下文
     * @param request HTTP POST 请求
     */
    private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 收集请求验证错误
        List<String> badRequestErrors = new ArrayList<>();

        // 验证 Accept 头
        String accept = request.headers().get(ACCEPT);
        if (accept == null || !accept.contains(TEXT_EVENT_STREAM)) {
            badRequestErrors.add("text/event-stream required in Accept header");
        }
        if (accept == null || !accept.contains(APPLICATION_JSON)) {
            badRequestErrors.add("application/json required in Accept header");
        }

        // 从请求中提取传输上下文
        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        // 从 Netty 上下文中提取认证主体
        Object authSubject = McpAuthExtractor.extractAuthSubjectFromContext(ctx);
        transportContext.put(MCP_AUTH_SUBJECT_KEY, authSubject);

        // 从 HTTP header 中提取 User ID
        String userId = McpAuthExtractor.extractUserIdFromRequest(request);
        transportContext.put(McpAuthExtractor.MCP_USER_ID_KEY, userId);

        try {
            // 读取请求体
            ByteBuf content = request.content();
            String body = content.toString(CharsetUtil.UTF_8);

            // 反序列化 JSON-RPC 消息
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            // 处理初始化请求
            if (message instanceof McpSchema.JSONRPCRequest) {
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                // 检查是否为初始化方法
                if (jsonrpcRequest.getMethod().equals(McpSchema.METHOD_INITIALIZE)) {
                    // 对于初始化请求，检查验证错误
                    if (!badRequestErrors.isEmpty()) {
                        String combinedMessage = String.join("; ", badRequestErrors);
                        sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError(combinedMessage));
                        return;
                    }

                    // 转换初始化请求参数
                    McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(jsonrpcRequest.getParams(),
                            new TypeReference<McpSchema.InitializeRequest>() {
                            });
                    // 使用工厂启动新会话
                    McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
                            .startSession(initializeRequest);
                    // 将新会话添加到会话映射表
                    this.sessions.put(init.session().getId(), init.session());

                    try {
                        // 处理初始化结果
                        init.initResult()
                                .thenAccept(initResult -> {
                                    try {
                                        // 创建成功响应
                                        FullHttpResponse response = new DefaultFullHttpResponse(
                                                HttpVersion.HTTP_1_1,
                                                HttpResponseStatus.OK,
                                                Unpooled.copiedBuffer(objectMapper.writeValueAsString(
                                                        new McpSchema.JSONRPCResponse(
                                                                McpSchema.JSONRPC_VERSION, jsonrpcRequest.getId(), initResult, null)),
                                                        CharsetUtil.UTF_8)
                                        );

                                        // 设置响应头
                                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON);
                                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                        response.headers().set(HttpHeaders.MCP_SESSION_ID, init.session().getId());
                                        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

                                        // 发送响应并关闭连接
                                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                    } catch (Exception e) {
                                        logger.error("Failed to serialize init response: {}", e.getMessage());
                                        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                                new McpError("Failed to serialize response"));
                                    }
                                })
                                .exceptionally(e -> {
                                    // 处理初始化失败
                                    logger.error("Failed to initialize session: {}", e.getMessage());
                                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                            new McpError("Failed to initialize session: " + e.getMessage()));
                                    return null;
                                });
                        return;
                    } catch (Exception e) {
                        logger.error("Failed to initialize session: {}", e.getMessage());
                        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                new McpError("Failed to initialize session: " + e.getMessage()));
                        return;
                    }
                }
            }

            // 对于非初始化请求，验证会话 ID
            String sessionId = request.headers().get(HttpHeaders.MCP_SESSION_ID);
            if (sessionId == null || sessionId.trim().isEmpty()) {
                badRequestErrors.add("Session ID required in mcp-session-id header");
            }

            // 如果有验证错误，返回 400 错误
            if (!badRequestErrors.isEmpty()) {
                String combinedMessage = String.join("; ", badRequestErrors);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError(combinedMessage));
                return;
            }

            // 查找会话
            McpStreamableServerSession session = this.sessions.get(sessionId);
            if (session == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND,
                        new McpError("Session not found: " + sessionId));
                return;
            }

            // 处理不同类型的 JSON-RPC 消息
            if (message instanceof McpSchema.JSONRPCResponse) {
                // 处理响应消息
                McpSchema.JSONRPCResponse jsonrpcResponse = (McpSchema.JSONRPCResponse) message;
                session.accept(jsonrpcResponse)
                        .thenRun(() -> {
                            // 返回 202 已接受响应
                            FullHttpResponse response = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    HttpResponseStatus.ACCEPTED
                            );
                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        })
                        .exceptionally(e -> {
                            logger.error("Failed to accept response: {}", e.getMessage());
                            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, new McpError("Failed to accept response"));
                            return null;
                        });
            } else if (message instanceof McpSchema.JSONRPCNotification) {
                // 处理通知消息
                McpSchema.JSONRPCNotification jsonrpcNotification = (McpSchema.JSONRPCNotification) message;
                session.accept(jsonrpcNotification, transportContext)
                        .thenRun(() -> {
                            // 返回 202 已接受响应
                            FullHttpResponse response = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    HttpResponseStatus.ACCEPTED
                            );
                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        })
                        .exceptionally(e -> {
                            logger.error("Failed to accept notification: {}", e.getMessage());
                            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, new McpError("Failed to accept notification"));
                            return null;
                        });
            } else if (message instanceof McpSchema.JSONRPCRequest) {
                // 处理请求消息，需要返回 SSE 流式响应
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                // 对于流式响应，需要返回 SSE
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_EVENT_STREAM);
                response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

                // 写入响应头
                ctx.writeAndFlush(response);

                // 创建会话传输对象
                NettyStreamableMcpSessionTransport sessionTransport = new NettyStreamableMcpSessionTransport(
                        sessionId, ctx);

                try {
                    // 处理请求流
                    session.responseStream(jsonrpcRequest, sessionTransport, transportContext)
                            .whenComplete((result, e) -> {
                                if (e != null) {
                                    logger.error("Failed to handle request stream: {}", e.getMessage());
                                    sessionTransport.close();
                                }
                            });
                } catch (Exception e) {
                    logger.error("Failed to handle request stream: {}", e.getMessage());
                    sessionTransport.close();
                }
            } else {
                // 未知消息类型
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        new McpError("Unknown message type"));
            }
        } catch (IllegalArgumentException | IOException e) {
            // 反序列化失败
            logger.error("Failed to deserialize message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                    new McpError("Invalid message format: " + e.getMessage()));
        } catch (Exception e) {
            // 其他处理错误
            logger.error("Error handling message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    new McpError("Error processing message: " + e.getMessage()));
        }
    }

    /**
     * 处理删除会话的 DELETE 请求。
     * DELETE 请求用于主动终止并删除一个会话。
     *
     * @param ctx Netty 通道处理器上下文
     * @param request HTTP DELETE 请求
     */
    private void handleDeleteRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 检查是否禁止 DELETE 方法
        if (this.disallowDelete) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("DELETE method not allowed"));
            return;
        }

        // 提取传输上下文（虽然当前未使用，但保持一致性）
        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        // 获取并验证会话 ID
        String sessionId = request.headers().get(HttpHeaders.MCP_SESSION_ID);
        if (sessionId == null) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                    new McpError("Session ID required in mcp-session-id header"));
            return;
        }

        // 查找会话
        McpStreamableServerSession session = this.sessions.get(sessionId);
        if (session == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Session not found"));
            return;
        }

        try {
            // 删除会话
            session.delete()
                    .thenRun(() -> {
                        // 从会话映射表中移除会话
                        this.sessions.remove(sessionId);
                        // 返回 200 成功响应
                        FullHttpResponse response = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1,
                                HttpResponseStatus.OK
                        );
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                        ctx.writeAndFlush(response);
                    })
                    .exceptionally(e -> {
                        // 处理删除失败
                        logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
                        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                new McpError(e.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            // 捕获同步异常
            logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    new McpError("Error deleting session"));
        }
    }

    /**
     * 向客户端发送错误响应。
     * 该方法创建包含错误信息的 JSON 响应并发送给客户端，然后关闭连接。
     *
     * @param ctx Netty 通道处理器上下文
     * @param status HTTP 响应状态码
     * @param mcpError MCP 错误对象
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, McpError mcpError) {
        try {
            // 将错误对象序列化为 JSON
            String jsonError = objectMapper.writeValueAsString(mcpError);
            // 创建字节缓冲区
            ByteBuf content = Unpooled.copiedBuffer(jsonError, CharsetUtil.UTF_8);

            // 创建完整 HTTP 响应
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
            // 如果发送错误响应失败，记录日志并发送空的 500 响应
            logger.error(FAILED_TO_SEND_ERROR_RESPONSE, e.getMessage());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    // Netty SSE 会话的 McpStreamableServerTransport 实现
    // 该内部类实现了 MCP 可流式服务器传输接口，用于通过 Netty SSE 连接发送消息
    private class NettyStreamableMcpSessionTransport implements McpStreamableServerTransport {

        // 会话 ID，用于标识会话
        private final String sessionId;

        // Netty 通道处理器上下文，用于写入 SSE 数据
        private final ChannelHandlerContext ctx;

        // 标志位，指示传输是否已关闭
        private final AtomicBoolean closed = new AtomicBoolean(false);

        // 可重入锁，用于保护发送和关闭操作的线程安全
        private final ReentrantLock lock = new ReentrantLock();

        /**
         * 构造一个新的 Netty 可流式 MCP 会话传输实例。
         *
         * @param sessionId 会话 ID
         * @param ctx Netty 通道处理器上下文
         */
        NettyStreamableMcpSessionTransport(String sessionId, ChannelHandlerContext ctx) {
            this.sessionId = sessionId;
            this.ctx = ctx;
            logger.debug("Streamable session transport {} initialized", sessionId);
        }

        /**
         * 发送消息，不指定消息 ID。
         *
         * @param message 要发送的 JSON-RPC 消息
         * @return CompletableFuture，当消息发送完成时完成
         */
        @Override
        public CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        /**
         * 发送消息，可选指定消息 ID。
         *
         * @param message 要发送的 JSON-RPC 消息
         * @param messageId 可选的消息 ID，用于 SSE 事件的 id 字段
         * @return CompletableFuture，当消息发送完成时完成
         */
        @Override
        public CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return CompletableFuture.runAsync(() -> {
                // 检查传输是否已关闭
                if (this.closed.get()) {
                    logger.warn("Attempted to send message to closed session: {}", this.sessionId);
                    return;
                }

                // 检查通道是否仍然活跃
                if (!this.ctx.channel().isActive()) {
                    logger.warn("Channel for session {} is not active, message will not be sent", this.sessionId);
                    return;
                }
                // 获取锁以保护发送操作
                lock.lock();
                try {
                    // 再次检查是否已关闭（双重检查锁定模式）
                    if (this.closed.get()) {
                        logger.debug("Session {} was closed during message send attempt", this.sessionId);
                        return;
                    }

                    // 将消息序列化为 JSON
                    String jsonText = objectMapper.writeValueAsString(message);
                    logger.debug("Sending SSE message to session {}: {}", this.sessionId,
                        jsonText.length() > 200 ? jsonText.substring(0, 200) + "..." : jsonText);
                    // 发送 SSE 事件
                    sendSseEvent(MESSAGE_EVENT_TYPE, jsonText, messageId != null ? messageId : this.sessionId);
                    logger.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
                } catch (Exception e) {
                    // 发送失败时记录错误并关闭通道
                    logger.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
                    this.ctx.close();
                } finally {
                    // 释放锁
                    lock.unlock();
                }
            });
        }

        /**
         * 将数据对象反序列化为指定类型。
         *
         * @param data 要反序列化的数据对象
         * @param typeRef 目标类型的类型引用
         * @param <T> 目标类型
         * @return 反序列化后的对象
         */
        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        /**
         * 优雅地关闭传输。
         *
         * @return CompletableFuture，当关闭操作完成时完成
         */
        @Override
        public CompletableFuture<Void> closeGracefully() {
            return CompletableFuture.runAsync(this::close);
        }

        /**
         * 关闭传输。
         * 该方法会发送 SSE 流的结束标记并关闭连接。
         */
        @Override
        public void close() {
            // 获取锁以保护关闭操作
            lock.lock();
            try {
                // 检查是否已经关闭
                if (this.closed.get()) {
                    logger.debug("Session transport {} already closed", this.sessionId);
                    return;
                }

                // 设置关闭标志
                this.closed.set(true);
                // 如果通道仍然活跃，发送结束标记并关闭
                if (ctx.channel().isActive()) {
                    ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                            .addListener(ChannelFutureListener.CLOSE);
                }
                logger.debug("Successfully closed session transport {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to close session transport {}: {}", sessionId, e.getMessage());
            } finally {
                // 释放锁
                lock.unlock();
            }
        }

        /**
         * 获取底层 Netty 通道。
         *
         * @return Netty 通道实例
         */
        @Override
        public Channel getChannel() {
            return ctx.channel();
        }

        /**
         * 发送 SSE 事件。
         * SSE 事件格式：id: <id>\nevent: <type>\ndata: <data>\n\n
         *
         * @param eventType 事件类型
         * @param data 事件数据（JSON 字符串）
         * @param id 可选的事件 ID
         */
        private void sendSseEvent(String eventType, String data, String id) {
            // 构建 SSE 事件字符串
            StringBuilder sseData = new StringBuilder();
            if (id != null) {
                sseData.append("id: ").append(id).append("\n");
            }
            sseData.append("event: ").append(eventType).append("\n");
            sseData.append("data: ").append(data).append("\n\n");

            // 创建字节缓冲区并发送
            ByteBuf buffer = Unpooled.copiedBuffer(sseData.toString(), CharsetUtil.UTF_8);
            this.ctx.writeAndFlush(new DefaultHttpContent(buffer));

            logger.debug("SSE event sent - Type: {}, ID: {}, Data length: {}",
                eventType, id, data != null ? data.length() : 0);
        }
    }

    /**
     * 创建一个新的构建器实例。
     *
     * @return 新的 Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 用于创建 {@link McpStreamableHttpRequestHandler} 实例的构建器。
     * 该构建器提供流式 API 以配置处理器的各个属性。
     */
    public static class Builder {

        // Jackson ObjectMapper，用于 JSON 序列化/反序列化（必需）
        private ObjectMapper objectMapper;

        // MCP 端点 URI，默认为 "/mcp"
        private String mcpEndpoint = "/mcp";

        // 是否禁止 DELETE 方法，默认为 false
        private boolean disallowDelete = false;

        // 传输上下文提取器，默认为返回原始上下文的实现
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (serverRequest, context) -> context;

        // 保活间隔，null 表示禁用
        private Duration keepAliveInterval;

        /**
         * 设置 ObjectMapper。
         *
         * @param ObjectMapper JSON 序列化/反序列化的 ObjectMapper
         * @return this 构建器实例
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * 设置 MCP 端点 URI。
         *
         * @param mcpEndpoint 端点 URI
         * @return this 构建器实例
         */
        public Builder mcpEndpoint(String mcpEndpoint) {
            Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        /**
         * 设置是否禁止 DELETE 方法。
         *
         * @param disallowDelete true 表示禁止 DELETE 请求
         * @return this 构建器实例
         */
        public Builder disallowDelete(boolean disallowDelete) {
            this.disallowDelete = disallowDelete;
            return this;
        }

        /**
         * 设置传输上下文提取器。
         *
         * @param contextExtractor 上下文提取器
         * @return this 构建器实例
         */
        public Builder contextExtractor(McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        /**
         * 设置保活间隔。
         *
         * @param keepAliveInterval 保活间隔，null 表示禁用
         * @return this 构建器实例
         */
        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        /**
         * 构建新的 McpStreamableHttpRequestHandler 实例。
         *
         * @return 新配置的处理程序实例
         * @throws IllegalArgumentException 如果必需参数未设置
         */
        public McpStreamableHttpRequestHandler build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            return new McpStreamableHttpRequestHandler(this.objectMapper, this.mcpEndpoint,
                    this.disallowDelete, this.contextExtractor, this.keepAliveInterval);
        }
    }

}

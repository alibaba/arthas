package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.config.McpServerProperties.ServerProtocol;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.util.Assert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCP HTTP请求处理器，负责分发请求到无状态或流式处理器。
 *
 * 该类是MCP服务器的HTTP请求入口点，根据配置的服务器协议类型（无状态或流式），
 * 将传入的HTTP请求分发到相应的处理器进行处理。它还负责优雅关闭机制和错误处理。
 *
 * @author Yeaury
 */
public class McpHttpRequestHandler {

    /**
     * 日志记录器，用于记录处理器的运行状态和错误信息
     */
    private static final Logger logger = LoggerFactory.getLogger(McpHttpRequestHandler.class);

    /**
     * JSON媒体类型常量，用于HTTP响应头
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * 服务器发送事件（SSE）媒体类型常量，用于流式传输
     */
    public static final String TEXT_EVENT_STREAM = "text/event-stream";

    /**
     * HTTP Accept请求头名称常量
     */
    private static final String ACCEPT_HEADER = "Accept";

    /**
     * MCP端点路径，例如 "/mcp"
     */
    private final String mcpEndpoint;

    /**
     * JSON序列化/反序列化映射器，用于处理JSON格式的请求和响应
     */
    private final ObjectMapper objectMapper;

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
     * 无状态协议处理器，处理无状态模式的MCP请求
     * 无状态模式不维护客户端会话状态
     */
    private McpStatelessHttpRequestHandler statelessHandler;

    /**
     * 流式协议处理器，处理流式模式的MCP请求
     * 流式模式支持服务器推送事件（SSE）
     */
    private McpStreamableHttpRequestHandler streamableHandler;

    /**
     * 服务器协议类型枚举，指定使用无状态还是流式协议
     */
    private ServerProtocol protocol;

    /**
     * 构造MCP HTTP请求处理器。
     *
     * @param mcpEndpoint MCP端点路径，客户端将向此路径发送请求
     * @param objectMapper JSON映射器，用于请求和响应的序列化与反序列化
     * @param contextExtractor 传输上下文提取器，从HTTP请求中提取MCP上下文信息
     * @throws IllegalArgumentException 如果任何参数为null
     */
    public McpHttpRequestHandler(String mcpEndpoint, ObjectMapper objectMapper,
                                McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
        Assert.notNull(mcpEndpoint, "mcpEndpoint must not be null");
        Assert.notNull(objectMapper, "objectMapper must not be null");
        Assert.notNull(contextExtractor, "contextExtractor must not be null");

        this.mcpEndpoint = mcpEndpoint;
        this.objectMapper = objectMapper;
        this.contextExtractor = contextExtractor;
    }

    /**
     * 设置服务器协议类型。
     *
     * @param protocol 服务器协议类型（无状态或流式）
     */
    public void setProtocol(ServerProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * 设置无状态请求处理器。
     *
     * @param statelessHandler 无状态HTTP请求处理器实例
     */
    public void setStatelessHandler(McpStatelessHttpRequestHandler statelessHandler) {
        this.statelessHandler = statelessHandler;
    }

    /**
     * 设置流式请求处理器。
     *
     * @param streamableHandler 流式HTTP请求处理器实例
     */
    public void setStreamableHandler(McpStreamableHttpRequestHandler streamableHandler) {
        this.streamableHandler = streamableHandler;
    }

    /**
     * 处理传入的HTTP请求。
     *
     * 该方法执行以下操作：
     * 1. 验证请求URI是否匹配配置的MCP端点
     * 2. 检查服务器是否正在关闭
     * 3. 根据协议类型分发请求到相应的处理器（无状态或流式）
     * 4. 捕获并处理任何异常情况
     *
     * @param ctx Netty通道处理上下文，用于写入响应
     * @param request 完整的HTTP请求对象
     * @throws Exception 如果处理过程中发生严重错误
     */
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
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

        // 记录调试信息，包括请求方法、URI和使用的协议类型
        logger.debug("Request {} {} -> using {} transport",
            request.method(), request.uri(), protocol);

        try {
            // 根据协议类型分发请求
            if (protocol == ServerProtocol.STREAMABLE) {
                // 流式协议处理
                if (streamableHandler == null) {
                    // 流式处理器未配置，返回503错误
                    sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE,
                        new McpError("Streamable transport handler not available"));
                    return;
                }
                // 委托给流式处理器处理请求
                streamableHandler.handle(ctx, request);
            } else {
                // 无状态协议处理
                if (statelessHandler == null) {
                    // 无状态处理器未配置，返回503错误
                    sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE,
                        new McpError("Stateless transport handler not available"));
                    return;
                }
                // 委托给无状态处理器处理请求
                statelessHandler.handle(ctx, request);
            }
        } catch (Exception e) {
            // 捕获处理过程中的任何异常
            logger.error("Error handling request: {}", e.getMessage(), e);
            // 返回500内部服务器错误
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                new McpError("Error processing request: " + e.getMessage()));
        }
    }

    /**
     * 优雅关闭处理器。
     *
     * 该方法执行以下操作：
     * 1. 设置关闭标志，拒绝新请求
     * 2. 等待无状态处理器完成关闭（如果已配置）
     * 3. 等待流式处理器完成关闭（如果已配置）
     * 4. 等待所有关闭操作完成后返回
     *
     * 优雅关闭确保正在处理的请求能够完成，同时拒绝新的请求。
     *
     * @return CompletableFuture，在所有关闭操作完成后完成
     */
    public CompletableFuture<Void> closeGracefully() {
        return CompletableFuture.runAsync(() -> {
            // 设置关闭标志，阻止新请求被处理
            this.isClosing.set(true);
            logger.debug("Initiating graceful shutdown of MCP handler");

            // 初始化无状态和流式处理器的关闭Future
            CompletableFuture<Void> statelessClose = CompletableFuture.completedFuture(null);
            CompletableFuture<Void> streamableClose = CompletableFuture.completedFuture(null);

            // 如果无状态处理器存在，启动其关闭流程
            if (statelessHandler != null) {
                statelessClose = statelessHandler.closeGracefully();
            }

            // 如果流式处理器存在，启动其关闭流程
            if (streamableHandler != null) {
                streamableClose = streamableHandler.closeGracefully();
            }

            // 等待两个处理器都完成关闭
            CompletableFuture.allOf(statelessClose, streamableClose).join();
            logger.debug("Graceful shutdown completed");
        });
    }

    /**
     * 发送错误响应给客户端。
     *
     * 该方法将MCPError对象序列化为JSON格式，并通过HTTP响应返回给客户端。
     * 如果序列化失败，则返回一个简单的500内部服务器错误响应。
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
            logger.error("Failed to send error response: {}", e.getMessage());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 获取MCP端点路径。
     *
     * @return MCP端点路径字符串
     */
    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    /**
     * 创建Builder实例用于构建McpHttpRequestHandler。
     *
     * @return 新的Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * McpHttpRequestHandler的构建器类。
     *
     * 提供流式API用于构建McpHttpRequestHandler实例，支持链式调用。
     * 所有必需的参数都在build()方法中进行验证。
     */
    public static class Builder {
        /**
         * MCP端点路径，默认为 "/mcp"
         */
        private String mcpEndpoint = "/mcp";

        /**
         * JSON对象映射器，必须设置
         */
        private ObjectMapper objectMapper;

        /**
         * 传输上下文提取器，默认为恒等函数（不提取任何额外上下文）
         */
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (request, context) -> context;

        /**
         * 服务器协议类型，默认为STREAMABLE（流式）
         */
        private ServerProtocol protocol;

        /**
         * 设置MCP端点路径。
         *
         * @param mcpEndpoint MCP端点路径，不能为null
         * @return 当前Builder实例，支持链式调用
         */
        public Builder mcpEndpoint(String mcpEndpoint) {
            Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        /**
         * 设置JSON对象映射器。
         *
         * @param ObjectMapper JSON对象映射器，不能为null
         * @return 当前Builder实例，支持链式调用
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * 设置传输上下文提取器。
         *
         * @param contextExtractor 上下文提取器，不能为null
         * @return 当前Builder实例，支持链式调用
         */
        public Builder contextExtractor(McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        /**
         * 设置服务器协议类型。
         *
         * @param protocol 协议类型（STREAMABLE或STATELESS）
         * @return 当前Builder实例，支持链式调用
         */
        public Builder protocol(ServerProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * 构建McpHttpRequestHandler实例。
         *
         * 该方法验证所有必需参数，创建处理器实例并设置协议类型。
         * 如果协议类型未设置，默认使用STREAMABLE（流式）。
         *
         * @return 配置好的McpHttpRequestHandler实例
         * @throws IllegalArgumentException 如果objectMapper或mcpEndpoint未设置
         */
        public McpHttpRequestHandler build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            // 如果未指定协议类型，默认使用流式协议
            if (this.protocol == null) {
                this.protocol = ServerProtocol.STREAMABLE;
            }

            // 创建处理器实例并设置协议
            McpHttpRequestHandler handler = new McpHttpRequestHandler(this.mcpEndpoint, this.objectMapper, this.contextExtractor);
            handler.setProtocol(this.protocol);
            return handler;
        }
    }
}

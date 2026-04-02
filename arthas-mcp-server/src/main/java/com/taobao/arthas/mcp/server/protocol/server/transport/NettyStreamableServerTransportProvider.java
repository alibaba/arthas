/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpStreamableHttpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.spec.McpStreamableServerSession;
import com.taobao.arthas.mcp.server.protocol.spec.McpStreamableServerTransportProvider;
import com.taobao.arthas.mcp.server.protocol.spec.ProtocolVersions;
import com.taobao.arthas.mcp.server.util.Assert;
import io.netty.handler.codec.http.FullHttpRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 模型上下文协议（MCP）可流式传输层的服务器端实现，使用 HTTP 和服务器发送事件（SSE）通过 Netty 实现。
 * 该实现提供了 Netty 操作和 MCP 传输接口之间的桥梁。
 *
 * @see McpStreamableServerTransportProvider
 */
public class NettyStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

    /**
     * 默认的 MCP 端点 URI。
     * 客户端默认将 JSON-RPC 消息发送到此端点。
     */
    public static final String DEFAULT_MCP_ENDPOINT = "/mcp";

    /**
     * 底层的 HTTP 请求处理器。
     * 该处理器负责处理所有传入的 MCP HTTP 请求，包括 SSE 连接管理。
     */
    private final McpStreamableHttpRequestHandler requestHandler;

    /**
     * 构造一个新的 NettyStreamableServerTransportProvider 实例。
     *
     * @param objectMapper 用于 JSON 消息序列化/反序列化的 ObjectMapper
     * @param mcpEndpoint 客户端通过 HTTP 发送 JSON-RPC 消息的端点 URI
     * @param disallowDelete 是否禁止在端点上使用 DELETE 请求
     * @param contextExtractor 从请求中提取传输上下文的提取器
     * @param keepAliveInterval 保活 ping 的间隔（null 表示禁用）
     * @throws IllegalArgumentException 如果任何参数为 null
     */
    private NettyStreamableServerTransportProvider(ObjectMapper objectMapper, String mcpEndpoint,
                                                   boolean disallowDelete, McpTransportContextExtractor<FullHttpRequest> contextExtractor,
                                                   Duration keepAliveInterval) {
        // 验证必需参数
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        // 创建底层的 HTTP 请求处理器
        this.requestHandler = new McpStreamableHttpRequestHandler(objectMapper, mcpEndpoint, disallowDelete, contextExtractor, keepAliveInterval);
    }

    /**
     * 获取支持的协议版本列表。
     * 该传输提供程序支持多个 MCP 协议版本。
     *
     * @return 支持的协议版本列表
     */
    @Override
    public List<String> protocolVersions() {
        return Arrays.asList(ProtocolVersions.MCP_2024_11_05, ProtocolVersions.MCP_2025_03_26,
                ProtocolVersions.MCP_2025_06_18);
    }

    /**
     * 设置 MCP 可流式服务器会话工厂。
     * 该工厂用于在客户端初始化时创建新的会话实例。
     *
     * @param sessionFactory 会话工厂
     */
    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        requestHandler.setSessionFactory(sessionFactory);
    }

    /**
     * 通过 SSE 连接向所有连接的客户端广播通知。
     * 该方法会并行地向所有活动会话发送通知，不会因为单个会话失败而影响其他会话。
     *
     * @param method 通知的方法名称
     * @param params 通知的参数对象
     * @return CompletableFuture，当广播尝试完成时完成
     */
    @Override
    public CompletableFuture<Void> notifyClients(String method, Object params) {
        return requestHandler.notifyClients(method, params);
    }

    /**
     * 启动传输层的优雅关闭流程。
     * 该方法会关闭所有活动会话并清理资源，不会中断正在处理的请求。
     *
     * @return CompletableFuture，当所有清理操作完成时完成
     */
    @Override
    public CompletableFuture<Void> closeGracefully() {
        return requestHandler.closeGracefully();
    }

    /**
     * 获取底层的 MCP HTTP 请求处理器。
     * 该处理器可用于与 Netty 管道集成。
     *
     * @return McpStreamableHttpRequestHandler 实例
     */
    @Override
    public McpStreamableHttpRequestHandler getMcpRequestHandler() {
        if (this.requestHandler != null) {
            return this.requestHandler;
        }
        throw new UnsupportedOperationException("Streamable transport provider does not support legacy SSE request handler");
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
     * 用于创建 {@link NettyStreamableServerTransportProvider} 实例的构建器。
     * 该构建器提供流式 API 以配置传输提供程序的各个属性。
     */
    public static class Builder {

        // Jackson ObjectMapper，用于 JSON 序列化/反序列化（必需）
        private ObjectMapper objectMapper;

        // MCP 端点 URI，默认为 "/mcp"
        private String mcpEndpoint = DEFAULT_MCP_ENDPOINT;

        // 是否禁止 DELETE 方法，默认为 false
        private boolean disallowDelete = false;

        // 传输上下文提取器，默认为返回原始上下文的实现
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (serverRequest, context) -> context;

        // 保活间隔，null 表示禁用
        private Duration keepAliveInterval;

        /**
         * 设置 ObjectMapper。
         *
         * @param objectMapper JSON 序列化/反序列化的 ObjectMapper
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
         * 构建新的 NettyStreamableServerTransportProvider 实例。
         *
         * @return 新配置的传输提供程序实例
         * @throws IllegalArgumentException 如果必需参数未设置
         */
        public NettyStreamableServerTransportProvider build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            return new NettyStreamableServerTransportProvider(this.objectMapper, this.mcpEndpoint,
                    this.disallowDelete, this.contextExtractor, this.keepAliveInterval);
        }
    }

    // KeepAliveScheduler 的占位符接口（如果尚未实现）
    // 该接口用于保活调度器，管理会话的心跳 ping
    private interface KeepAliveScheduler {
        /**
         * 关闭保活调度器并释放资源。
         */
        void shutdown();
    }

}

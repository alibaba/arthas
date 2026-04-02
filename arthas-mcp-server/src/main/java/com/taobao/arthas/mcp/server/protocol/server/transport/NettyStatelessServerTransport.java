/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.server.McpStatelessServerHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpStatelessHttpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.spec.McpStatelessServerTransport;
import com.taobao.arthas.mcp.server.util.Assert;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.concurrent.CompletableFuture;

/**
 * 模型上下文协议（MCP）无状态传输层的服务器端实现，使用 HTTP 通过 Netty 实现。
 * 该实现提供了 Netty 操作和 MCP 传输接口之间的桥梁，用于无状态操作。
 *
 * @see McpStatelessServerTransport
 */
public class NettyStatelessServerTransport implements McpStatelessServerTransport {

    /**
     * 默认的 MCP 端点 URI。
     * 客户端默认将 JSON-RPC 消息发送到此端点。
     */
    public static final String DEFAULT_MCP_ENDPOINT = "/mcp";

    /**
     * 底层的 HTTP 请求处理器。
     * 该处理器负责处理所有传入的 MCP HTTP 请求。
     */
    private final McpStatelessHttpRequestHandler requestHandler;

    /**
     * 构造一个新的 NettyStatelessServerTransport 实例。
     *
     * @param objectMapper 用于 JSON 消息序列化/反序列化的 ObjectMapper
     * @param mcpEndpoint 客户端通过 HTTP 发送 JSON-RPC 消息的端点 URI
     * @param contextExtractor 从请求中提取传输上下文的提取器
     * @throws IllegalArgumentException 如果任何参数为 null
     */
    private NettyStatelessServerTransport(ObjectMapper objectMapper, String mcpEndpoint,
                                          McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
        // 验证必需参数
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        // 创建底层的 HTTP 请求处理器
        this.requestHandler = new McpStatelessHttpRequestHandler(objectMapper, mcpEndpoint, contextExtractor);
    }

    /**
     * 设置 MCP 无状态服务器处理器。
     * 该处理器将处理所有无状态的 MCP 请求。
     *
     * @param mcpHandler MCP 无状态服务器处理器
     */
    @Override
    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        requestHandler.setMcpHandler(mcpHandler);
    }

    /**
     * 启动传输层的优雅关闭流程。
     * 该方法会等待所有正在处理的请求完成，然后清理资源。
     *
     * @return CompletableFuture，当所有清理操作完成时完成
     */
    @Override
    public CompletableFuture<Void> closeGracefully() {
        return requestHandler.closeGracefully();
    }

    /**
     * 获取底层的 HTTP 请求处理器。
     * 该处理器可用于与 Netty 管道集成。
     *
     * @return McpStatelessHttpRequestHandler 实例
     */
    public McpStatelessHttpRequestHandler getMcpRequestHandler() {
        if (this.requestHandler != null) {
            return this.requestHandler;
        }
        throw new UnsupportedOperationException("Stateless transport provider does not support request handler");
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
     * 用于创建 {@link NettyStatelessServerTransport} 实例的构建器。
     * 该构建器提供流式 API 以配置传输层的各个属性。
     */
    public static class Builder {

        // Jackson ObjectMapper，用于 JSON 序列化/反序列化（必需）
        private ObjectMapper objectMapper;

        // MCP 端点 URI，默认为 "/mcp"
        private String mcpEndpoint = DEFAULT_MCP_ENDPOINT;

        // 传输上下文提取器，默认为返回原始上下文的实现
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (serverRequest, context) -> context;

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
         * 构建新的 NettyStatelessServerTransport 实例。
         *
         * @return 新配置的传输层实例
         * @throws IllegalArgumentException 如果必需参数未设置
         */
        public NettyStatelessServerTransport build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            return new NettyStatelessServerTransport(this.objectMapper, this.mcpEndpoint, this.contextExtractor);
        }
    }
}

/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpStreamableHttpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.spec.McpStreamableServerSession;
import com.taobao.arthas.mcp.server.protocol.spec.McpStreamableServerTransportProvider;
import com.taobao.arthas.mcp.server.util.Assert;
import io.netty.handler.codec.http.FullHttpRequest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.taobao.arthas.mcp.server.ArthasMcpServer.DEFAULT_MCP_ENDPOINT;

/**
 * Server-side implementation of the Model Context Protocol (MCP) streamable transport
 * layer using HTTP with Server-Sent Events (SSE) through Netty. This implementation
 * provides a bridge between Netty operations and the MCP transport interface.
 *
 * @see McpStreamableServerTransportProvider
 */
public class NettyStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

    private final McpStreamableHttpRequestHandler requestHandler;

    /**
     * Constructs a new NettyStreamableServerTransportProvider instance.
     * 
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     *                     of messages.
     * @param mcpEndpoint The endpoint URI where clients should send their JSON-RPC
     *                    messages via HTTP.
     * @param disallowDelete Whether to disallow DELETE requests on the endpoint.
     * @param contextExtractor The extractor for transport context from the request.
     * @param keepAliveInterval Interval for keep-alive pings (null to disable)
     * @throws IllegalArgumentException if any parameter is null
     */
    private NettyStreamableServerTransportProvider(ObjectMapper objectMapper, String mcpEndpoint,
                                                   boolean disallowDelete, McpTransportContextExtractor<FullHttpRequest> contextExtractor,
                                                   Duration keepAliveInterval) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        this.requestHandler = new McpStreamableHttpRequestHandler(objectMapper, mcpEndpoint, disallowDelete, contextExtractor, keepAliveInterval);
    }

    @Override
    public String protocolVersion() {
        return "2025-03-26";
    }

    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        requestHandler.setSessionFactory(sessionFactory);
    }

    /**
     * Broadcasts a notification to all connected clients through their SSE connections.
     * 
     * @param method The method name for the notification
     * @param params The parameters for the notification
     * @return A CompletableFuture that completes when the broadcast attempt is finished
     */
    @Override
    public CompletableFuture<Void> notifyClients(String method, Object params) {
        return requestHandler.notifyClients(method, params);
    }

    /**
     * Initiates a graceful shutdown of the transport.
     * 
     * @return A CompletableFuture that completes when all cleanup operations are finished
     */
    @Override
    public CompletableFuture<Void> closeGracefully() {
        return requestHandler.closeGracefully();
    }

    @Override
    public McpStreamableHttpRequestHandler getMcpRequestHandler() {
        if (this.requestHandler != null) {
            return this.requestHandler;
        }
        throw new UnsupportedOperationException("Streamable transport provider does not support legacy SSE request handler");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating instances of {@link NettyStreamableServerTransportProvider}.
     */
    public static class Builder {

        private ObjectMapper objectMapper;
        private String mcpEndpoint = DEFAULT_MCP_ENDPOINT;
        private boolean disallowDelete = false;
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (serverRequest, context) -> context;
        private Duration keepAliveInterval;

        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder mcpEndpoint(String mcpEndpoint) {
            Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        public Builder disallowDelete(boolean disallowDelete) {
            this.disallowDelete = disallowDelete;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public NettyStreamableServerTransportProvider build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            return new NettyStreamableServerTransportProvider(this.objectMapper, this.mcpEndpoint,
                    this.disallowDelete, this.contextExtractor, this.keepAliveInterval);
        }
    }

    // Placeholder interface for KeepAliveScheduler if not already implemented
    private interface KeepAliveScheduler {
        void shutdown();
    }

}

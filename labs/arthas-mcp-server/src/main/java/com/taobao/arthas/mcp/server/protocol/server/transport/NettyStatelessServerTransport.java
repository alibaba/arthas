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
 * Server-side implementation of the Model Context Protocol (MCP) stateless transport
 * layer using HTTP through Netty. This implementation provides a bridge between 
 * Netty operations and the MCP transport interface for stateless operations.
 *
 * @see McpStatelessServerTransport
 */
public class NettyStatelessServerTransport implements McpStatelessServerTransport {

    private final McpStatelessHttpRequestHandler requestHandler;

    /**
     * Constructs a new NettyStatelessServerTransportProvider instance.
     * 
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     *                     of messages.
     * @param mcpEndpoint The endpoint URI where clients should send their JSON-RPC
     *                    messages via HTTP.
     * @param contextExtractor The extractor for transport context from the request.
     * @throws IllegalArgumentException if any parameter is null
     */
    private NettyStatelessServerTransport(ObjectMapper objectMapper, String mcpEndpoint,
                                          McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        this.requestHandler = new McpStatelessHttpRequestHandler(objectMapper, mcpEndpoint, contextExtractor);
    }

    @Override
    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        requestHandler.setMcpHandler(mcpHandler);
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

    /**
     * Gets the underlying HTTP request handler.
     * 
     * @return The McpStatelessHttpRequestHandler instance
     */
    public McpStatelessHttpRequestHandler getMcpRequestHandler() {
        if (this.requestHandler != null) {
            return this.requestHandler;
        }
        throw new UnsupportedOperationException("Stateless transport provider does not support request handler");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating instances of {@link NettyStatelessServerTransport}.
     */
    public static class Builder {

        private ObjectMapper objectMapper;
        private String mcpEndpoint = "/mcp";
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (serverRequest, context) -> context;

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

        public Builder contextExtractor(McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public NettyStatelessServerTransport build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            return new NettyStatelessServerTransport(this.objectMapper, this.mcpEndpoint, this.contextExtractor);
        }
    }
}

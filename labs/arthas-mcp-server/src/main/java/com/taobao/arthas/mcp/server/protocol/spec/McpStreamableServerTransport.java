/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.CompletableFuture;

/**
 * Marker interface for the server-side MCP streamable transport.
 * This extends the basic server transport with streamable message sending capabilities.
 *
 */
public interface McpStreamableServerTransport extends McpServerTransport {

    CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId);

    <T> T unmarshalFrom(Object value, TypeReference<T> typeRef);
} 
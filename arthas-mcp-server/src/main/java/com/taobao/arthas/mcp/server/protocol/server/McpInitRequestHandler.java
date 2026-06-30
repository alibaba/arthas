/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;

/**
 * Handles MCP initialization requests from clients using CompletableFuture for async operations.
 * This is the Netty-specific version that doesn't depend on Reactor.
 */
public interface McpInitRequestHandler {

	/**
	 * Handles the initialization request.
	 * @param initializeRequest the initialization request by the client
	 * @return a CompletableFuture that will emit the result of the initialization
	 */
	CompletableFuture<McpSchema.InitializeResult> handle(McpSchema.InitializeRequest initializeRequest);

}

/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.protocol.server.handler.McpStreamableHttpRequestHandler;

import java.util.concurrent.CompletableFuture;

/**
 * Provides the abstraction for server-side transport providers in MCP protocol.
 * Defines methods for session factory setup, client notification, and graceful shutdown.
 *
 * @author Yeaury
 */
public interface McpServerTransportProvider {

	CompletableFuture<Void> notifyClients(String method, Object params);

	CompletableFuture<Void> closeGracefully();

	default void close() {
		closeGracefully();
	}

	McpStreamableHttpRequestHandler getMcpRequestHandler();

}

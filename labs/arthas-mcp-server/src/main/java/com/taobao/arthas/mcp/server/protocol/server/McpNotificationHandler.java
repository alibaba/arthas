/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;

import java.util.concurrent.CompletableFuture;

/**
 * Handles MCP notifications from clients using CompletableFuture for async operations.
 * This is the Netty-specific version that doesn't depend on Reactor.
 */
public interface McpNotificationHandler {

	/**
	 * Handles a notification from the client.
	 * @param exchange the exchange associated with the client that allows calling back to
	 * the connected client or inspecting its capabilities.
	 * @param params the parameters of the notification.
	 * @return a CompletableFuture that completes once the notification is handled.
	 */
	CompletableFuture<Void> handle(McpNettyServerExchange exchange, ArthasCommandContext arthasCommandContext, Object params);

}

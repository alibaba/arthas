/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;

public interface McpStatelessServerHandler {

	/**
	 * Handle the request using user-provided feature implementations.
	 * @param transportContext {@link McpTransportContext} carrying transport layer
	 * metadata
	 * @param request the request JSON object
	 * @return Mono containing the JSON response
	 */
	CompletableFuture<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext transportContext,
												  McpSchema.JSONRPCRequest request);

	/**
	 * Handle the notification.
	 * @param transportContext {@link McpTransportContext} carrying transport layer
	 * metadata
	 * @param notification the notification JSON object
	 * @return Mono that completes once handling is finished
	 */
	CompletableFuture<Void> handleNotification(McpTransportContext transportContext, McpSchema.JSONRPCNotification notification);

}

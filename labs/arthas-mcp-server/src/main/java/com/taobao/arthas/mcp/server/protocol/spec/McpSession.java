package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Represents a server-side MCP session that manages bidirectional JSON-RPC communication with the client.
 *
 * @author Yeaury
 */
public interface McpSession {

	<T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef);

	default CompletableFuture<Void> sendNotification(String method) {
		return sendNotification(method, null);
	}

	CompletableFuture<Void> sendNotification(String method, Object params);

	CompletableFuture<Void> closeGracefully();

	void close();

}

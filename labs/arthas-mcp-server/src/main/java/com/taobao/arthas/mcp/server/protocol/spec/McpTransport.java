package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Defines the transport abstraction for sending messages and unmarshalling data in MCP protocol.
 *
 * @author Yeaury
 */
public interface McpTransport {

	CompletableFuture<Void> closeGracefully();

	default void close() {
		this.closeGracefully();
	}

	CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message);

	<T> T unmarshalFrom(Object data, TypeReference<T> typeRef);

}
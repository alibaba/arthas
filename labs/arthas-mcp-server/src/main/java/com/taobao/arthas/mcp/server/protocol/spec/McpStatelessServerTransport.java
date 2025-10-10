/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.protocol.server.McpStatelessServerHandler;

import java.util.concurrent.CompletableFuture;

public interface McpStatelessServerTransport {

	void setMcpHandler(McpStatelessServerHandler mcpHandler);

	default void close() {
		this.closeGracefully();
	}

	CompletableFuture<Void> closeGracefully();

}

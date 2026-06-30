/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

public interface McpTransportContext {

	String KEY = "MCP_TRANSPORT_CONTEXT";

	McpTransportContext EMPTY = new DefaultMcpTransportContext();

	Object get(String key);

	void put(String key, Object value);

	McpTransportContext copy();

}

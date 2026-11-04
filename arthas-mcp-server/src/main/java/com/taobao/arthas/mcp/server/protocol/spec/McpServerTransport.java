/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import io.netty.channel.Channel;

/**
 * Extends McpTransport to provide access to the underlying Netty Channel for server-side transport.
 *
 * @author Yeaury
 */
public interface McpServerTransport extends McpTransport {

    Channel getChannel();
}

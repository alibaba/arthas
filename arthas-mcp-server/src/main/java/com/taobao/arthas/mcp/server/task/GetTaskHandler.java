/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;

/**
 * Optional custom handler for {@code tasks/get} requests.
 *
 * @author Yeaury
 */
@FunctionalInterface
public interface GetTaskHandler {

    CompletableFuture<McpSchema.GetTaskResult> handle(
        McpNettyServerExchange exchange,
        McpSchema.GetTaskRequest request
    );
}

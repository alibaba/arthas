/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;

/**
 * Optional custom handler for {@code tasks/result} requests.
 *
 * @author Yeaury
 */
@FunctionalInterface
public interface GetTaskResultHandler {

    CompletableFuture<McpSchema.ServerTaskPayloadResult> handle(
        McpNettyServerExchange exchange,
        McpSchema.GetTaskPayloadRequest request
    );
}

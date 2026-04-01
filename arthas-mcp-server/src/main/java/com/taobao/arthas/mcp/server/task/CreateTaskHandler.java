/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handler for task creation. Implementations start async work and return immediately with a task.
 *
 * @author Yeaury
 */
@FunctionalInterface
public interface CreateTaskHandler {

    CompletableFuture<McpSchema.CreateTaskResult> createTask(
        Map<String, Object> args,
        CreateTaskContext context
    );
}

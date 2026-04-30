/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Task lifecycle context provided to {@link CreateTaskHandler} implementations.
 *
 * @author Yeaury
 */
public interface CreateTaskContext {

    McpNettyServerExchange exchange();

    String sessionId();

    Long requestTtl();

    McpSchema.Request originatingRequest();

    ArthasCommandContext commandContext();

    CompletableFuture<McpSchema.Task> createTask();

    CompletableFuture<McpSchema.Task> createTask(Consumer<CreateTaskOptions.Builder> customizer);

    CompletableFuture<Void> completeTask(String taskId, McpSchema.CallToolResult result);

    CompletableFuture<Void> failTask(String taskId, McpSchema.CallToolResult errorResult);

    CompletableFuture<Void> setInputRequired(String taskId, String message);

    CompletableFuture<Boolean> isCancellationRequested(String taskId);

    ArthasCommandSessionManager sessionManager();

    ArthasCommandContext createIsolatedTaskSession(String taskId);

    void cleanupTaskSession(String taskId);

    boolean isAtConcurrencyLimit();
}

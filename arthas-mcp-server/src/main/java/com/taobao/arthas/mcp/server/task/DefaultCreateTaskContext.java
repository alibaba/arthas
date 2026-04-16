/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager.CommandSessionBinding;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Default implementation of {@link CreateTaskContext}.
 *
 * @author Yeaury
 */
public class DefaultCreateTaskContext implements CreateTaskContext {

    private final TaskStore<McpSchema.ServerTaskPayloadResult> taskStore;

    private final TaskMessageQueue messageQueue;

    private final McpNettyServerExchange exchange;

    private final String sessionId;

    private final Long requestTtl;

    private final McpSchema.Request originatingRequest;

    private final ArthasCommandContext commandContext;

    private final ArthasCommandSessionManager sessionManager;

    public DefaultCreateTaskContext(
            TaskStore<McpSchema.ServerTaskPayloadResult> taskStore,
            TaskMessageQueue messageQueue,
            McpNettyServerExchange exchange,
            String sessionId,
            Long requestTtl,
            McpSchema.Request originatingRequest,
            ArthasCommandContext commandContext,
            ArthasCommandSessionManager sessionManager) {
        this.taskStore = taskStore;
        this.messageQueue = messageQueue;
        this.exchange = exchange;
        this.sessionId = sessionId;
        this.requestTtl = requestTtl;
        this.originatingRequest = originatingRequest;
        this.commandContext = commandContext;
        this.sessionManager = sessionManager;
    }

    @Override
    public McpNettyServerExchange exchange() {
        return exchange;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public Long requestTtl() {
        return requestTtl;
    }

    @Override
    public McpSchema.Request originatingRequest() {
        return originatingRequest;
    }

    @Override
    public ArthasCommandContext commandContext() {
        return commandContext;
    }

    @Override
    public CompletableFuture<McpSchema.Task> createTask() {
        return createTask(builder -> {});
    }

    @Override
    public CompletableFuture<McpSchema.Task> createTask(Consumer<CreateTaskOptions.Builder> customizer) {
        CreateTaskOptions.Builder builder = CreateTaskOptions.builder()
                .sessionId(sessionId)
                .ttl(requestTtl)
                .originatingRequest(originatingRequest);
        customizer.accept(builder);
        return taskStore.createTask(builder.build());
    }

    @Override
    public CompletableFuture<Void> completeTask(String taskId, McpSchema.CallToolResult result) {
        return taskStore.storeTaskResult(taskId, sessionId, McpSchema.TaskStatus.COMPLETED, result);
    }

    @Override
    public CompletableFuture<Void> failTask(String taskId, McpSchema.CallToolResult errorResult) {
        return taskStore.storeTaskResult(taskId, sessionId, McpSchema.TaskStatus.FAILED, errorResult);
    }

    @Override
    public CompletableFuture<Void> setInputRequired(String taskId, String message) {
        return taskStore.updateTaskStatus(taskId, sessionId, McpSchema.TaskStatus.INPUT_REQUIRED, message);
    }

    TaskStore<McpSchema.ServerTaskPayloadResult> taskStore() {
        return taskStore;
    }

    TaskMessageQueue taskMessageQueue() {
        return messageQueue;
    }

    @Override
    public CompletableFuture<Boolean> isCancellationRequested(String taskId) {
        return taskStore.isCancellationRequested(taskId, sessionId);
    }

    @Override
    public ArthasCommandSessionManager sessionManager() {
        return sessionManager;
    }

    @Override
    public ArthasCommandContext createIsolatedTaskSession(String taskId) {
        if (sessionManager == null) {
            throw new IllegalStateException("SessionManager is not available");
        }
        CommandSessionBinding binding = sessionManager.createIsolatedTaskSession(taskId);
        return new ArthasCommandContext(commandContext.getCommandExecutor(), binding);
    }

    @Override
    public void cleanupTaskSession(String taskId) {
        if (sessionManager != null) {
            sessionManager.closeTaskSession(taskId);
        }
    }
}

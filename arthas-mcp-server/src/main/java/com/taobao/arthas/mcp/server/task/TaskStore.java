/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Persistent store for task state and results with session isolation.
 *
 * <p>Session validation rules: null {@code sessionId} allows all access (single-tenant);
 * tasks without a session are accessible from any session; otherwise sessionIds must match.
 *
 * <p>Error conventions: {@link #getTask} and {@link #getTaskResult} return null on miss;
 * {@link #storeTaskResult} throws on miss; {@link #updateTaskStatus} silently ignores misses;
 * {@link #requestCancellation} throws (-32602) for terminal tasks.
 *
 * @param <R> result type stored by this store
 * @author Yeaury
 */
public interface TaskStore<R extends McpSchema.Result> {

    CompletableFuture<McpSchema.Task> createTask(CreateTaskOptions options);

    CompletableFuture<GetTaskFromStoreResult> getTask(String taskId, String sessionId);

    CompletableFuture<Void> updateTaskStatus(String taskId, String sessionId,
                                              McpSchema.TaskStatus status, String statusMessage);

    CompletableFuture<Void> storeTaskResult(String taskId, String sessionId,
                                             McpSchema.TaskStatus status, R result);

    CompletableFuture<R> getTaskResult(String taskId, String sessionId);

    CompletableFuture<McpSchema.ListTasksResult> listTasks(String cursor, String sessionId);

    CompletableFuture<McpSchema.Task> requestCancellation(String taskId, String sessionId);

    CompletableFuture<Boolean> isCancellationRequested(String taskId, String sessionId);

    CompletableFuture<List<McpSchema.Task>> watchTaskUntilTerminal(
            String taskId, String sessionId, long timeoutMs);

    default CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }
}

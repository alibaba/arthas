/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;

/**
 * Communication interface between {@link TaskManager} and the protocol layer.
 *
 * @author Yeaury
 */
public interface TaskManagerHost {

    <T extends McpSchema.Result> CompletableFuture<T> request(McpSchema.Request request, Class<T> resultType);

    CompletableFuture<Void> notification(String notificationMethod, Object notification);

    /** Register a handler for a task-related method (e.g. tasks/get). */
    void registerHandler(String method, TaskRequestHandler handler);

    /**
     * Invoke a custom task handler if one is registered for the given task and method.
     * Returns null if no custom handler exists.
     */
    <T extends McpSchema.Result> CompletableFuture<T> invokeCustomTaskHandler(
            String taskId, String method, McpSchema.Request request,
            TaskHandlerContext context, Class<T> resultType);

    @FunctionalInterface
    interface TaskRequestHandler {
        CompletableFuture<McpSchema.Result> handle(String requestMethod, Object requestParams,
                                                    TaskHandlerContext context);
    }

    interface TaskHandlerContext {
        String sessionId();

        <T extends McpSchema.Result> CompletableFuture<T> sendRequest(String method, Object params,
                                                                       Class<T> resultType);

        CompletableFuture<Void> sendNotification(String method, Object notification);
    }
}

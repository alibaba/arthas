/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base for task handlers, managing TaskStore and TaskManager lifecycle.
 *
 * @param <S> result type stored in TaskStore
 * @author Yeaury
 */
public abstract class AbstractTaskHandler<S extends McpSchema.Result> implements TaskManagerHost {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTaskHandler.class);

    protected final TaskStore<S> taskStore;
    protected final TaskManager taskManager;
    protected final TaskHandlerRegistry taskHandlerRegistry = new TaskHandlerRegistry();

    protected AbstractTaskHandler(TaskStore<S> taskStore, TaskManagerOptions taskOptions) {
        this.taskStore = taskStore;
        if (taskOptions != null && taskStore != null) {
            this.taskManager = taskOptions.createTaskManager();
            this.taskManager.bind(this);
            logger.info("TaskManager created: {}", this.taskManager.getClass().getSimpleName());
        } else {
            this.taskManager = NullTaskManager.getInstance();
            logger.info("Using NullTaskManager (tasks not configured)");
        }
    }

    @Override
    public void registerHandler(String method, TaskRequestHandler handler) {
        this.taskHandlerRegistry.registerHandler(method, handler);
        logger.debug("Registered task handler for method: {}", method);
    }

    @Override
    public <T extends McpSchema.Result> CompletableFuture<T> invokeCustomTaskHandler(
            String taskId, String method, McpSchema.Request request,
            TaskHandlerContext context, Class<T> resultType) {

        if (this.taskStore == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.taskStore.getTask(taskId, context.sessionId())
                .thenCompose(storeResult -> {
                    if (storeResult == null) {
                        logger.debug("invokeCustomTaskHandler: task not found for taskId={}", taskId);
                        return CompletableFuture.completedFuture(null);
                    }
                    return findAndInvokeCustomHandler(storeResult, method, request, context, resultType);
                })
                .exceptionally(ex -> {
                    logger.debug("invokeCustomTaskHandler: task lookup failed for taskId={}, returning null",
                            taskId, ex);
                    return null;
                });
    }

    /** Hook for subclasses to find and invoke tool-specific custom handlers. Returns null by default. */
    protected <T extends McpSchema.Result> CompletableFuture<T> findAndInvokeCustomHandler(
            GetTaskFromStoreResult storeResult, String method, McpSchema.Request request,
            TaskHandlerContext context, Class<T> resultType) {
        return CompletableFuture.completedFuture(null);
    }

    public TaskStore<S> getTaskStore() {
        return this.taskStore;
    }

    public TaskManager taskManager() {
        return this.taskManager;
    }

    public void close() {
        if (this.taskManager != null) {
            try {
                this.taskManager.onClose();
                logger.info("TaskManager closed");
            } catch (Exception e) {
                logger.error("Error closing TaskManager", e);
            }
        }
        if (this.taskStore != null) {
            try {
                this.taskStore.shutdown().get(TaskDefaults.TASK_STORE_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                logger.info("TaskStore shutdown completed");
            } catch (Exception e) {
                logger.error("Error shutting down TaskStore", e);
            }
        }
    }

    public CompletableFuture<Void> closeGracefully() {
        if (this.taskManager != null) {
            this.taskManager.onClose();
        }
        return this.taskStore != null ? this.taskStore.shutdown() : CompletableFuture.completedFuture(null);
    }

    // ---------------------------------------
    // Handler Context Factory
    // ---------------------------------------

    protected static TaskManagerHost.TaskHandlerContext createTaskHandlerContext(
            String sessionId,
            TriFunction<String, Object, Class<? extends McpSchema.Result>, CompletableFuture<? extends McpSchema.Result>> requestSender,
            java.util.function.BiFunction<String, Object, CompletableFuture<Void>> notificationSender) {
        return new TaskManagerHost.TaskHandlerContext() {
            @Override
            public String sessionId() {
                return sessionId;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R extends McpSchema.Result> CompletableFuture<R> sendRequest(
                    String reqMethod, Object reqParams, Class<R> resultType) {
                return (CompletableFuture<R>) requestSender.apply(reqMethod, reqParams, resultType);
            }

            @Override
            public CompletableFuture<Void> sendNotification(String notifMethod, Object notification) {
                return notificationSender.apply(notifMethod, notification);
            }
        };
    }
}

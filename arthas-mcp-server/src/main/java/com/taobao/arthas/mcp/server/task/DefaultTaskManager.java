/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Default {@link TaskManager} implementation handling task orchestration and side-channel messaging.
 *
 * @see TaskManager
 * @see NullTaskManager
 */
class DefaultTaskManager implements TaskManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskManager.class);

    private static final String RELATED_TASK_META_KEY = "relatedTask";

    private final TaskStore<?> taskStore;
    private final TaskMessageQueue messageQueue;
    private final Duration defaultPollInterval;
    private final Duration pollTimeout;

    /** Pending request resolvers awaiting side-channel responses. */
    private final Map<Object, RequestResolver> requestResolvers = new ConcurrentHashMap<>();

    /** Stores response and error handlers for a pending request. */
    private static class RequestResolver {
        final Consumer<Object> responseHandler;
        final Consumer<Throwable> errorHandler;

        RequestResolver(Consumer<Object> responseHandler, Consumer<Throwable> errorHandler) {
            this.responseHandler = responseHandler;
            this.errorHandler = errorHandler;
        }
    }

    private TaskManagerHost host;

    DefaultTaskManager(TaskManagerOptions options) {
        this.taskStore = options.taskStore();
        this.messageQueue = options.messageQueue();
        this.defaultPollInterval = options.defaultPollInterval();
        this.pollTimeout = options.pollTimeout() != null
                ? options.pollTimeout()
                : Duration.ofMillis(TaskDefaults.DEFAULT_AUTOMATIC_POLLING_TIMEOUT_MS);
    }

    @Override
    public void bind(TaskManagerHost host) {
        this.host = host;

        if (this.taskStore != null) {
            host.registerHandler(McpSchema.METHOD_TASKS_GET, this::handleGetTask);
            host.registerHandler(McpSchema.METHOD_TASKS_RESULT, this::handleGetTaskResult);
            host.registerHandler(McpSchema.METHOD_TASKS_LIST, this::handleListTasks);
            host.registerHandler(McpSchema.METHOD_TASKS_CANCEL, this::handleCancelTask);
        }
    }

    @Override
    public InboundRequestResult processInboundRequest(String requestMethod, Object requestParams,
                                                       InboundRequestContext ctx) {
        String relatedTaskId = extractRelatedTaskId(requestParams);
        TaskCreationParams taskCreationParams = extractTaskCreationParams(requestParams);

        Consumer<Object> wrappedSendNotification;
        if (relatedTaskId != null) {
            wrappedSendNotification = notification -> ctx.sendNotification()
                    .send(notification, NotificationOptions.withRelatedTask(new RelatedTaskInfo(relatedTaskId)))
                    .exceptionally(ex -> {
                        logger.warn("Failed to send notification", ex);
                        return null;
                    });
        } else {
            wrappedSendNotification = notification -> ctx.sendNotification()
                    .send(notification, NotificationOptions.empty())
                    .exceptionally(ex -> {
                        logger.warn("Failed to send notification", ex);
                        return null;
                    });
        }

        RequestSender wrappedSendRequest = getSendRequest(ctx, relatedTaskId);

        Function<Object, CompletableFuture<Boolean>> routeResponse = response -> {
            if (relatedTaskId == null) {
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(false);
        };

        return new InboundRequestResult(
                wrappedSendNotification,
                wrappedSendRequest,
                routeResponse,
                taskCreationParams != null
        );
    }

    private static RequestSender getSendRequest(InboundRequestContext ctx, String relatedTaskId) {
        if (relatedTaskId != null) {
            return new RequestSender() {
                @Override
                public <T> CompletableFuture<T> send(Object request, Class<T> resultType, RequestOptions options) {
                    RequestOptions augmented = new RequestOptions(
                            options != null ? options.task() : null,
                            new RelatedTaskInfo(relatedTaskId));
                    return ctx.sendRequest().send(request, resultType, augmented);
                }
            };
        } else {
            return ctx.sendRequest();
        }
    }

    @Override
    public OutboundRequestResult processOutboundRequest(String requestMethod, Object requestParams,
                                                         RequestOptions options, Object messageId,
                                                         Consumer<Object> responseHandler,
                                                         Consumer<Throwable> errorHandler) {
        String relatedTaskId = options != null && options.relatedTask() != null
                ? options.relatedTask().taskId()
                : null;

        if (relatedTaskId != null && this.messageQueue != null) {
            this.requestResolvers.put(messageId, new RequestResolver(responseHandler, errorHandler));

            McpSchema.Request typedRequest = requestParams instanceof McpSchema.Request
                    ? (McpSchema.Request) requestParams : null;
            QueuedMessage.Request queuedRequest = new QueuedMessage.Request(messageId, requestMethod, typedRequest);
            this.messageQueue.enqueue(relatedTaskId, queuedRequest)
                    .exceptionally(ex -> {
                        errorHandler.accept(ex);
                        return null;
                    });

            return new OutboundRequestResult(true);
        }

        return new OutboundRequestResult(false);
    }

    @Override
    public InboundResponseResult processInboundResponse(Object responseResult, Object messageId) {
        RequestResolver resolver = this.requestResolvers.remove(messageId);
        if (resolver != null) {
            if (responseResult instanceof McpSchema.JSONRPCResponse) {
                McpSchema.JSONRPCResponse response = (McpSchema.JSONRPCResponse) responseResult;
                if (response.getError() != null) {
                    if (resolver.errorHandler != null) {
                        resolver.errorHandler.accept(new McpError(response.getError()));
                    } else {
                        resolver.responseHandler.accept(new McpError(response.getError()));
                    }
                } else {
                    resolver.responseHandler.accept(response.getResult());
                }
            } else if (responseResult instanceof Throwable) {
                if (resolver.errorHandler != null) {
                    resolver.errorHandler.accept((Throwable) responseResult);
                } else {
                    resolver.responseHandler.accept(responseResult);
                }
            } else {
                resolver.responseHandler.accept(responseResult);
            }
            return new InboundResponseResult(true);
        }

        return new InboundResponseResult(false);
    }

    @Override
    public CompletableFuture<OutboundNotificationResult> processOutboundNotification(
            String notificationMethod, Object notification, NotificationOptions options) {
        String relatedTaskId = options != null && options.relatedTask() != null
                ? options.relatedTask().taskId()
                : null;

        if (relatedTaskId != null && this.messageQueue != null) {
            QueuedMessage.Notification queuedNotification = new QueuedMessage.Notification(
                    notificationMethod, notification);

            return this.messageQueue.enqueue(relatedTaskId, queuedNotification)
                    .thenApply(v -> new OutboundNotificationResult(true, null));
        }

        return CompletableFuture.completedFuture(
                new OutboundNotificationResult(false, notification));
    }

    @Override
    public void onClose() {
        this.requestResolvers.clear();
    }

    @Override
    public Optional<TaskStore<?>> taskStore() {
        return Optional.ofNullable(this.taskStore);
    }

    @Override
    public Optional<TaskMessageQueue> messageQueue() {
        return Optional.ofNullable(this.messageQueue);
    }

    @Override
    public Duration defaultPollInterval() {
        return this.defaultPollInterval;
    }

    // Private helpers

    @SuppressWarnings("unchecked")
    private String extractRelatedTaskId(Object requestParams) {
        if (requestParams == null) {
            return null;
        }
        try {
            if (requestParams instanceof Map) {
                Map<String, Object> params = (Map<String, Object>) requestParams;
                Object meta = params.get("_meta");
                if (meta instanceof Map) {
                    Map<String, Object> metaMap = (Map<String, Object>) meta;
                    Object relatedTask = metaMap.get(RELATED_TASK_META_KEY);
                    if (relatedTask instanceof Map) {
                        Map<String, Object> relatedTaskMap = (Map<String, Object>) relatedTask;
                        return (String) relatedTaskMap.get("taskId");
                    }
                }
            }
        } catch (ClassCastException e) {
            logger.debug("Failed to extract related task ID: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private TaskCreationParams extractTaskCreationParams(Object requestParams) {
        if (requestParams == null) {
            return null;
        }
        try {
            if (requestParams instanceof Map) {
                Map<String, Object> params = (Map<String, Object>) requestParams;
                Object task = params.get("task");
                if (task instanceof Map) {
                    Map<String, Object> taskMap = (Map<String, Object>) task;
                    Long ttl = taskMap.containsKey("ttl")
                            ? ((Number) taskMap.get("ttl")).longValue()
                            : null;
                    return new TaskCreationParams(ttl);
                }
            }
        } catch (ClassCastException e) {
            logger.debug("Failed to extract task creation params: {}", e.getMessage());
        }
        return null;
    }

    // Handler implementations

    private CompletableFuture<McpSchema.Result> handleGetTask(String requestMethod, Object requestParams,
                                                               TaskManagerHost.TaskHandlerContext ctx) {
        if (this.taskStore == null) {
            CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
            failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("TaskStore not configured")
                    .build());
            return failed;
        }

        String taskId = extractTaskIdFromParams(requestParams);
        if (taskId == null) {
            CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
            failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                    .message("Missing required parameter: taskId")
                    .build());
            return failed;
        }

        McpSchema.GetTaskRequest typedRequest = new McpSchema.GetTaskRequest(taskId, null);

        return host.invokeCustomTaskHandler(taskId, McpSchema.METHOD_TASKS_GET, typedRequest, ctx,
                        McpSchema.GetTaskResult.class)
                .thenCompose(result -> {
                    if (result != null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    return this.taskStore.getTask(taskId, ctx.sessionId())
                            .thenCompose(storeResult -> {
                                if (storeResult == null) {
                                    CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
                                    failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                                            .message("Task not found (may have expired after TTL)")
                                            .data("Task ID: " + taskId)
                                            .build());
                                    return failed;
                                }
                                return CompletableFuture.completedFuture(
                                        (McpSchema.Result) McpSchema.GetTaskResult.fromTask(storeResult.task()));
                            });
                });
    }

    private CompletableFuture<McpSchema.Result> handleGetTaskResult(String requestMethod, Object requestParams,
                                                                      TaskManagerHost.TaskHandlerContext ctx) {
        if (this.taskStore == null) {
            CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
            failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("TaskStore not configured")
                    .build());
            return failed;
        }

        String taskId = extractTaskIdFromParams(requestParams);
        if (taskId == null) {
            CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
            failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                    .message("Missing required parameter: taskId")
                    .build());
            return failed;
        }

        String sessionId = ctx.sessionId();

        McpSchema.GetTaskPayloadRequest typedRequest = new McpSchema.GetTaskPayloadRequest(taskId, null);

        return this.taskStore.getTask(taskId, sessionId)
                .thenCompose(storeResult -> {
                    if (storeResult == null) {
                        CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
                        failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                                .message("Task not found (may have expired after TTL)")
                                .data("Task ID: " + taskId)
                                .build());
                        return failed;
                    }

                    McpSchema.Task task = storeResult.task();

                    logger.debug("handleGetTaskResult: Task {} status={}, messageQueue={}",
                            taskId, task.getStatus(),
                            this.messageQueue != null ? "present" : "null");

                    // Handle INPUT_REQUIRED: process queued side-channel messages first
                    if (task.getStatus() == McpSchema.TaskStatus.INPUT_REQUIRED && this.messageQueue != null) {
                        logger.debug("handleGetTaskResult: Task {} is INPUT_REQUIRED, starting side-channel processing",
                                taskId);
                        return processQueuedMessagesAndWaitForTerminal(ctx, taskId, sessionId)
                                .thenCompose(sideChannelResult -> {
                                    return tryCustomHandlerOrDefault(taskId, typedRequest, ctx, sessionId);
                                });
                    }

                    return tryCustomHandlerOrDefault(taskId, typedRequest, ctx, sessionId);
                });
    }

    /** Tries the custom tasks/result handler, falling back to default store lookup. */
    private CompletableFuture<McpSchema.Result> tryCustomHandlerOrDefault(
            String taskId, McpSchema.GetTaskPayloadRequest typedRequest,
            TaskManagerHost.TaskHandlerContext ctx, String sessionId) {

        return host.invokeCustomTaskHandler(taskId, McpSchema.METHOD_TASKS_RESULT, typedRequest, ctx,
                        McpSchema.ServerTaskPayloadResult.class)
                .thenCompose(result -> {
                    if (result != null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    return defaultGetTaskResult(taskId, sessionId);
                });
    }

    /** Default tasks/result implementation using the TaskStore. */
    private CompletableFuture<McpSchema.Result> defaultGetTaskResult(String taskId, String sessionId) {
        return this.taskStore.getTask(taskId, sessionId)
                .thenCompose(storeResult -> {
                    if (storeResult == null) {
                        CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
                        failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                                .message("Task not found")
                                .build());
                        return failed;
                    }

                    McpSchema.Task task = storeResult.task();

                    if (task.isTerminal()) {
                        logger.debug("defaultGetTaskResult: Task {} is terminal, fetching result", taskId);
                        return fetchTaskResult(taskId, sessionId);
                    }

                    return watchAndFetchResult(taskId, sessionId);
                });
    }

    /** Fetches the result of a terminal task. */
    @SuppressWarnings("unchecked")
    private CompletableFuture<McpSchema.Result> fetchTaskResult(String taskId, String sessionId) {
        // Re-fetch the task to get its current status for fallback construction.
        return this.taskStore.getTask(taskId, sessionId).thenCompose(storeResult -> {
            final McpSchema.Task task = storeResult != null ? storeResult.task() : null;
            TaskStore<McpSchema.Result> store = (TaskStore<McpSchema.Result>) this.taskStore;
            return store.getTaskResult(taskId, sessionId)
                    .thenApply(result -> {
                        if (result != null) {
                            return result;
                        }
                        // CANCELLED tasks never store a payload — construct a semantic response.
                        if (task != null && task.getStatus() == McpSchema.TaskStatus.CANCELLED) {
                            String msg = "Task was cancelled" +
                                    (task.getStatusMessage() != null ? ": " + task.getStatusMessage() : "");
                            return (McpSchema.Result) new McpSchema.CallToolResult(msg, true, null);
                        }
                        // Should not reach here for FAILED tasks (payload stored by failTask).
                        throw new RuntimeException("Task result not found");
                    });
        });
    }

    /** Watches a task until terminal, then fetches its result. */
    private CompletableFuture<McpSchema.Result> watchAndFetchResult(String taskId, String sessionId) {
        long timeoutMs = this.pollTimeout.toMillis();
        return this.taskStore.watchTaskUntilTerminal(taskId, sessionId, timeoutMs)
                .thenCompose(updates -> {
                    if (updates == null || updates.isEmpty()) {
                        CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
                        failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                .message("Task did not complete within timeout")
                                .data("Task ID: " + taskId)
                                .build());
                        return failed;
                    }
                    McpSchema.Task terminalTask = updates.get(updates.size() - 1);
                    if (!terminalTask.isTerminal()) {
                        CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
                        failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                .message("Task did not complete within timeout")
                                .data("Task ID: " + taskId)
                                .build());
                        return failed;
                    }
                    return fetchTaskResult(taskId, sessionId);
                })
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
                        throw new RuntimeException(
                                McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                        .message("Task did not complete within timeout")
                                        .data("Task ID: " + taskId)
                                        .build());
                    }
                    throw new RuntimeException(ex);
                });
    }

    /**
     * Processes all queued side-channel messages for an INPUT_REQUIRED task, then waits for terminal state.
     */
    private CompletableFuture<McpSchema.Result> processQueuedMessagesAndWaitForTerminal(
            TaskManagerHost.TaskHandlerContext ctx, String taskId, String sessionId) {
        logger.debug("processQueuedMessagesAndWaitForTerminal: Starting side-channel processing for task {}", taskId);

        return processAllQueuedMessages(ctx, taskId)
                .thenCompose(v -> {
                    logger.debug("processQueuedMessagesAndWaitForTerminal: Finished processing queue for task {}",
                            taskId);
                    return pollAndProcessUntilTerminal(ctx, taskId, sessionId);
                });
    }

    /** Dequeues and processes all actionable messages for a task. */
    private CompletableFuture<Void> processAllQueuedMessages(TaskManagerHost.TaskHandlerContext ctx, String taskId) {
        return this.messageQueue.dequeueAll(taskId)
                .thenCompose(messages -> {
                    CompletableFuture<Void> allProcessed = CompletableFuture.completedFuture(null);
                    for (QueuedMessage msg : messages) {
                        allProcessed = allProcessed.thenCompose(v -> processMessage(ctx, msg, taskId));
                    }
                    return allProcessed;
                });
    }

    /** Dispatches a single queued message to the client. */
    private CompletableFuture<Void> processMessage(TaskManagerHost.TaskHandlerContext ctx, QueuedMessage msg,
                                                     String taskId) {
        if (msg instanceof QueuedMessage.Request) {
            QueuedMessage.Request req = (QueuedMessage.Request) msg;
            return sendRequestAndEnqueueResponse(ctx, req, taskId);
        }

        if (msg instanceof QueuedMessage.Notification) {
            QueuedMessage.Notification notif = (QueuedMessage.Notification) msg;
            return sendNotificationToClient(ctx, notif, taskId);
        }

        return CompletableFuture.completedFuture(null);
    }

    /** Sends a request to the client and enqueues the response for waitForResponse() to retrieve. */
    private CompletableFuture<Void> sendRequestAndEnqueueResponse(TaskManagerHost.TaskHandlerContext ctx,
                                                                    QueuedMessage.Request req, String taskId) {
        String requestId = String.valueOf(req.requestId());

        logger.debug("sendRequestAndEnqueueResponse: Sending {} request {} to client for task {}",
                req.method(), requestId, taskId);

        Class<? extends McpSchema.Result> resultClass = getResultClass(req.method());

        return ctx.sendRequest(req.method(), req.request(), resultClass)
                .thenCompose(result -> {
                    logger.debug("sendRequestAndEnqueueResponse: Got response for request {}, enqueueing for task {}",
                            requestId, taskId);
                    QueuedMessage.Response response = new QueuedMessage.Response(requestId, result);
                    return this.messageQueue.enqueue(taskId, response);
                });
    }

    /** Returns the result class for a known side-channel method. */
    private Class<? extends McpSchema.Result> getResultClass(String method) {
        if (McpSchema.METHOD_ELICITATION_CREATE.equals(method)) {
            return McpSchema.ElicitResult.class;
        } else if (McpSchema.METHOD_SAMPLING_CREATE_MESSAGE.equals(method)) {
            return McpSchema.CreateMessageResult.class;
        } else {
            throw new IllegalArgumentException("Unsupported side-channel method: " + method);
        }
    }

    /** Sends a notification to the client without waiting for a response. */
    private CompletableFuture<Void> sendNotificationToClient(TaskManagerHost.TaskHandlerContext ctx,
                                                              QueuedMessage.Notification notif, String taskId) {
        Object notification = TaskMetadataUtils.addRelatedTaskMetadata(taskId, notif.notification());
        return ctx.sendNotification(notif.method(), notification);
    }

    /** Polls and processes messages until the task reaches a terminal state. */
    private CompletableFuture<McpSchema.Result> pollAndProcessUntilTerminal(
            TaskManagerHost.TaskHandlerContext ctx, String taskId, String sessionId) {

        CompletableFuture<McpSchema.Result> pollingFuture = doPollAndProcess(ctx, taskId, sessionId);

        CompletableFuture<McpSchema.Result> timeoutFuture = delay(this.pollTimeout.toMillis())
                .thenApply(v -> {
                    throw new java.util.concurrent.CompletionException(
                            McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                    .message("Task did not complete within timeout")
                                    .data("Task ID: " + taskId)
                                    .build());
                });

        return CompletableFuture.anyOf(pollingFuture, timeoutFuture)
                .thenApply(obj -> (McpSchema.Result) obj);
    }

    /** Recursive poll-and-process loop. */
    private CompletableFuture<McpSchema.Result> doPollAndProcess(
            TaskManagerHost.TaskHandlerContext ctx, String taskId, String sessionId) {
        return this.taskStore.getTask(taskId, sessionId)
                .thenCompose(storeResult -> {
                    if (storeResult == null) {
                        CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
                        failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                                .message("Task not found during polling")
                                .data("Task ID: " + taskId)
                                .build());
                        return failed;
                    }

                    McpSchema.Task task = storeResult.task();

                    if (task.isTerminal()) {
                        return fetchTaskResult(taskId, sessionId);
                    }

                    long interval = task.getPollInterval() != null
                            ? task.getPollInterval()
                            : this.defaultPollInterval.toMillis();

                    if (task.getStatus() == McpSchema.TaskStatus.INPUT_REQUIRED) {
                        return processAllQueuedMessages(ctx, taskId)
                                .thenCompose(v -> delay(interval))
                                .thenCompose(ignored -> doPollAndProcess(ctx, taskId, sessionId));
                    }

                    return delay(interval)
                            .thenCompose(ignored -> doPollAndProcess(ctx, taskId, sessionId));
                });
    }

    private CompletableFuture<McpSchema.Result> handleListTasks(String requestMethod, Object requestParams,
                                                                 TaskManagerHost.TaskHandlerContext ctx) {
        if (this.taskStore == null) {
            CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
            failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("TaskStore not configured")
                    .build());
            return failed;
        }

        String cursor = extractCursorFromParams(requestParams);

        return this.taskStore.listTasks(cursor, ctx.sessionId())
                .thenApply(result -> (McpSchema.Result) result);
    }

    private CompletableFuture<McpSchema.Result> handleCancelTask(String requestMethod, Object requestParams,
                                                                  TaskManagerHost.TaskHandlerContext ctx) {
        if (this.taskStore == null) {
            CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
            failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("TaskStore not configured")
                    .build());
            return failed;
        }

        String taskId = extractTaskIdFromParams(requestParams);
        if (taskId == null) {
            CompletableFuture<McpSchema.Result> failed = new CompletableFuture<>();
            failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                    .message("Missing required parameter: taskId")
                    .build());
            return failed;
        }

        return this.taskStore.requestCancellation(taskId, ctx.sessionId())
                .thenApply(task -> {
                    if (task == null) {
                        throw new CompletionException(
                                McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                                        .message("Task not found or not accessible")
                                        .data("Task ID: " + taskId)
                                        .build());
                    }
                    return (McpSchema.Result) McpSchema.CancelTaskResult.fromTask(task);
                });
    }

    private String extractTaskIdFromParams(Object params) {
        return extractStringFromParams(params, "taskId");
    }

    private String extractCursorFromParams(Object params) {
        return extractStringFromParams(params, "cursor");
    }

    @SuppressWarnings("unchecked")
    private String extractStringFromParams(Object params, String key) {
        if (params == null) {
            return null;
        }
        try {
            if (params instanceof Map) {
                Map<String, Object> paramsMap = (Map<String, Object>) params;
                return (String) paramsMap.get(key);
            }
        } catch (ClassCastException e) {
            logger.debug("Failed to extract {} from params: {}", key, e.getMessage());
        }
        return null;
    }

    /** Shared scheduler to avoid creating a new thread pool per delay() call. */
    private static final java.util.concurrent.ScheduledExecutorService DELAY_SCHEDULER =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "task-manager-delay");
                t.setDaemon(true);
                return t;
            });

    /** Delays for the given number of milliseconds. */
    private CompletableFuture<Void> delay(long milliseconds) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DELAY_SCHEDULER.schedule(() -> future.complete(null), milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
        return future;
    }
}

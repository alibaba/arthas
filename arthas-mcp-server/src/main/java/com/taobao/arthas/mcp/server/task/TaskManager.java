/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Orchestrates task state, message queuing, polling, and handler registration.
 *
 * <p>Interacts with the protocol layer via five lifecycle methods:
 * {@link #processInboundRequest}, {@link #processOutboundRequest},
 * {@link #processInboundResponse}, {@link #processOutboundNotification}, {@link #onClose}.
 * Must be bound to a {@link TaskManagerHost} via {@link #bind} before use.
 *
 * @author Yeaury
 * @see DefaultTaskManager
 * @see NullTaskManager
 */
public interface TaskManager {

    void bind(TaskManagerHost host);

    InboundRequestResult processInboundRequest(String requestMethod, Object requestParams,
                                                InboundRequestContext ctx);

    OutboundRequestResult processOutboundRequest(String requestMethod, Object requestParams,
                                                  RequestOptions options, Object messageId,
                                                  Consumer<Object> responseHandler,
                                                  Consumer<Throwable> errorHandler);

    InboundResponseResult processInboundResponse(Object responseResult, Object messageId);

    CompletableFuture<OutboundNotificationResult> processOutboundNotification(
            String notificationMethod, Object notification, NotificationOptions options);

    void onClose();

    Optional<TaskStore<?>> taskStore();

    Optional<TaskMessageQueue> messageQueue();

    Duration defaultPollInterval();

    // === Supporting types ===

    class InboundRequestContext {
        private final String sessionId;
        private final NotificationSender sendNotification;
        private final RequestSender sendRequest;

        public InboundRequestContext(String sessionId,
                                     NotificationSender sendNotification,
                                     RequestSender sendRequest) {
            this.sessionId = sessionId;
            this.sendNotification = sendNotification;
            this.sendRequest = sendRequest;
        }

        public String sessionId() {
            return sessionId;
        }

        public NotificationSender sendNotification() {
            return sendNotification;
        }

        public RequestSender sendRequest() {
            return sendRequest;
        }
    }

    @FunctionalInterface
    interface NotificationSender {
        CompletableFuture<Void> send(Object notification, NotificationOptions options);
    }

    @FunctionalInterface
    interface RequestSender {
        <T> CompletableFuture<T> send(Object request, Class<T> resultType, RequestOptions options);
    }

    class InboundRequestResult {
        private final Consumer<Object> sendNotification;
        private final RequestSender sendRequest;
        private final java.util.function.Function<Object, CompletableFuture<Boolean>> routeResponse;
        private final boolean hasTaskCreationParams;

        public InboundRequestResult(Consumer<Object> sendNotification,
                                     RequestSender sendRequest,
                                     java.util.function.Function<Object, CompletableFuture<Boolean>> routeResponse,
                                     boolean hasTaskCreationParams) {
            this.sendNotification = sendNotification;
            this.sendRequest = sendRequest;
            this.routeResponse = routeResponse;
            this.hasTaskCreationParams = hasTaskCreationParams;
        }

        public Consumer<Object> sendNotification() {
            return sendNotification;
        }

        public RequestSender sendRequest() {
            return sendRequest;
        }

        public java.util.function.Function<Object, CompletableFuture<Boolean>> routeResponse() {
            return routeResponse;
        }

        public boolean hasTaskCreationParams() {
            return hasTaskCreationParams;
        }
    }

    class OutboundRequestResult {
        private final boolean queued;

        public OutboundRequestResult(boolean queued) {
            this.queued = queued;
        }

        public boolean queued() {
            return queued;
        }
    }

    class InboundResponseResult {
        private final boolean consumed;

        public InboundResponseResult(boolean consumed) {
            this.consumed = consumed;
        }

        public boolean consumed() {
            return consumed;
        }
    }

    class OutboundNotificationResult {
        private final boolean queued;
        private final Object jsonrpcNotification;

        public OutboundNotificationResult(boolean queued, Object jsonrpcNotification) {
            this.queued = queued;
            this.jsonrpcNotification = jsonrpcNotification;
        }

        public OutboundNotificationResult(boolean queued) {
            this(queued, null);
        }

        public boolean queued() {
            return queued;
        }

        public Object jsonrpcNotification() {
            return jsonrpcNotification;
        }
    }

    class RequestOptions {
        private final TaskCreationParams task;
        private final RelatedTaskInfo relatedTask;

        public RequestOptions(TaskCreationParams task, RelatedTaskInfo relatedTask) {
            this.task = task;
            this.relatedTask = relatedTask;
        }

        public static RequestOptions empty() {
            return new RequestOptions(null, null);
        }

        public TaskCreationParams task() {
            return task;
        }

        public RelatedTaskInfo relatedTask() {
            return relatedTask;
        }
    }

    class NotificationOptions {
        private final RelatedTaskInfo relatedTask;

        public NotificationOptions(RelatedTaskInfo relatedTask) {
            this.relatedTask = relatedTask;
        }

        public static NotificationOptions empty() {
            return new NotificationOptions(null);
        }
        public static NotificationOptions withRelatedTask(RelatedTaskInfo relatedTask) {
            return new NotificationOptions(relatedTask);
        }

        public RelatedTaskInfo relatedTask() {
            return relatedTask;
        }
    }

    class TaskCreationParams {
        private final Long ttl;

        public TaskCreationParams(Long ttl) {
            this.ttl = ttl;
        }

        public Long ttl() {
            return ttl;
        }
    }

    class RelatedTaskInfo {
        private final String taskId;

        public RelatedTaskInfo(String taskId) {
            this.taskId = taskId;
        }

        public String taskId() {
            return taskId;
        }
    }
}

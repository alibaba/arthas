/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * No-op {@link TaskManager} used when task support is not configured.
 *
 * @author Yeaury
 */
final class NullTaskManager implements TaskManager {

    private static final NullTaskManager INSTANCE = new NullTaskManager();

    private NullTaskManager() {}

    static TaskManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void bind(TaskManagerHost host) {
    }

    @Override
    public InboundRequestResult processInboundRequest(String requestMethod, Object requestParams,
                                                       InboundRequestContext ctx) {
        return new InboundRequestResult(
                notification -> {},
                ctx.sendRequest(),
                response -> CompletableFuture.completedFuture(false),
                false
        );
    }

    @Override
    public OutboundRequestResult processOutboundRequest(String requestMethod, Object requestParams,
                                                         RequestOptions options, Object messageId,
                                                         Consumer<Object> responseHandler,
                                                         Consumer<Throwable> errorHandler) {
        return new OutboundRequestResult(false);
    }

    @Override
    public InboundResponseResult processInboundResponse(Object responseResult, Object messageId) {
        return new InboundResponseResult(false);
    }

    @Override
    public CompletableFuture<OutboundNotificationResult> processOutboundNotification(
            String notificationMethod, Object notification, NotificationOptions options) {
        return CompletableFuture.completedFuture(new OutboundNotificationResult(false));
    }

    @Override
    public void onClose() {
    }

    @Override
    public Optional<TaskStore<?>> taskStore() {
        return Optional.empty();
    }

    @Override
    public Optional<TaskMessageQueue> messageQueue() {
        return Optional.empty();
    }

    @Override
    public Duration defaultPollInterval() {
        return Duration.ofMillis(TaskDefaults.DEFAULT_POLL_INTERVAL_MS);
    }
}

/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Queue for side-channel messages during task execution.
 *
 * <p>Supports three message types: Request (server→client, needs response),
 * Notification (async, no response), and Response (client reply, retrieved via
 * {@link #waitForResponse} only).
 *
 * @author Yeaury
 */
public interface TaskMessageQueue {

    /** Enqueue a message (Request, Response, or Notification). */
    CompletableFuture<Void> enqueue(String taskId, QueuedMessage message);

    /** Dequeue the next actionable message (Request or Notification); returns null if empty. */
    CompletableFuture<QueuedMessage> dequeue(String taskId);

    /** Dequeue all actionable messages (Request and Notification). */
    CompletableFuture<List<QueuedMessage>> dequeueAll(String taskId);

    /** Block until a Response matching {@code requestId} is enqueued, or timeout. */
    CompletableFuture<QueuedMessage.Response> waitForResponse(String taskId, Object requestId, Duration timeout);

    /** Remove all messages for a task (called on task expiry/cleanup). */
    CompletableFuture<Void> clearTask(String taskId);

    default CompletableFuture<Integer> getQueueSize(String taskId) {
        return CompletableFuture.completedFuture(0);
    }

    CompletableFuture<Void> shutdown();
}

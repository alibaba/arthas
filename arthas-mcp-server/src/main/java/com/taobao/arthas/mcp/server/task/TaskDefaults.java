/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.time.Duration;

/**
 * Default configuration constants for the task system.
 *
 * @author Yeaury
 */
public final class TaskDefaults {

    public static final long DEFAULT_TTL_MS = 60_000L;

    public static final long DEFAULT_POLL_INTERVAL_MS = 1000L;

    public static final int DEFAULT_PAGE_SIZE = 100;

    public static final int DEFAULT_MAX_TASKS = 10_000;

    public static final long DEFAULT_AUTOMATIC_POLLING_TIMEOUT_MS = 600000L;

    public static final int DEFAULT_SIDE_CHANNEL_TIMEOUT_MINUTES = 5;

    public static final long MAX_TTL_MS = 24 * 60 * 60 * 1000L;

    public static final long MIN_POLL_INTERVAL_MS = 100L;

    public static final long MAX_POLL_INTERVAL_MS = 60 * 60 * 1000L;

    public static final long CLEANUP_INTERVAL_MINUTES = 1L;

    public static final long MESSAGE_QUEUE_CLEANUP_TIMEOUT_MS = 1_000L;

    public static final long RESPONSE_POLL_INTERVAL_MS = 50L;

    public static final long TASK_STORE_SHUTDOWN_TIMEOUT_SECONDS = 5L;

    public static final int DEFAULT_MAX_POLL_ATTEMPTS = 60;

    public static final long MAX_TIMEOUT_MS = 3_600_000L;

    public static final int MAX_WATCH_UPDATES = 100;

    public static final McpSchema.JsonSchema EMPTY_INPUT_SCHEMA =
            new McpSchema.JsonSchema("object", null, null, null);

    /**
     * Calculates a polling timeout scaled to the given poll interval, capped at {@link #MAX_TIMEOUT_MS}.
     */
    public static Duration calculateTimeout(Long pollInterval) {
        long interval = pollInterval != null ? pollInterval : DEFAULT_POLL_INTERVAL_MS;
        long calculatedMs = interval * DEFAULT_MAX_POLL_ATTEMPTS;
        return Duration.ofMillis(Math.min(calculatedMs, MAX_TIMEOUT_MS));
    }

    /**
     * Validates that task-aware tools are not registered without a TaskStore.
     */
    public static void validateTaskConfiguration(boolean hasTaskTools, boolean hasTaskStore) {
        if (hasTaskTools && !hasTaskStore) {
            throw new IllegalStateException(
                    "Task-aware tools registered but no TaskStore configured. " +
                    "Add a TaskStore via .taskStore(store) or remove task tools.");
        }
    }

    private TaskDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }
}

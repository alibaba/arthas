/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTaskStoreTest {

    @Test
    void cleanupExpiredNonTerminalTaskCancellationSignalAfterRetention() throws Exception {
        InMemoryTaskStore<McpSchema.CallToolResult> store = new InMemoryTaskStore<>(
                1L, 1000L, null, 100, 1L);
        String taskId = "expired-working-task";
        String sessionId = "session-1";

        try {
            store.createTask(CreateTaskOptions.builder()
                    .taskId(taskId)
                    .sessionId(sessionId)
                    .ttl(1L)
                    .build()).join();

            Thread.sleep(20L);
            store.cleanupExpiredTasks();

            assertThat(store.getTask(taskId, sessionId).join()).isNull();
            assertThat(store.isCancellationRequested(taskId, sessionId).join()).isTrue();

            Thread.sleep(20L);
            store.cleanupExpiredTasks();

            assertThat(store.isCancellationRequested(taskId, sessionId).join()).isFalse();
        } finally {
            store.shutdown().join();
        }
    }

    @Test
    void cleanupExpiredCancelledTaskRetainsCancellationSignalAfterTtl() throws Exception {
        InMemoryTaskStore<McpSchema.CallToolResult> store = new InMemoryTaskStore<>(
                1L, 1000L, null, 100, 1L);
        String taskId = "expired-cancelled-task";
        String sessionId = "session-1";

        try {
            store.createTask(CreateTaskOptions.builder()
                    .taskId(taskId)
                    .sessionId(sessionId)
                    .ttl(1L)
                    .build()).join();
            store.requestCancellation(taskId, sessionId).join();

            Thread.sleep(20L);
            store.cleanupExpiredTasks();

            assertThat(store.getTask(taskId, sessionId).join()).isNull();
            assertThat(store.isCancellationRequested(taskId, sessionId).join()).isTrue();

            Thread.sleep(20L);
            store.cleanupExpiredTasks();

            assertThat(store.isCancellationRequested(taskId, sessionId).join()).isFalse();
        } finally {
            store.shutdown().join();
        }
    }
}

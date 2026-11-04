/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ServerTaskToolHandlerTest {

    @Test
    void automaticPollingTimeoutShouldAddGraceWhenTimeoutArgumentIsMissing() {
        ServerTaskToolHandler handler = newHandler(Duration.ofSeconds(30));

        Duration timeout = handler.resolveAutomaticPollingTimeout(
                new McpSchema.CallToolRequest("watch", null, null),
                Duration.ofSeconds(30));

        assertThat(timeout).isEqualTo(Duration.ofSeconds(40));
    }

    @Test
    void automaticPollingTimeoutShouldCoverLongCommandTimeout() {
        ServerTaskToolHandler handler = newHandler(Duration.ofSeconds(30));

        Duration timeout = handler.resolveAutomaticPollingTimeout(
                requestWithTimeout(45),
                Duration.ofSeconds(30));

        assertThat(timeout).isEqualTo(Duration.ofSeconds(55));
    }

    @Test
    void automaticPollingTimeoutShouldNotShrinkBelowDefault() {
        ServerTaskToolHandler handler = newHandler(Duration.ofSeconds(30));

        Duration timeout = handler.resolveAutomaticPollingTimeout(
                requestWithTimeout(5),
                Duration.ofSeconds(30));

        assertThat(timeout).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void automaticPollingTimeoutShouldParseNumericStringTimeout() {
        ServerTaskToolHandler handler = newHandler(Duration.ofSeconds(30));

        Duration timeout = handler.resolveAutomaticPollingTimeout(
                requestWithTimeout("45"),
                Duration.ofSeconds(30));

        assertThat(timeout).isEqualTo(Duration.ofSeconds(55));
    }

    @Test
    void automaticPollingTimeoutShouldFallbackWithGraceForInvalidTimeoutArguments() {
        ServerTaskToolHandler handler = newHandler(Duration.ofSeconds(30));
        Object[] invalidValues = new Object[] { "bad", "0", "-1", 0, -1, new Object() };

        for (Object invalidValue : invalidValues) {
            Duration timeout = handler.resolveAutomaticPollingTimeout(
                    requestWithTimeout(invalidValue),
                    Duration.ofSeconds(30));

            assertThat(timeout).isEqualTo(Duration.ofSeconds(40));
        }
    }

    @Test
    void automaticPollingTimeoutShouldFallbackToTaskDefaultWhenDefaultIsNull() {
        ServerTaskToolHandler handler = newHandler(null);

        Duration timeout = handler.resolveAutomaticPollingTimeout(
                new McpSchema.CallToolRequest("watch", null, null),
                null);

        assertThat(timeout).isEqualTo(Duration.ofMillis(TaskDefaults.DEFAULT_AUTOMATIC_POLLING_TIMEOUT_MS)
                .plus(Duration.ofSeconds(10)));
    }

    private static ServerTaskToolHandler newHandler(Duration automaticPollingTimeout) {
        return new ServerTaskToolHandler(
                Collections.emptyList(),
                null,
                new ObjectMapper(),
                (method, payload) -> CompletableFuture.completedFuture(null),
                automaticPollingTimeout,
                null);
    }

    private static McpSchema.CallToolRequest requestWithTimeout(Object timeout) {
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("timeout", timeout);
        return new McpSchema.CallToolRequest("watch", arguments, null);
    }
}

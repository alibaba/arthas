/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

/**
 * Result of a task lookup from {@link TaskStore}, including the originating request.
 *
 * @author Yeaury
 */
public class GetTaskFromStoreResult {

    private final McpSchema.Task task;
    private final McpSchema.Request originatingRequest;

    public GetTaskFromStoreResult(McpSchema.Task task, McpSchema.Request originatingRequest) {
        this.task = task;
        this.originatingRequest = originatingRequest;
    }

    public McpSchema.Task task() {
        return task;
    }

    public McpSchema.Request originatingRequest() {
        return originatingRequest;
    }
}

/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

/**
 * Options for creating a task.
 *
 * @author Yeaury
 */
public class CreateTaskOptions {

    private final String sessionId;
    private final String taskId;
    private final Long requestedTtl;
    private final Long pollInterval;
    private final McpSchema.Request originatingRequest;
    private final Object context;

    private CreateTaskOptions(Builder builder) {
        this.sessionId = builder.sessionId;
        this.taskId = builder.taskId;
        this.requestedTtl = builder.requestedTtl;
        this.pollInterval = builder.pollInterval;
        this.originatingRequest = builder.originatingRequest;
        this.context = builder.context;
    }

    public String sessionId() {
        return sessionId;
    }

    public String taskId() {
        return taskId;
    }

    public Long requestedTtl() {
        return requestedTtl;
    }

    public Long pollInterval() {
        return pollInterval;
    }

    public McpSchema.Request originatingRequest() {
        return originatingRequest;
    }

    public Object context() {
        return context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sessionId;
        private String taskId;
        private Long requestedTtl;
        private Long pollInterval;
        private McpSchema.Request originatingRequest;
        private Object context;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder ttl(Long ttl) {
            this.requestedTtl = ttl;
            return this;
        }

        public Builder pollInterval(Long pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        public Builder originatingRequest(McpSchema.Request request) {
            this.originatingRequest = request;
            return this;
        }

        public Builder context(Object context) {
            this.context = context;
            return this;
        }

        public CreateTaskOptions build() {
            return new CreateTaskOptions(this);
        }
    }
}

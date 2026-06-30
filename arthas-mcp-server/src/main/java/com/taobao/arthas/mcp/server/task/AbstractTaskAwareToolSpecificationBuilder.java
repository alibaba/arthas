/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

/**
 * Abstract base for task-aware tool specification builders with self-referencing generics.
 *
 * @param <T> the concrete builder type
 * @author Yeaury
 */
public abstract class AbstractTaskAwareToolSpecificationBuilder<T extends AbstractTaskAwareToolSpecificationBuilder<T>> {

    protected String name;
    protected String description;
    protected McpSchema.JsonSchema inputSchema;
    protected McpSchema.TaskSupportMode taskSupport = McpSchema.TaskSupportMode.OPTIONAL;

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T name(String name) {
        this.name = name;
        return self();
    }

    public T description(String description) {
        this.description = description;
        return self();
    }

    public T inputSchema(McpSchema.JsonSchema schema) {
        this.inputSchema = schema;
        return self();
    }

    public T taskSupport(McpSchema.TaskSupportMode mode) {
        this.taskSupport = mode;
        return self();
    }

    public T taskSupport(String mode) {
        if ("optional".equalsIgnoreCase(mode)) {
            this.taskSupport = McpSchema.TaskSupportMode.OPTIONAL;
        } else if ("required".equalsIgnoreCase(mode)) {
            this.taskSupport = McpSchema.TaskSupportMode.REQUIRED;
        } else if ("forbidden".equalsIgnoreCase(mode)) {
            this.taskSupport = McpSchema.TaskSupportMode.FORBIDDEN;
        } else {
            throw new IllegalArgumentException("Invalid taskSupport mode: " + mode);
        }
        return self();
    }

    protected void validateCommonFields() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name must not be null or empty");
        }
        if (inputSchema == null) {
            throw new IllegalArgumentException("Input schema must not be null");
        }
    }

    protected McpSchema.Tool buildTool() {
        return McpSchema.Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchema)
            .execution(new McpSchema.ToolExecution(taskSupport))
            .build();
    }
}

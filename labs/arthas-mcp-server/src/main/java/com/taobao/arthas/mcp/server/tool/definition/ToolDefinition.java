package com.taobao.arthas.mcp.server.tool.definition;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.Map;

public class ToolDefinition {
    private String name;

    private String description;

    private McpSchema.JsonSchema inputSchema;

    private Map<String, Object> outputSchema;

    private boolean streamable;

    public ToolDefinition(String name, String description,
                          McpSchema.JsonSchema inputSchema, Map<String, Object> outputSchema, boolean streamable) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.streamable = streamable;
    }
    
    // Backwards compatibility constructor
    public ToolDefinition(String name, String description,
                          McpSchema.JsonSchema inputSchema, boolean streamable) {
        this(name, description, inputSchema, null, streamable);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public McpSchema.JsonSchema getInputSchema() {
        return inputSchema;
    }
    
    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    public boolean isStreamable() {
        return streamable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String name;

        private String description;

        private McpSchema.JsonSchema inputSchema;

        private Map<String, Object> outputSchema;

        private boolean streamable;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder inputSchema(McpSchema.JsonSchema inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        public Builder outputSchema(Map<String, Object> outputSchema) {
            this.outputSchema = outputSchema;
            return this;
        }

        public Builder streamable(boolean streamable) {
            this.streamable = streamable;
            return this;
        }

        public ToolDefinition build() {
            return new ToolDefinition(this.name, this.description, this.inputSchema, this.outputSchema, this.streamable);
        }

    }
}

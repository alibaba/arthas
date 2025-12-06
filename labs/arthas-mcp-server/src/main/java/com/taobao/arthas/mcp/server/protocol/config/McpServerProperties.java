package com.taobao.arthas.mcp.server.protocol.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP Server Configuration Properties
 * Used to manage all configuration items for MCP server.
 *
 * @author Yeaury
 */
public class McpServerProperties {

    /**
     * Server basic information
     */
    private final String name;
    private final String version;
    private final String instructions;

    /**
     * Server capability configuration
     */
    private final boolean toolChangeNotification;
    private final boolean resourceChangeNotification;
    private final boolean promptChangeNotification;
    private final boolean resourceSubscribe;

    private final String mcpEndpoint;

    /**
     * Timeout configuration
     */
    private final Duration requestTimeout;
    private final Duration initializationTimeout;

    private final ObjectMapper objectMapper;

    private final ServerProtocol protocol;

    /**
     * (Optional) response MIME type per tool name.
     */
    private Map<String, String> toolResponseMimeType = new HashMap<>();

    /**
     * Private constructor, can only be created through Builder
     */
    private McpServerProperties(Builder builder) {
        this.name = builder.name;
        this.version = builder.version;
        this.instructions = builder.instructions;
        this.toolChangeNotification = builder.toolChangeNotification;
        this.resourceChangeNotification = builder.resourceChangeNotification;
        this.promptChangeNotification = builder.promptChangeNotification;
        this.resourceSubscribe = builder.resourceSubscribe;
        this.mcpEndpoint = builder.mcpEndpoint;
        this.requestTimeout = builder.requestTimeout;
        this.initializationTimeout = builder.initializationTimeout;
        this.objectMapper = builder.objectMapper;
        this.protocol = builder.protocol;
    }

    /**
     * Create Builder with default configuration
     */
    public static Builder builder() {
        return new Builder();
    }

    public enum ServerProtocol {
        STREAMABLE, STATELESS
    }

    /**
     * Get server name
     * @return Server name
     */
    public String getName() {
        return name;
    }

    /**
     * Get server version
     * @return Server version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get server instructions
     * @return Server instructions
     */
    public String getInstructions() {
        return instructions;
    }

    /**
     * Get tool change notification
     * @return Tool change notification
     */
    public boolean isToolChangeNotification() {
        return toolChangeNotification;
    }

    /**
     * Get resource change notification
     * @return Resource change notification
     */
    public boolean isResourceChangeNotification() {
        return resourceChangeNotification;
    }

    /**
     * Get prompt change notification
     * @return Prompt change notification
     */
    public boolean isPromptChangeNotification() {
        return promptChangeNotification;
    }

    /**
     * Get resource subscribe
     * @return Resource subscribe
     */
    public boolean isResourceSubscribe() {
        return resourceSubscribe;
    }

    /**
     * Get SSE endpoint
     * @return SSE endpoint
     */
    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    /**
     * Get request timeout
     * @return Request timeout
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Get initialization timeout
     * @return Initialization timeout
     */
    public Duration getInitializationTimeout() {
        return initializationTimeout;
    }

    /**
     * Get object mapper
     * @return Object mapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public ServerProtocol getProtocol() {
        return protocol;
    }

    public Map<String, String> getToolResponseMimeType() {
        return toolResponseMimeType;
    }

    public void setToolResponseMimeType(Map<String, String> toolResponseMimeType) {
        this.toolResponseMimeType = toolResponseMimeType;
    }

    /**
     * Builder class for McpServerProperties
     */
    public static class Builder {
        // Default values
        private String name = "mcp-server";
        private String version = "1.0.0";
        private String instructions;
        private boolean toolChangeNotification = true;
        private boolean resourceChangeNotification = false;
        private boolean promptChangeNotification = false;
        private boolean resourceSubscribe = false;
        private String bindAddress = "localhost";
        private int port = 8080;
        private String mcpEndpoint = "/mcp";
        private Duration requestTimeout = Duration.ofSeconds(10);
        private Duration initializationTimeout = Duration.ofSeconds(30);
        private ObjectMapper objectMapper;
        private ServerProtocol protocol = ServerProtocol.STATELESS;

        public Builder() {
            // Private constructor to prevent direct instantiation
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder toolChangeNotification(boolean toolChangeNotification) {
            this.toolChangeNotification = toolChangeNotification;
            return this;
        }

        public Builder resourceChangeNotification(boolean resourceChangeNotification) {
            this.resourceChangeNotification = resourceChangeNotification;
            return this;
        }

        public Builder promptChangeNotification(boolean promptChangeNotification) {
            this.promptChangeNotification = promptChangeNotification;
            return this;
        }

        public Builder resourceSubscribe(boolean resourceSubscribe) {
            this.resourceSubscribe = resourceSubscribe;
            return this;
        }

        public Builder mcpEndpoint(String mcpEndpoint) {
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder initializationTimeout(Duration initializationTimeout) {
            this.initializationTimeout = initializationTimeout;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder protocol(ServerProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Build McpServerProperties instance
         */
        public McpServerProperties build() {
            return new McpServerProperties(this);
        }
    }
}

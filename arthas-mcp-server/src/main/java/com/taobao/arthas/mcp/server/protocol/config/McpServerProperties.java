package com.taobao.arthas.mcp.server.protocol.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP服务器配置属性类。
 *
 * 该类封装了MCP服务器的所有配置项，包括：
 * - 服务器基本信息（名称、版本、说明）
 * - 服务器能力配置（变更通知、资源订阅等）
 * - 网络配置（端点路径、超时设置）
 * - 协议配置（无状态或流式）
 *
 * 使用建造者模式（Builder Pattern）创建实例，确保配置的不可变性和一致性。
 *
 * @author Yeaury
 */
public class McpServerProperties {

    /**
     * 服务器名称，用于标识MCP服务器实例
     */
    private final String name;

    /**
     * 服务器版本号，遵循语义化版本规范
     */
    private final String version;

    /**
     * 服务器使用说明，提供给客户端的使用指南
     */
    private final String instructions;

    /**
     * 工具变更通知开关，当服务器工具列表发生变化时是否通知客户端
     */
    private final boolean toolChangeNotification;

    /**
     * 资源变更通知开关，当服务器资源列表发生变化时是否通知客户端
     */
    private final boolean resourceChangeNotification;

    /**
     * 提示模板变更通知开关，当服务器提示模板列表发生变化时是否通知客户端
     */
    private final boolean promptChangeNotification;

    /**
     * 资源订阅功能开关，是否允许客户端订阅资源变更
     */
    private final boolean resourceSubscribe;

    /**
     * MCP端点路径，客户端向此路径发送MCP协议请求
     */
    private final String mcpEndpoint;

    /**
     * 请求超时时长，单个请求的最大处理时间
     */
    private final Duration requestTimeout;

    /**
     * 初始化超时时长，服务器启动和初始化的最大时间
     */
    private final Duration initializationTimeout;

    /**
     * JSON对象映射器，用于序列化和反序列化MCP协议消息
     */
    private final ObjectMapper objectMapper;

    /**
     * 服务器协议类型枚举，指定使用流式或无状态协议
     */
    private final ServerProtocol protocol;

    /**
     * 按工具名称指定的响应MIME类型映射。
     *
     * 这是一个可选配置，允许为特定的工具返回不同MIME类型的响应。
     * 例如，某些工具可能返回文本/plain，而其他工具返回application/json。
     * 键为工具名称，值为对应的MIME类型字符串。
     */
    private Map<String, String> toolResponseMimeType = new HashMap<>();

    /**
     * 私有构造函数，只能通过Builder创建实例。
     *
     * 从Builder中复制所有配置属性到新创建的实例，确保配置的不可变性。
     *
     * @param builder 包含所有配置信息的建造者实例
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
     * 创建带有默认配置的Builder实例。
     *
     * @return 新的Builder实例，预配置了默认值
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 服务器协议类型枚举。
     *
     * 定义MCP服务器支持的两种传输协议：
     * - STREAMABLE: 流式协议，支持服务器推送事件（SSE），保持持久连接
     * - STATELESS: 无状态协议，每次请求独立处理，不维护会话状态
     */
    public enum ServerProtocol {
        /**
         * 流式协议，使用服务器发送事件（SSE）实现服务器到客户端的主动推送
         */
        STREAMABLE,

        /**
         * 无状态协议，基于标准的HTTP请求-响应模式，不维护连接状态
         */
        STATELESS
    }

    /**
     * 获取服务器名称。
     *
     * @return 服务器名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 获取服务器版本号。
     *
     * @return 服务器版本字符串
     */
    public String getVersion() {
        return version;
    }

    /**
     * 获取服务器使用说明。
     *
     * @return 服务器说明字符串
     */
    public String getInstructions() {
        return instructions;
    }

    /**
     * 获取工具变更通知开关状态。
     *
     * @return 如果启用工具变更通知返回true，否则返回false
     */
    public boolean isToolChangeNotification() {
        return toolChangeNotification;
    }

    /**
     * 获取资源变更通知开关状态。
     *
     * @return 如果启用资源变更通知返回true，否则返回false
     */
    public boolean isResourceChangeNotification() {
        return resourceChangeNotification;
    }

    /**
     * 获取提示模板变更通知开关状态。
     *
     * @return 如果启用提示模板变更通知返回true，否则返回false
     */
    public boolean isPromptChangeNotification() {
        return promptChangeNotification;
    }

    /**
     * 获取资源订阅功能开关状态。
     *
     * @return 如果启用资源订阅功能返回true，否则返回false
     */
    public boolean isResourceSubscribe() {
        return resourceSubscribe;
    }

    /**
     * 获取MCP端点路径。
     *
     * @return MCP端点路径字符串（例如 "/mcp"）
     */
    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    /**
     * 获取请求超时时长。
     *
     * @return 请求超时时间Duration对象
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * 获取初始化超时时长。
     *
     * @return 初始化超时时间Duration对象
     */
    public Duration getInitializationTimeout() {
        return initializationTimeout;
    }

    /**
     * 获取JSON对象映射器。
     *
     * @return ObjectMapper实例
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 获取服务器协议类型。
     *
     * @return ServerProtocol枚举值（STREAMABLE或STATELESS）
     */
    public ServerProtocol getProtocol() {
        return protocol;
    }

    /**
     * 获取工具响应MIME类型映射。
     *
     * @return 工具名称到MIME类型的映射Map
     */
    public Map<String, String> getToolResponseMimeType() {
        return toolResponseMimeType;
    }

    /**
     * 设置工具响应MIME类型映射。
     *
     * 允许为特定工具配置自定义的响应MIME类型。
     *
     * @param toolResponseMimeType 工具名称到MIME类型的映射Map
     */
    public void setToolResponseMimeType(Map<String, String> toolResponseMimeType) {
        this.toolResponseMimeType = toolResponseMimeType;
    }

    /**
     * McpServerProperties的建造者类。
     *
     * 提供流式API用于构建McpServerProperties实例，支持链式调用。
     * 所有配置项都有合理的默认值，简化服务器配置过程。
     */
    public static class Builder {
        // 默认值配置
        /**
         * 默认服务器名称
         */
        private String name = "mcp-server";

        /**
         * 默认服务器版本号
         */
        private String version = "1.0.0";

        /**
         * 服务器使用说明，默认为null
         */
        private String instructions;

        /**
         * 工具变更通知，默认启用
         */
        private boolean toolChangeNotification = true;

        /**
         * 资源变更通知，默认禁用
         */
        private boolean resourceChangeNotification = false;

        /**
         * 提示模板变更通知，默认禁用
         */
        private boolean promptChangeNotification = false;

        /**
         * 资源订阅功能，默认禁用
         */
        private boolean resourceSubscribe = false;

        /**
         * 服务器绑定地址，默认为localhost
         */
        private String bindAddress = "localhost";

        /**
         * 服务器监听端口，默认为8080
         */
        private int port = 8080;

        /**
         * MCP端点路径，默认为 "/mcp"
         */
        private String mcpEndpoint = "/mcp";

        /**
         * 请求超时时间，默认10秒
         */
        private Duration requestTimeout = Duration.ofSeconds(10);

        /**
         * 初始化超时时间，默认30秒
         */
        private Duration initializationTimeout = Duration.ofSeconds(30);

        /**
         * JSON对象映射器，必须手动设置
         */
        private ObjectMapper objectMapper;

        /**
         * 服务器协议类型，默认为流式协议
         */
        private ServerProtocol protocol = ServerProtocol.STREAMABLE;

        /**
         * 默认构造函数。
         */
        public Builder() {
            // 默认构造函数
        }

        /**
         * 设置服务器名称。
         *
         * @param name 服务器名称
         * @return 当前Builder实例，支持链式调用
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置服务器版本号。
         *
         * @param version 服务器版本号
         * @return 当前Builder实例，支持链式调用
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * 设置服务器使用说明。
         *
         * @param instructions 服务器说明文本
         * @return 当前Builder实例，支持链式调用
         */
        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        /**
         * 设置工具变更通知开关。
         *
         * @param toolChangeNotification 是否启用工具变更通知
         * @return 当前Builder实例，支持链式调用
         */
        public Builder toolChangeNotification(boolean toolChangeNotification) {
            this.toolChangeNotification = toolChangeNotification;
            return this;
        }

        /**
         * 设置资源变更通知开关。
         *
         * @param resourceChangeNotification 是否启用资源变更通知
         * @return 当前Builder实例，支持链式调用
         */
        public Builder resourceChangeNotification(boolean resourceChangeNotification) {
            this.resourceChangeNotification = resourceChangeNotification;
            return this;
        }

        /**
         * 设置提示模板变更通知开关。
         *
         * @param promptChangeNotification 是否启用提示模板变更通知
         * @return 当前Builder实例，支持链式调用
         */
        public Builder promptChangeNotification(boolean promptChangeNotification) {
            this.promptChangeNotification = promptChangeNotification;
            return this;
        }

        /**
         * 设置资源订阅功能开关。
         *
         * @param resourceSubscribe 是否启用资源订阅功能
         * @return 当前Builder实例，支持链式调用
         */
        public Builder resourceSubscribe(boolean resourceSubscribe) {
            this.resourceSubscribe = resourceSubscribe;
            return this;
        }

        /**
         * 设置MCP端点路径。
         *
         * @param mcpEndpoint MCP端点路径（如 "/mcp"）
         * @return 当前Builder实例，支持链式调用
         */
        public Builder mcpEndpoint(String mcpEndpoint) {
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        /**
         * 设置请求超时时间。
         *
         * @param requestTimeout 请求超时时长
         * @return 当前Builder实例，支持链式调用
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        /**
         * 设置初始化超时时间。
         *
         * @param initializationTimeout 初始化超时时长
         * @return 当前Builder实例，支持链式调用
         */
        public Builder initializationTimeout(Duration initializationTimeout) {
            this.initializationTimeout = initializationTimeout;
            return this;
        }

        /**
         * 设置JSON对象映射器。
         *
         * @param ObjectMapper JSON对象映射器实例
         * @return 当前Builder实例，支持链式调用
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * 设置服务器协议类型。
         *
         * @param protocol 协议类型（STREAMABLE或STATELESS）
         * @return 当前Builder实例，支持链式调用
         */
        public Builder protocol(ServerProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * 构建McpServerProperties实例。
         *
         * 根据当前Builder的所有配置创建一个不可变的McpServerProperties实例。
         *
         * @return 配置完成的McpServerProperties实例
         */
        public McpServerProperties build() {
            return new McpServerProperties(this);
        }
    }
}

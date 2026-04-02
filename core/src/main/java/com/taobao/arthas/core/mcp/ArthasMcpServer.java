// Arthas MCP服务器类包
package com.taobao.arthas.core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.core.mcp.tool.util.McpToolUtils;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.config.McpServerProperties;
import com.taobao.arthas.mcp.server.protocol.config.McpServerProperties.ServerProtocol;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServer;
import com.taobao.arthas.mcp.server.protocol.server.McpServer;
import com.taobao.arthas.mcp.server.protocol.server.McpStatelessNettyServer;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpHttpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpStatelessHttpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpStreamableHttpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.transport.NettyStatelessServerTransport;
import com.taobao.arthas.mcp.server.protocol.server.transport.NettyStreamableServerTransportProvider;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.Implementation;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.ServerCapabilities;
import com.taobao.arthas.mcp.server.protocol.spec.McpStreamableServerTransportProvider;
import com.taobao.arthas.mcp.server.tool.DefaultToolCallbackProvider;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.tool.ToolCallbackProvider;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arthas MCP服务器类
 * 在Arthas启动后用于暴露HTTP服务，提供MCP协议接口
 * 支持两种协议模式：STREAMABLE（可流式）和STATELESS（无状态）
 */
public class ArthasMcpServer {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ArthasMcpServer.class);

    /**
     * Arthas工具类的基包（在core模块中）
     * 该包下包含了所有Arthas MCP工具的实现类
     */
    public static final String ARTHAS_TOOL_BASE_PACKAGE = "com.taobao.arthas.core.mcp.tool.function";

    // 可流式处理的MCP Netty服务器实例
    private McpNettyServer streamableServer;
    // 无状态的MCP Netty服务器实例
    private McpStatelessNettyServer statelessServer;

    // MCP端点路径（如 "/mcp"）
    private final String mcpEndpoint;
    // MCP协议类型（STREAMABLE或STATELESS）
    private final ServerProtocol protocol;

    // 命令执行器，用于执行Arthas命令
    private final CommandExecutor commandExecutor;

    // 统一的MCP HTTP请求处理器
    private McpHttpRequestHandler unifiedMcpHandler;

    // 可流式处理的HTTP请求处理器
    private McpStreamableHttpRequestHandler streamableHandler;

    // 无状态的HTTP请求处理器
    private McpStatelessHttpRequestHandler statelessHandler;

    // 默认的MCP端点路径
    public static final String DEFAULT_MCP_ENDPOINT = "/mcp";

    /**
     * 构造ArthasMcpServer实例
     *
     * @param mcpEndpoint MCP端点路径
     * @param commandExecutor 命令执行器
     * @param protocol MCP协议类型
     */
    public ArthasMcpServer(String mcpEndpoint, CommandExecutor commandExecutor, String protocol) {
        // 使用默认端点如果未指定
        this.mcpEndpoint = mcpEndpoint != null ? mcpEndpoint : DEFAULT_MCP_ENDPOINT;
        this.commandExecutor = commandExecutor;

        // 解析协议类型，默认为STREAMABLE
        ServerProtocol resolvedProtocol = ServerProtocol.STREAMABLE;
        if (protocol != null && !protocol.trim().isEmpty()) {
            try {
                resolvedProtocol = ServerProtocol.valueOf(protocol.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid MCP protocol: {}. Using default: STREAMABLE", protocol);
            }
        }
        this.protocol = resolvedProtocol;
    }

    /**
     * 获取统一的MCP HTTP请求处理器
     *
     * @return MCP HTTP请求处理器实例
     */
    public McpHttpRequestHandler getMcpRequestHandler() {
        return unifiedMcpHandler;
    }

    /**
     * 启动MCP服务器
     * 根据配置的协议类型创建相应的服务器并启动
     */
    public void start() {
        try {
            // 注册Arthas专用的JSON过滤器
            com.taobao.arthas.core.mcp.util.McpObjectVOFilter.register();

            // 构建MCP服务器配置属性
            McpServerProperties properties = new McpServerProperties.Builder()
                    .name("arthas-mcp-server")
                    .version("4.1.8")
                    .mcpEndpoint(mcpEndpoint)
                    .toolChangeNotification(true)
                    .resourceChangeNotification(true)
                    .promptChangeNotification(true)
                    .objectMapper(JsonParser.getObjectMapper())
                    .protocol(this.protocol)
                    .build();

            // 使用core模块中的Arthas工具基包
            DefaultToolCallbackProvider toolCallbackProvider = new DefaultToolCallbackProvider();
            toolCallbackProvider.setToolBasePackage(ARTHAS_TOOL_BASE_PACKAGE);

            // 获取所有工具回调
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            List<ToolCallback> providerToolCallbacks = Arrays.stream(callbacks)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 创建统一的MCP HTTP请求处理器
            unifiedMcpHandler = McpHttpRequestHandler.builder()
                    .mcpEndpoint(properties.getMcpEndpoint())
                    .objectMapper(properties.getObjectMapper())
                    .protocol(properties.getProtocol())
                    .build();

            // 根据协议类型创建不同的服务器
            if (properties.getProtocol() == ServerProtocol.STREAMABLE) {
                // 创建可流式处理的服务器
                McpStreamableServerTransportProvider transportProvider = createStreamableHttpTransportProvider(properties);
                streamableHandler = transportProvider.getMcpRequestHandler();
                unifiedMcpHandler.setStreamableHandler(streamableHandler);

                // 构建可流式处理的服务器规范
                McpServer.StreamableServerNettySpecification streamableServerNettySpecification = McpServer.netty(transportProvider)
                        .serverInfo(new Implementation(properties.getName(), properties.getVersion()))
                        .capabilities(buildServerCapabilities(properties))
                        .instructions(properties.getInstructions())
                        .requestTimeout(properties.getRequestTimeout())
                        .commandExecutor(commandExecutor)
                        .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : JsonParser.getObjectMapper());

                // 注册工具
                streamableServerNettySpecification.tools(
                        McpToolUtils.toStreamableToolSpecifications(providerToolCallbacks));

                // 构建并启动服务器
                streamableServer = streamableServerNettySpecification.build();
            } else {
                // 创建无状态服务器
                NettyStatelessServerTransport statelessTransport = createStatelessHttpTransport(properties);
                statelessHandler = statelessTransport.getMcpRequestHandler();
                unifiedMcpHandler.setStatelessHandler(statelessHandler);

                // 构建无状态服务器规范
                McpServer.StatelessServerNettySpecification statelessServerNettySpecification = McpServer.netty(statelessTransport)
                        .serverInfo(new Implementation(properties.getName(), properties.getVersion()))
                        .capabilities(buildServerCapabilities(properties))
                        .instructions(properties.getInstructions())
                        .requestTimeout(properties.getRequestTimeout())
                        .commandExecutor(commandExecutor)
                        .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : JsonParser.getObjectMapper());

                // 注册工具
                statelessServerNettySpecification.tools(
                        McpToolUtils.toStatelessToolSpecifications(providerToolCallbacks));

                // 构建并启动服务器
                statelessServer = statelessServerNettySpecification.build();
            }

            // 输出启动成功信息
            logger.info("Arthas MCP server started successfully");
            logger.info("- MCP Endpoint: {}", properties.getMcpEndpoint());
            logger.info("- Transport mode: {}", properties.getProtocol());
            logger.info("- Available tools: {}", providerToolCallbacks.size());
            logger.info("- Server ready to accept connections");
        } catch (Exception e) {
            logger.error("Failed to start Arthas MCP server", e);
            throw new RuntimeException("Failed to start Arthas MCP server", e);
        }
    }

    /**
     * MCP服务器的默认保活间隔时间（15秒）
     */
    public static final Duration DEFAULT_KEEP_ALIVE_INTERVAL = Duration.ofSeconds(15);

    /**
     * 创建可流式处理的HTTP传输提供者
     *
     * @param properties MCP服务器配置属性
     * @return 可流式处理的HTTP传输提供者
     */
    private NettyStreamableServerTransportProvider createStreamableHttpTransportProvider(McpServerProperties properties) {
        return NettyStreamableServerTransportProvider.builder()
                .mcpEndpoint(properties.getMcpEndpoint())
                .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : new ObjectMapper())
                .keepAliveInterval(DEFAULT_KEEP_ALIVE_INTERVAL)
                .build();
    }

    /**
     * 创建无状态HTTP传输
     *
     * @param properties MCP服务器配置属性
     * @return 无状态HTTP传输实例
     */
    private NettyStatelessServerTransport createStatelessHttpTransport(McpServerProperties properties) {
        return NettyStatelessServerTransport.builder()
                .mcpEndpoint(properties.getMcpEndpoint())
                .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : new ObjectMapper())
                .build();
    }

    /**
     * 构建服务器能力配置
     *
     * @param properties MCP服务器配置属性
     * @return 服务器能力配置对象
     */
    private ServerCapabilities buildServerCapabilities(McpServerProperties properties) {
        return ServerCapabilities.builder()
                .prompts(new ServerCapabilities.PromptCapabilities(properties.isPromptChangeNotification()))
                .resources(new ServerCapabilities.ResourceCapabilities(properties.isResourceSubscribe(), properties.isResourceChangeNotification()))
                .tools(new ServerCapabilities.ToolCapabilities(properties.isToolChangeNotification()))
                .build();
    }

    /**
     * 停止MCP服务器
     * 优雅地关闭所有服务器组件并释放资源
     */
    public void stop() {
        logger.info("Stopping Arthas MCP server...");
        try {
            // 关闭统一的MCP处理器
            if (unifiedMcpHandler != null) {
                logger.debug("Shutting down unified MCP handler");
                unifiedMcpHandler.closeGracefully().get();
                logger.info("Unified MCP handler stopped successfully");
            }

            // 关闭可流式处理的服务器
            if (streamableServer != null) {
                logger.debug("Shutting down streamable server");
                streamableServer.closeGracefully().get();
                logger.info("Streamable server stopped successfully");
            }

            // 关闭无状态服务器
            if (statelessServer != null) {
                logger.debug("Shutting down stateless server");
                statelessServer.closeGracefully().get();
                logger.info("Stateless server stopped successfully");
            }

            logger.info("Arthas MCP server stopped completely");
        } catch (Exception e) {
            logger.error("Failed to stop Arthas MCP server gracefully", e);
        }
    }
}

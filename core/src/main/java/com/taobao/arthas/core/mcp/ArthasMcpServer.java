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
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import com.taobao.arthas.mcp.server.task.InMemoryTaskMessageQueue;
import com.taobao.arthas.mcp.server.task.InMemoryTaskStore;
import com.taobao.arthas.mcp.server.task.TaskAwareToolSpecification;
import com.taobao.arthas.mcp.server.task.TaskMessageQueue;
import com.taobao.arthas.mcp.server.task.TaskStore;
import com.taobao.arthas.mcp.server.tool.DefaultToolCallbackProvider;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.tool.ToolCallbackCreateTaskHandler;
import com.taobao.arthas.mcp.server.tool.ToolCallbackProvider;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arthas MCP Server
 * Used to expose HTTP service after Arthas startup
 */
public class ArthasMcpServer {
    private static final Logger logger = LoggerFactory.getLogger(ArthasMcpServer.class);

    /**
     * Arthas tool base package in core module
     */
    public static final String ARTHAS_TOOL_BASE_PACKAGE = "com.taobao.arthas.core.mcp.tool.function";

    private McpNettyServer streamableServer;
    private McpStatelessNettyServer statelessServer;

    private final String mcpEndpoint;
    private final ServerProtocol protocol;

    private final CommandExecutor commandExecutor;
    private ArthasCommandSessionManager sessionManager;

    private McpHttpRequestHandler unifiedMcpHandler;

    private McpStreamableHttpRequestHandler streamableHandler;

    private McpStatelessHttpRequestHandler statelessHandler;

    public static final String DEFAULT_MCP_ENDPOINT = "/mcp";
    
    public ArthasMcpServer(String mcpEndpoint, CommandExecutor commandExecutor, String protocol) {
        this.mcpEndpoint = mcpEndpoint != null ? mcpEndpoint : DEFAULT_MCP_ENDPOINT;
        this.commandExecutor = commandExecutor;
        
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

    public McpHttpRequestHandler getMcpRequestHandler() {
        return unifiedMcpHandler;
    }

    /**
     * 启动 MCP 服务器
     */
    public void start() {
        try {
            // 注册 Arthas 特定的 JSON 过滤器
            com.taobao.arthas.core.mcp.util.McpObjectVOFilter.register();

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

            ToolClassification toolClassification = scanAndClassifyTools();

            unifiedMcpHandler = McpHttpRequestHandler.builder()
                    .mcpEndpoint(properties.getMcpEndpoint())
                    .objectMapper(properties.getObjectMapper())
                    .protocol(properties.getProtocol())
                    .build();

            if (properties.getProtocol() == ServerProtocol.STREAMABLE) {
                startStreamableServer(properties, toolClassification);
            } else {
                startStatelessServer(properties, toolClassification);
            }

            logger.info("Arthas MCP server started successfully");
            logger.info("- MCP Endpoint: {}", properties.getMcpEndpoint());
            logger.info("- Transport mode: {}", properties.getProtocol());
        } catch (Exception e) {
            logger.error("Failed to start Arthas MCP server", e);
            throw new RuntimeException("Failed to start Arthas MCP server", e);
        }
    }

    /**
     * 扫描并分类工具
     */
    private ToolClassification scanAndClassifyTools() {
        DefaultToolCallbackProvider toolCallbackProvider = new DefaultToolCallbackProvider();
        toolCallbackProvider.setToolBasePackage(ARTHAS_TOOL_BASE_PACKAGE);
        
        ToolCallback[] allCallbacks = toolCallbackProvider.getToolCallbacks();
        
        // 根据 taskSupport 属性分类工具
        List<ToolCallback> requiredTaskTools = new ArrayList<>();  // taskSupport=required
        List<ToolCallback> optionalTaskTools = new ArrayList<>();  // taskSupport=optional
        List<ToolCallback> normalTools = new ArrayList<>();        // taskSupport=forbidden
        
        for (ToolCallback callback : allCallbacks) {
            if (callback == null) {
                continue;
            }
            
            ToolDefinition def = callback.getToolDefinition();
            McpSchema.TaskSupportMode taskSupport = def.taskSupport();
            
            // 根据 taskSupport 分类
            switch (taskSupport) {
                case REQUIRED:
                    requiredTaskTools.add(callback);
                    break;
                case OPTIONAL:
                    optionalTaskTools.add(callback);
                    break;
                case FORBIDDEN:
                default:
                    normalTools.add(callback);
                    break;
            }
        }
        
        logger.info("Scanned {} tools: {} normal, {} optional-task, {} required-task", 
                allCallbacks.length, normalTools.size(), optionalTaskTools.size(), requiredTaskTools.size());
        
        return new ToolClassification(Arrays.asList(allCallbacks), normalTools, optionalTaskTools, requiredTaskTools);
    }
    
    /**
     * 启动 Streamable 模式服务器
     */
    private void startStreamableServer(McpServerProperties properties, ToolClassification classification) {
        // 初始化 SessionManager
        this.sessionManager = new ArthasCommandSessionManager(commandExecutor);
        logger.info("Initialized ArthasCommandSessionManager for MCP server");
        
        McpStreamableServerTransportProvider transportProvider = createStreamableHttpTransportProvider(properties);
        streamableHandler = transportProvider.getMcpRequestHandler();
        unifiedMcpHandler.setStreamableHandler(streamableHandler);

        // 准备任务感知工具列表（taskSupport = OPTIONAL 或 REQUIRED）
        List<ToolCallback> taskAwareTools = new ArrayList<>();
        taskAwareTools.addAll(classification.optionalTaskTools);
        taskAwareTools.addAll(classification.requiredTaskTools);
        
        boolean hasTaskTools = !taskAwareTools.isEmpty();

        McpServer.StreamableServerNettySpecification serverSpec = McpServer.netty(transportProvider)
                .serverInfo(new Implementation(properties.getName(), properties.getVersion()))
                .capabilities(buildServerCapabilities(properties, hasTaskTools))
                .instructions(properties.getInstructions())
                .requestTimeout(properties.getRequestTimeout())
                .commandExecutor(commandExecutor)
                .sessionManager(this.sessionManager)
                .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : JsonParser.getObjectMapper());

        // 只注册普通工具（taskSupport = FORBIDDEN）
        serverSpec.tools(McpToolUtils.toStreamableToolSpecifications(classification.normalTools));
        logger.debug("Registered {} normal tools", classification.normalTools.size());
        
        if (hasTaskTools) {
            configureTaskSupport(serverSpec, taskAwareTools);
        }

        streamableServer = serverSpec.build();
    }

    /**
     * 配置任务支持
     */
    private void configureTaskSupport(McpServer.StreamableServerNettySpecification serverSpec,
                                      List<ToolCallback> taskAwareTools) {
        logger.info("Configuring tasks support for {} task-aware tools", taskAwareTools.size());

        // 创建 TaskStore 和 TaskMessageQueue
        TaskStore<McpSchema.ServerTaskPayloadResult> taskStore = InMemoryTaskStore.<McpSchema.ServerTaskPayloadResult>builder()
                .defaultTtl(Duration.ofMinutes(30))  // 任务 TTL 30 分钟
                .build();

        TaskMessageQueue messageQueue = new InMemoryTaskMessageQueue();

        // 配置 TaskStore 和 TaskMessageQueue
        serverSpec.taskStore(taskStore).taskMessageQueue(messageQueue);

        // 为每个任务感知工具创建 TaskAwareToolSpecification
        for (ToolCallback callback : taskAwareTools) {
            ToolDefinition def = callback.getToolDefinition();

            ToolCallbackCreateTaskHandler createTaskHandler = new ToolCallbackCreateTaskHandler(callback);

            TaskAwareToolSpecification spec = TaskAwareToolSpecification.builder()
                    .name(def.getName())
                    .description(def.getDescription())
                    .inputSchema(def.getInputSchema())
                    .taskSupport(def.taskSupport())
                    .createTaskHandler(createTaskHandler)
                    .build();

            serverSpec.taskTool(spec);
            logger.debug("Registered task-aware tool: {} (taskSupport: {})", def.getName(), def.taskSupport());
        }

        logger.info("Registered {} task-aware tools successfully", taskAwareTools.size());
    }
    
    /**
     * 启动 Stateless 模式服务器
     */
    private void startStatelessServer(McpServerProperties properties, ToolClassification classification) {
        // 创建传输层
        NettyStatelessServerTransport statelessTransport = createStatelessHttpTransport(properties);
        statelessHandler = statelessTransport.getMcpRequestHandler();
        unifiedMcpHandler.setStatelessHandler(statelessHandler);
        
        // Stateless 模式不支持任务
        boolean enableTasks = false;
        
        // 构建服务器规格
        McpServer.StatelessServerNettySpecification serverSpec = McpServer.netty(statelessTransport)
                .serverInfo(new Implementation(properties.getName(), properties.getVersion()))
                .capabilities(buildServerCapabilities(properties, enableTasks))
                .instructions(properties.getInstructions())
                .requestTimeout(properties.getRequestTimeout())
                .commandExecutor(commandExecutor)
                .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : JsonParser.getObjectMapper());
        
        // 在 stateless 模式下，所有工具都作为普通工具注册（不支持任务）
        serverSpec.tools(McpToolUtils.toStatelessToolSpecifications(classification.allCallbacks));
        logger.info("Registered {} tools in stateless mode (tasks not supported)", classification.allCallbacks.size());
        
        // 构建并启动服务器
        statelessServer = serverSpec.build();
    }
    
    /**
     * 工具分类结果
     */
    private static class ToolClassification {
        final List<ToolCallback> allCallbacks;
        final List<ToolCallback> normalTools;
        final List<ToolCallback> optionalTaskTools;
        final List<ToolCallback> requiredTaskTools;
        
        ToolClassification(List<ToolCallback> allCallbacks, 
                          List<ToolCallback> normalTools,
                          List<ToolCallback> optionalTaskTools,
                          List<ToolCallback> requiredTaskTools) {
            this.allCallbacks = allCallbacks;
            this.normalTools = normalTools;
            this.optionalTaskTools = optionalTaskTools;
            this.requiredTaskTools = requiredTaskTools;
        }
    }
    
    /**
     * Default keep-alive interval for MCP server (15 seconds)
     */
    public static final Duration DEFAULT_KEEP_ALIVE_INTERVAL = Duration.ofSeconds(15);
    
    /**
     * Create HTTP transport provider
     */
    private NettyStreamableServerTransportProvider createStreamableHttpTransportProvider(McpServerProperties properties) {
        return NettyStreamableServerTransportProvider.builder()
                .mcpEndpoint(properties.getMcpEndpoint())
                .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : new ObjectMapper())
                .keepAliveInterval(DEFAULT_KEEP_ALIVE_INTERVAL)
                .build();
    }

    private NettyStatelessServerTransport createStatelessHttpTransport(McpServerProperties properties) {
        return NettyStatelessServerTransport.builder()
                .mcpEndpoint(properties.getMcpEndpoint())
                .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : new ObjectMapper())
                .build();
    }

    /**
     * 构建服务器能力声明。
     * 
     * @param properties 服务器属性
     * @param enableTasks 是否启用任务支持（只有在有任务工具时才启用）
     * @return ServerCapabilities
     */
    private ServerCapabilities buildServerCapabilities(McpServerProperties properties, boolean enableTasks) {
        ServerCapabilities.Builder builder = ServerCapabilities.builder()
                .prompts(new ServerCapabilities.PromptCapabilities(properties.isPromptChangeNotification()))
                .resources(new ServerCapabilities.ResourceCapabilities(properties.isResourceSubscribe(), properties.isResourceChangeNotification()))
                .tools(new ServerCapabilities.ToolCapabilities(properties.isToolChangeNotification()));
        
        // 只有在有任务工具时才声明 tasks capability
        if (enableTasks) {
            // 声明服务器支持的任务能力
            ServerCapabilities.TaskCapabilities taskCapabilities = ServerCapabilities.TaskCapabilities.builder()
                    .list()        // 支持 tasks/list（列出所有任务）
                    .cancel()      // 支持 tasks/cancel（取消任务）
                    .toolsCall()   // 支持 tools/call 的任务增强执行（包括 tasks/get 和 tasks/result）
                    .build();
            
            builder.tasks(taskCapabilities);
            logger.info("Tasks capability enabled (supports list, cancel, tools/call with tasks)");
        } else {
            logger.info("Tasks capability disabled (no task-aware tools)");
        }
        
        return builder.build();
    }

    public void stop() {
        logger.info("Stopping Arthas MCP server...");
        try {
            if (unifiedMcpHandler != null) {
                logger.debug("Shutting down unified MCP handler");
                unifiedMcpHandler.closeGracefully().get();
                logger.info("Unified MCP handler stopped successfully");
            }

            if (streamableServer != null) {
                logger.debug("Shutting down streamable server");
                streamableServer.closeGracefully().get();
                logger.info("Streamable server stopped successfully");
            }

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

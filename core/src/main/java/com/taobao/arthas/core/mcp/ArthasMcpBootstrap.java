// Arthas MCP启动引导类包
package com.taobao.arthas.core.mcp;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arthas MCP启动引导类
 * 负责初始化、启动和关闭MCP（Model Context Protocol）服务器
 * 提供Arthas命令的MCP协议访问接口
 *
 * @author Yeaury
 */
public class ArthasMcpBootstrap {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ArthasMcpBootstrap.class);

    // MCP服务器实例
    private ArthasMcpServer mcpServer;
    // 命令执行器，用于执行Arthas命令
    private final CommandExecutor commandExecutor;
    // MCP端点路径（如 "/mcp"）
    private final String mcpEndpoint;
    // MCP协议类型（如 "STREAMABLE" 或 "STATELESS"）
    private final String protocol;
    // 静态单例实例
    private static ArthasMcpBootstrap instance;

    /**
     * 构造ArthasMcpBootstrap实例
     *
     * @param commandExecutor 命令执行器，用于执行Arthas命令
     * @param mcpEndpoint MCP端点路径
     * @param protocol MCP协议类型
     */
    public ArthasMcpBootstrap(CommandExecutor commandExecutor, String mcpEndpoint, String protocol) {
        this.commandExecutor = commandExecutor;
        this.mcpEndpoint = mcpEndpoint;
        this.protocol = protocol;
        // 设置静态单例实例
        instance = this;
    }

    /**
     * 获取ArthasMcpBootstrap单例实例
     *
     * @return ArthasMcpBootstrap单例实例
     */
    public static ArthasMcpBootstrap getInstance() {
        return instance;
    }

    /**
     * 获取命令执行器
     *
     * @return 命令执行器实例
     */
    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    /**
     * 启动MCP服务器
     * 创建MCP服务器实例并启动服务
     *
     * @return 启动后的MCP服务器实例
     */
    public ArthasMcpServer start() {
        logger.info("Initializing Arthas MCP Bootstrap...");
        try {
            logger.debug("Creating MCP server instance with command executor: {}",
                    commandExecutor.getClass().getSimpleName());

            // 创建MCP服务器实例，传入命令执行器、端点和协议类型
            mcpServer = new ArthasMcpServer(mcpEndpoint, commandExecutor, protocol);
            logger.debug("MCP server instance created successfully");

            // 启动MCP服务器
            mcpServer.start();
            logger.info("Arthas MCP server initialized successfully");
            logger.info("Bootstrap ready - server is operational");
            return mcpServer;
        } catch (Exception e) {
            logger.error("Failed to initialize Arthas MCP server", e);
            throw new RuntimeException("Failed to initialize Arthas MCP server", e);
        }
    }

    /**
     * 关闭MCP服务器
     * 停止MCP服务器并释放相关资源
     */
    public void shutdown() {
        logger.info("Initiating Arthas MCP Bootstrap shutdown...");
        if (mcpServer != null) {
            logger.debug("Stopping MCP server...");
            // 停止MCP服务器
            mcpServer.stop();
            logger.info("MCP server stopped");
        } else {
            logger.warn("MCP server was null during shutdown - may not have been properly initialized");
        }
        logger.info("Arthas MCP Bootstrap shutdown completed");
    }
}

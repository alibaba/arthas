package com.taobao.arthas.mcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arthas MCP Bootstrap class
 *
 * @author Yeaury
 */
public class ArthasMcpBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(ArthasMcpBootstrap.class);
    
    private ArthasMcpServer mcpServer;
    private final CommandExecutor commandExecutor;
    private final String mcpEndpoint;
    private final String protocol;
    private static ArthasMcpBootstrap instance;

    public ArthasMcpBootstrap(CommandExecutor commandExecutor, String mcpEndpoint, String protocol) {
        this.commandExecutor = commandExecutor;
        this.mcpEndpoint = mcpEndpoint;
        this.protocol = protocol;
        instance = this;
    }

    public static ArthasMcpBootstrap getInstance() {
        return instance;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public ArthasMcpServer start() {
        logger.info("Initializing Arthas MCP Bootstrap...");
        try {
            logger.debug("Creating MCP server instance with command executor: {}", 
                    commandExecutor.getClass().getSimpleName());
            
            // Create and start MCP server with CommandExecutor and custom endpoint
            mcpServer = new ArthasMcpServer(mcpEndpoint, commandExecutor, protocol);
            logger.debug("MCP server instance created successfully");
            
            mcpServer.start();
            logger.info("Arthas MCP server initialized successfully");
            logger.info("Bootstrap ready - server is operational");
            return mcpServer;
        } catch (Exception e) {
            logger.error("Failed to initialize Arthas MCP server", e);
            throw new RuntimeException("Failed to initialize Arthas MCP server", e);
        }
    }

    public void shutdown() {
        logger.info("Initiating Arthas MCP Bootstrap shutdown...");
        if (mcpServer != null) {
            logger.debug("Stopping MCP server...");
            mcpServer.stop();
            logger.info("MCP server stopped");
        } else {
            logger.warn("MCP server was null during shutdown - may not have been properly initialized");
        }
        logger.info("Arthas MCP Bootstrap shutdown completed");
    }
}

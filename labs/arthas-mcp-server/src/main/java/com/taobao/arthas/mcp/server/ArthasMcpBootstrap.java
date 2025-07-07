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
    private static ArthasMcpBootstrap instance;

    public ArthasMcpBootstrap(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        instance = this;
    }

    public static ArthasMcpBootstrap getInstance() {
        return instance;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public ArthasMcpServer start() {
        try {
            // Create and start MCP server
            mcpServer = new ArthasMcpServer();
            mcpServer.start();
            logger.info("Arthas MCP server initialized successfully");
            return mcpServer;
        } catch (Exception e) {
            logger.error("Failed to initialize Arthas MCP server", e);
            throw new RuntimeException("Failed to initialize Arthas MCP server", e);
        }
    }

    public void shutdown() {
        if (mcpServer != null) {
            mcpServer.stop();
        }
    }
} 
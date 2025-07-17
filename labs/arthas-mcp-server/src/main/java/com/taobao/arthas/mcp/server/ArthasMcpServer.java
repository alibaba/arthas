package com.taobao.arthas.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.config.McpServerProperties;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServer;
import com.taobao.arthas.mcp.server.protocol.server.McpServer;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.transport.HttpNettyServerTransportProvider;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.*;
import com.taobao.arthas.mcp.server.protocol.spec.McpServerTransportProvider;
import com.taobao.arthas.mcp.server.tool.DefaultToolCallbackProvider;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.tool.ToolCallbackProvider;
import com.taobao.arthas.mcp.server.tool.util.McpToolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Arthas MCP Server
 * Used to expose HTTP service after Arthas startup
 */
public class ArthasMcpServer {
    private static final Logger logger = LoggerFactory.getLogger(ArthasMcpServer.class);
    private McpNettyServer server;
    private final int port;
    private final String bindAddress;
    private McpRequestHandler mcpRequestHandler;
    
    public ArthasMcpServer() {
        this(8080, "localhost");
    }
    
    public ArthasMcpServer(int port, String bindAddress) {
        this.port = port;
        this.bindAddress = bindAddress;
    }

    public McpRequestHandler getMcpRequestHandler() {
        return mcpRequestHandler;
    }

    /**
     * Start MCP server
     */
    public void start() {
        try {
            // 1. Create server configuration
            McpServerProperties properties = new McpServerProperties.Builder()
                    .name("arthas-mcp-server")
                    .version("1.0.0")
                    .bindAddress(bindAddress)
                    .port(port)
                    .messageEndpoint("/sse/message")
                    .sseEndpoint("/sse")
                    .toolChangeNotification(true)
                    .resourceChangeNotification(true)
                    .promptChangeNotification(true)
                    .objectMapper(new ObjectMapper())
                    .build();
            
            // 2. Create transport provider
            McpServerTransportProvider transportProvider = createHttpTransportProvider(properties);
            mcpRequestHandler = transportProvider.getMcpRequestHandler();
            
            // 3. Create server builder
            McpServer.NettySpecification serverBuilder = McpServer.netty(transportProvider)
                    .serverInfo(new Implementation(properties.getName(), properties.getVersion()))
                    .capabilities(buildServerCapabilities(properties))
                    .instructions(properties.getInstructions())
                    .requestTimeout(properties.getRequestTimeout())
                    .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : new ObjectMapper());

            ToolCallbackProvider toolCallbackProvider = new DefaultToolCallbackProvider();
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            List<ToolCallback> providerToolCallbacks = Arrays.stream(callbacks)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            serverBuilder.tools(
                    McpToolUtils.toToolSpecifications(providerToolCallbacks, properties));
            server = serverBuilder.build();
            
            logger.info("Arthas MCP server started, listening on {}:{}", bindAddress, port);
        } catch (Exception e) {
            logger.error("Failed to start Arthas MCP server", e);
            throw new RuntimeException("Failed to start Arthas MCP server", e);
        }
    }
    
    /**
     * Create HTTP transport provider
     */
    private HttpNettyServerTransportProvider createHttpTransportProvider(McpServerProperties properties) {
        return HttpNettyServerTransportProvider.builder()
                .messageEndpoint(properties.getMessageEndpoint())
                .sseEndpoint(properties.getSseEndpoint())
                .objectMapper(properties.getObjectMapper() != null ? properties.getObjectMapper() : new ObjectMapper())
                .build();
    }

    /**
     * Build server capabilities configuration
     */
    private ServerCapabilities buildServerCapabilities(McpServerProperties properties) {
        return ServerCapabilities.builder()
                .prompts(new ServerCapabilities.PromptCapabilities(properties.isPromptChangeNotification()))
                .resources(new ServerCapabilities.ResourceCapabilities(properties.isResourceSubscribe(), properties.isResourceChangeNotification()))
                .tools(new ServerCapabilities.ToolCapabilities(properties.isToolChangeNotification()))
                .build();
    }
    
    /**
     * Stop MCP server
     */
    public void stop() {
        if (server != null) {
            try {
                server.closeGracefully().get();
                logger.info("Arthas MCP server stopped");
            } catch (Exception e) {
                logger.error("Failed to stop Arthas MCP server", e);
            }
        }
    }

    public static void main(String[] args) {
        ArthasMcpServer arthasMcpServer = new ArthasMcpServer();
        arthasMcpServer.start();
        // Keep the server running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

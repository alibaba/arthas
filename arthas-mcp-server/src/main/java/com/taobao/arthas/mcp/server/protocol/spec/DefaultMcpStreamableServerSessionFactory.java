/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.McpInitRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpNotificationHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.store.InMemoryEventStore;
import com.taobao.arthas.mcp.server.task.TaskMessageQueue;
import com.taobao.arthas.mcp.server.task.TaskStore;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of the streamable server session factory.
 * This factory creates new MCP streamable server sessions with the provided configuration.
 *
 */
public class DefaultMcpStreamableServerSessionFactory implements McpStreamableServerSession.Factory {

    private final Duration requestTimeout;
    private final McpInitRequestHandler mcpInitRequestHandler;
    private final Map<String, McpRequestHandler<?>> requestHandlers;
    private final Map<String, McpNotificationHandler> notificationHandlers;
    private final CommandExecutor commandExecutor;
    private final TaskStore<McpSchema.ServerTaskPayloadResult> taskStore;
    private final TaskMessageQueue taskMessageQueue;

    public DefaultMcpStreamableServerSessionFactory(Duration requestTimeout,
                                                    McpInitRequestHandler mcpInitRequestHandler,
                                                    Map<String, McpRequestHandler<?>> requestHandlers,
                                                    Map<String, McpNotificationHandler> notificationHandlers,
                                                    CommandExecutor commandExecutor,
                                                    TaskStore<McpSchema.ServerTaskPayloadResult> taskStore,
                                                    TaskMessageQueue taskMessageQueue) {
        this.requestTimeout = requestTimeout;
        this.mcpInitRequestHandler = mcpInitRequestHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
        this.commandExecutor = commandExecutor;
        this.taskStore = taskStore;
        this.taskMessageQueue = taskMessageQueue;
    }

    @Override
    public McpStreamableServerSession.McpStreamableServerSessionInit startSession(
            McpSchema.InitializeRequest initializeRequest) {

        // Create a new session with a unique ID
        McpStreamableServerSession session = new McpStreamableServerSession(
                UUID.randomUUID().toString(),
                initializeRequest.getCapabilities(),
                initializeRequest.getClientInfo(),
                requestTimeout,
                requestHandlers,
                notificationHandlers,
                commandExecutor,
                new InMemoryEventStore(),
                taskStore,
                taskMessageQueue);

        // Handle the initialization request
        CompletableFuture<McpSchema.InitializeResult> initResult = 
                this.mcpInitRequestHandler.handle(initializeRequest);

        return new McpStreamableServerSession.McpStreamableServerSessionInit(session, initResult);
    }
}

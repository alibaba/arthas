/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;

import com.taobao.arthas.mcp.server.protocol.server.McpNotificationHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Main implementation of a streamable MCP server session that manages client connections
 * and handles JSON-RPC communication using CompletableFuture for async operations.
 */
public class McpStreamableServerSession implements McpSession {

    private static final Logger logger = LoggerFactory.getLogger(McpStreamableServerSession.class);

    private final ConcurrentHashMap<Object, McpStreamableServerSessionStream> requestIdToStream = new ConcurrentHashMap<>();

    private final String id;
    private final Duration requestTimeout;
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Map<String, McpRequestHandler<?>> requestHandlers;
    private final Map<String, McpNotificationHandler> notificationHandlers;
    
    private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();

    private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();

    private final AtomicReference<McpSession> listeningStreamRef;

    private final MissingMcpTransportSession missingMcpTransportSession;
    
    private volatile McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.INFO;

    private final CommandExecutor commandExecutor;

    private final ArthasCommandSessionManager commandSessionManager;
    
    private final EventStore eventStore;

    public McpStreamableServerSession(String id, McpSchema.ClientCapabilities clientCapabilities,
                                      McpSchema.Implementation clientInfo, Duration requestTimeout,
                                      Map<String, McpRequestHandler<?>> requestHandlers,
                                      Map<String, McpNotificationHandler> notificationHandlers,
                                      CommandExecutor commandExecutor, EventStore eventStore) {
        this.id = id;
        this.missingMcpTransportSession = new MissingMcpTransportSession(id);
        this.listeningStreamRef = new AtomicReference<>(this.missingMcpTransportSession);
        this.clientCapabilities.lazySet(clientCapabilities);
        this.clientInfo.lazySet(clientInfo);
        this.requestTimeout = requestTimeout;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
        this.commandExecutor = commandExecutor;
        this.commandSessionManager = new ArthasCommandSessionManager(commandExecutor);
        this.eventStore = eventStore;
    }

    /**
     * Sets the minimum logging level for this session.
     * @param minLoggingLevel the minimum logging level
     */
    public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
        Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
        this.minLoggingLevel = minLoggingLevel;
    }

    /**
     * Checks if notifications for the given logging level are allowed.
     * @param loggingLevel the logging level to check
     * @return true if notifications for this level are allowed
     */
    public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
        return loggingLevel.level() >= this.minLoggingLevel.level();
    }

    public String getId() {
        return this.id;
    }

    private String generateRequestId() {
        return this.id + "-" + this.requestCounter.getAndIncrement();
    }

    @Override
    public <T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
        McpSession listeningStream = this.listeningStreamRef.get();
        return listeningStream.sendRequest(method, requestParams, typeRef);
    }

    @Override
    public CompletableFuture<Void> sendNotification(String method, Object params) {
        McpSession listeningStream = this.listeningStreamRef.get();
        return listeningStream.sendNotification(method, params);
    }

    public CompletableFuture<Void> delete() {
        return this.closeGracefully().thenRun(() -> {
            try {
                eventStore.removeSessionEvents(this.id);
                commandSessionManager.closeCommandSession(this.id);
            } catch (Exception e) {
                logger.warn("Failed to clear session during deletion: {}", e.getMessage());
            }
        });
    }

    public McpStreamableServerSessionStream listeningStream(McpStreamableServerTransport transport) {
        McpStreamableServerSessionStream listeningStream = new McpStreamableServerSessionStream(transport);
        this.listeningStreamRef.set(listeningStream);
        return listeningStream;
    }

    /**
     * 重播会话事件，从指定的最后事件ID之后开始
     * 
     * @param lastEventId 最后一个事件ID，如果为null则从头开始重播
     * @return 事件消息流
     */
    public Stream<McpSchema.JSONRPCMessage> replay(Object lastEventId) {
        String lastEventIdStr = lastEventId != null ? lastEventId.toString() : null;
        
        return eventStore.getEventsForSession(this.id, lastEventIdStr)
                .map(EventStore.StoredEvent::getMessage);
    }

    public CompletableFuture<Void> responseStream(McpSchema.JSONRPCRequest jsonrpcRequest, 
            McpStreamableServerTransport transport, McpTransportContext transportContext) {
        
        McpStreamableServerSessionStream stream = new McpStreamableServerSessionStream(transport);
        McpRequestHandler<?> requestHandler = this.requestHandlers.get(jsonrpcRequest.getMethod());
        
        if (requestHandler == null) {
            MethodNotFoundError error = getMethodNotFoundError(jsonrpcRequest.getMethod());
            McpSchema.JSONRPCResponse errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, 
                    jsonrpcRequest.getId(), null,
                    new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
                            error.getMessage(), error.getData()));

            // 存储错误响应
            try {
                eventStore.storeEvent(this.id, errorResponse);
            } catch (Exception e) {
                logger.warn("Failed to store error response event: {}", e.getMessage());
            }

            return transport.sendMessage(errorResponse, null);
        }
        ArthasCommandContext commandContext = createCommandContext();
        
        return requestHandler
                .handle(new McpNettyServerExchange(this.id, stream, clientCapabilities.get(), 
                        clientInfo.get(), transportContext), commandContext, jsonrpcRequest.getParams())
                .thenApply(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, 
                        jsonrpcRequest.getId(), result, null))
                .thenCompose(response -> transport.sendMessage(response, null))
                .thenCompose(v -> transport.closeGracefully());
    }

    public CompletableFuture<Void> accept(McpSchema.JSONRPCNotification notification, 
            McpTransportContext transportContext) {
        
        McpNotificationHandler notificationHandler = this.notificationHandlers.get(notification.getMethod());
        if (notificationHandler == null) {
            logger.error("No handler registered for notification method: {}", notification.getMethod());
            return CompletableFuture.completedFuture(null);
        }

        ArthasCommandContext commandContext = createCommandContext();
        McpSession listeningStream = this.listeningStreamRef.get();
        return notificationHandler.handle(new McpNettyServerExchange(this.id, listeningStream,
                this.clientCapabilities.get(), this.clientInfo.get(), transportContext), commandContext, notification.getParams());
    }

    public CompletableFuture<Void> accept(McpSchema.JSONRPCResponse response) {
        McpStreamableServerSessionStream stream = this.requestIdToStream.get(response.getId());
        if (stream == null) {
            CompletableFuture<Void> f = CompletableFuture.completedFuture(null);
            f.completeExceptionally(new McpError("Unexpected response for unknown id " + response.getId()));
            return f;
        }

        CompletableFuture<McpSchema.JSONRPCResponse> future = stream.pendingResponses.remove(response.getId());
        if (future == null) {
            CompletableFuture<Void> f = CompletableFuture.completedFuture(null);
            f.completeExceptionally(new McpError("Unexpected response for unknown id " + response.getId()));
            return f;
        } else {
            future.complete(response);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    public class MethodNotFoundError {
        private final String method;
        private final String message;
        private final Object data;

        public MethodNotFoundError(String method, String message, Object data) {
            this.method = method;
            this.message = message;
            this.data = data;
        }

        public String getMethod() {
            return method;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }
    }


    private MethodNotFoundError getMethodNotFoundError(String method) {
        return new MethodNotFoundError(method, "Method not found: " + method, null);
    }

    @Override
    public CompletableFuture<Void> closeGracefully() {
        McpSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);
        
        // 清理 Arthas 命令会话
        try {
            commandSessionManager.closeCommandSession(this.id);
            logger.debug("Successfully closed command session during graceful shutdown: {}", this.id);
        } catch (Exception e) {
            logger.warn("Failed to close command session during graceful shutdown: {}", e.getMessage());
        }
        
        return listeningStream.closeGracefully();
        // TODO: Also close all the open streams
    }

    @Override
    public void close() {
        McpSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);
        
        // 清理 Arthas 命令会话
        try {
            commandSessionManager.closeCommandSession(this.id);
            logger.debug("Successfully closed command session during close: {}", this.id);
        } catch (Exception e) {
            logger.warn("Failed to close command session during close: {}", e.getMessage());
        }
        
        if (listeningStream != null) {
            listeningStream.close();
        }
        // TODO: Also close all open streams
    }

    public interface Factory {
        McpStreamableServerSessionInit startSession(McpSchema.InitializeRequest initializeRequest);
    }

    public static class McpStreamableServerSessionInit {
        private final McpStreamableServerSession session;
        private final CompletableFuture<McpSchema.InitializeResult> initResult;

        public McpStreamableServerSessionInit(
                McpStreamableServerSession session,
                CompletableFuture<McpSchema.InitializeResult> initResult) {
            this.session = session;
            this.initResult = initResult;
        }

        public McpStreamableServerSession session() {
            return session;
        }

        public CompletableFuture<McpSchema.InitializeResult> initResult() {
            return initResult;
        }
    }


    public final class McpStreamableServerSessionStream implements McpSession {

        private final ConcurrentHashMap<Object, CompletableFuture<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();

        private final McpStreamableServerTransport transport;
        private final String transportId;
        private final Supplier<String> uuidGenerator;

        public McpStreamableServerSessionStream(McpStreamableServerTransport transport) {
            this.transport = transport;
            this.transportId = UUID.randomUUID().toString();
            // This ID design allows for a constant-time extraction of the history by
            // precisely identifying the SSE stream using the first component
            this.uuidGenerator = () -> this.transportId + "_" + UUID.randomUUID();
        }

        @Override
        public <T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
            String requestId = McpStreamableServerSession.this.generateRequestId();

            McpStreamableServerSession.this.requestIdToStream.put(requestId, this);

            CompletableFuture<McpSchema.JSONRPCResponse> responseFuture = new CompletableFuture<>();
            this.pendingResponses.put(requestId, responseFuture);

            McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION,
                    method, requestId, requestParams);
            String messageId = null;
            
            // 存储发送的请求到事件存储
            try {
                messageId = McpStreamableServerSession.this.eventStore.storeEvent(
                    McpStreamableServerSession.this.id, jsonrpcRequest);
            } catch (Exception e) {
                logger.warn("Failed to store outbound request event: {}", e.getMessage());
            }

            // Send the message
            this.transport.sendMessage(jsonrpcRequest, messageId).exceptionally(ex -> {
                responseFuture.completeExceptionally(ex);
                return null;
            });

            return responseFuture.handle((jsonRpcResponse, throwable) -> {
                // Cleanup
                this.pendingResponses.remove(requestId);
                McpStreamableServerSession.this.requestIdToStream.remove(requestId);

                if (throwable != null) {
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    }
                    throw new RuntimeException(throwable);
                }

                if (jsonRpcResponse.getError() != null) {
                    throw new RuntimeException(new McpError(jsonRpcResponse.getError()));
                } else {
                    if (typeRef.getType().equals(Void.class)) {
                        return null;
                    } else {
                        return this.transport.unmarshalFrom(jsonRpcResponse.getResult(), typeRef);
                    }
                }
            });
        }

        @Override
        public CompletableFuture<Void> sendNotification(String method, Object params) {
            McpSchema.JSONRPCNotification jsonrpcNotification = new McpSchema.JSONRPCNotification(
                    McpSchema.JSONRPC_VERSION, method, params);
            String messageId = null;
            try {
                messageId = McpStreamableServerSession.this.eventStore.storeEvent(
                        McpStreamableServerSession.this.id, jsonrpcNotification);
            } catch (Exception e) {
                logger.warn("Failed to store outbound notification event: {}", e.getMessage());
            }

            return this.transport.sendMessage(jsonrpcNotification, messageId);
        }

        @Override
        public CompletableFuture<Void> closeGracefully() {
            // Complete all pending responses with error
            this.pendingResponses.values().forEach(future -> 
                    future.completeExceptionally(new RuntimeException("Stream closed")));
            this.pendingResponses.clear();
            
            // If this was the generic stream, reset it
            McpStreamableServerSession.this.listeningStreamRef.compareAndSet(this,
                    McpStreamableServerSession.this.missingMcpTransportSession);

            McpStreamableServerSession.this.requestIdToStream.values().removeIf(this::equals);
            
            return this.transport.closeGracefully();
        }

        @Override
        public void close() {
            this.pendingResponses.values().forEach(future -> 
                    future.completeExceptionally(new RuntimeException("Stream closed")));
            this.pendingResponses.clear();
            
            // If this was the generic stream, reset it
            McpStreamableServerSession.this.listeningStreamRef.compareAndSet(this,
                    McpStreamableServerSession.this.missingMcpTransportSession);
            McpStreamableServerSession.this.requestIdToStream.values().removeIf(this::equals);
            
            this.transport.close();
        }


    }

    /**
     * 创建命令执行上下文
     *
     * @return 命令执行上下文
     */
    private ArthasCommandContext createCommandContext() {
        ArthasCommandSessionManager.CommandSessionBinding binding = commandSessionManager.getCommandSession(this.id);
        return new ArthasCommandContext(commandExecutor, binding);
    }
} 
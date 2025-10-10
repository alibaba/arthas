/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.spec.HttpHeaders;
import com.taobao.arthas.mcp.server.protocol.spec.*;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.KeepAliveScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;

/**
 * Server-side implementation of the Model Context Protocol (MCP) streamable transport
 * layer using HTTP with Server-Sent Events (SSE) through Netty. This implementation
 * provides a bridge between Netty operations and the MCP transport interface.
 *
 * @see McpStreamableServerTransportProvider
 */
public class McpStreamableHttpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpStreamableHttpRequestHandler.class);

    /**
     * Event type for JSON-RPC messages sent through the SSE connection.
     */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /**
     * Header name for the response media types accepted by the requester.
     */
    private static final String ACCEPT = "Accept";

    public static final String UTF_8 = "UTF-8";
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";

    /**
     * The endpoint URI where clients should send their JSON-RPC messages. Defaults to
     * "/mcp".
     */
    private final String mcpEndpoint;

    /**
     * Flag indicating whether DELETE requests are disallowed on the endpoint.
     */
    private final boolean disallowDelete;

    private final ObjectMapper objectMapper;

    private McpStreamableServerSession.Factory sessionFactory;

    /**
     * Map of active client sessions, keyed by mcp-session-id.
     */
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();

    private McpTransportContextExtractor<FullHttpRequest> contextExtractor;

    /**
     * Flag indicating if the transport is shutting down.
     */
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    /**
     * Keep-alive scheduler for managing session pings. Activated if keepAliveInterval is
     * set. Disabled by default.
     */
    private KeepAliveScheduler keepAliveScheduler;

    /**
     * Constructs a new NettyStreamableServerTransportProvider instance.
     * 
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     *                     of messages.
     * @param mcpEndpoint The endpoint URI where clients should send their JSON-RPC
     *                    messages via HTTP.
     * @param disallowDelete Whether to disallow DELETE requests on the endpoint.
     * @param contextExtractor The extractor for transport context from the request.
     * @param keepAliveInterval Interval for keep-alive pings (null to disable)
     * @throws IllegalArgumentException if any parameter is null
     */
    public McpStreamableHttpRequestHandler(ObjectMapper objectMapper, String mcpEndpoint,
                                           boolean disallowDelete, McpTransportContextExtractor<FullHttpRequest> contextExtractor,
                                           Duration keepAliveInterval) {
        this.objectMapper = objectMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.disallowDelete = disallowDelete;
        this.contextExtractor = contextExtractor;

        if (keepAliveInterval != null) {
            this.keepAliveScheduler = KeepAliveScheduler
                    .builder(() -> this.isClosing.get() ? Collections.emptyList() : this.sessions.values())
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();

            this.keepAliveScheduler.start();
            logger.debug("Keep-alive scheduler started with interval: {}ms", keepAliveInterval.toMillis());
        }
    }

    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public CompletableFuture<Void> notifyClients(String method, Object params) {
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return CompletableFuture.completedFuture(null);
        }

        logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());

        return CompletableFuture.runAsync(() -> {
            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.sendNotification(method, params);
                } catch (Exception e) {
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            });
        });
    }

    public CompletableFuture<Void> closeGracefully() {
        return CompletableFuture.runAsync(() -> {
            this.isClosing.set(true);
            logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());

            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.closeGracefully();
                } catch (Exception e) {
                    logger.error("Failed to close session {}: {}", session.getId(), e.getMessage());
                }
            });

            this.sessions.clear();
            logger.debug("Graceful shutdown completed");

            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    protected void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        if (!uri.endsWith(mcpEndpoint)) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Endpoint not found"));
            return;
        }

        if (isClosing.get()) {
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, new McpError("Server is shutting down"));
            return;
        }

        HttpMethod method = request.method();
        if (method == HttpMethod.GET) {
            handleGetRequest(ctx, request);
        } else if (method == HttpMethod.POST) {
            handlePostRequest(ctx, request);
        } else if (method == HttpMethod.DELETE) {
            handleDeleteRequest(ctx, request);
        } else {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("Method not allowed"));
        }
    }

    /**
     * Handles GET requests to establish SSE connections and message replay.
     */
    private void handleGetRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        List<String> badRequestErrors = new ArrayList<>();

        String accept = request.headers().get(ACCEPT);
        if (accept == null || !accept.contains(TEXT_EVENT_STREAM)) {
            badRequestErrors.add("text/event-stream required in Accept header");
        }

        String sessionId = request.headers().get(HttpHeaders.MCP_SESSION_ID);
        if (sessionId == null || sessionId.trim().isEmpty()) {
            badRequestErrors.add("Session ID required in mcp-session-id header");
        }

        if (!badRequestErrors.isEmpty()) {
            String combinedMessage = String.join("; ", badRequestErrors);
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError(combinedMessage));
            return;
        }

        McpStreamableServerSession session = this.sessions.get(sessionId);
        if (session == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Session not found"));
            return;
        }

        logger.debug("Handling GET request for session: {}", sessionId);

        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        Object authSubject = McpAuthExtractor.extractAuthSubjectFromContext(ctx);
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);

        try {
            // Set up SSE response headers
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_EVENT_STREAM);
            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
            response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

            ctx.writeAndFlush(response);

            NettyStreamableMcpSessionTransport sessionTransport = new NettyStreamableMcpSessionTransport(
                    sessionId, ctx);

            // Check if this is a replay request
            String lastEventId = request.headers().get(HttpHeaders.LAST_EVENT_ID);
            if (lastEventId != null) {
                try {
                    // Replay messages from the last event ID
                    try {
                        session.replay(lastEventId).forEach(message -> {
                            try {
                                sessionTransport.sendMessage(message).join();
                            } catch (Exception e) {
                                logger.error("Failed to replay message: {}", e.getMessage());
                                ctx.close();
                            }
                        });
                    } catch (Exception e) {
                        logger.error("Failed to replay messages: {}", e.getMessage());
                        ctx.close();
                    }
                } catch (Exception e) {
                    logger.error("Failed to replay messages: {}", e.getMessage());
                    ctx.close();
                }
            } else {
                // Establish new listening stream
                McpStreamableServerSession.McpStreamableServerSessionStream listeningStream = session
                        .listeningStream(sessionTransport);

                // Handle channel closure
                ctx.channel().closeFuture().addListener(future -> {
                    logger.debug("SSE connection closed for session: {}", sessionId);
                    listeningStream.close();
                });
            }
        } catch (Exception e) {
            logger.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, new McpError("Internal server error"));
        }
    }

    /**
     * Handles POST requests for incoming JSON-RPC messages from clients.
     */
    private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        List<String> badRequestErrors = new ArrayList<>();

        String accept = request.headers().get(ACCEPT);
        if (accept == null || !accept.contains(TEXT_EVENT_STREAM)) {
            badRequestErrors.add("text/event-stream required in Accept header");
        }
        if (accept == null || !accept.contains(APPLICATION_JSON)) {
            badRequestErrors.add("application/json required in Accept header");
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        Object authSubject = McpAuthExtractor.extractAuthSubjectFromContext(ctx);
        transportContext.put(MCP_AUTH_SUBJECT_KEY, authSubject);

        try {
            ByteBuf content = request.content();
            String body = content.toString(CharsetUtil.UTF_8);

            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            // Handle initialization request
            if (message instanceof McpSchema.JSONRPCRequest) {
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                if (jsonrpcRequest.getMethod().equals(McpSchema.METHOD_INITIALIZE)) {
                    if (!badRequestErrors.isEmpty()) {
                        String combinedMessage = String.join("; ", badRequestErrors);
                        sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError(combinedMessage));
                        return;
                    }

                    McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(jsonrpcRequest.getParams(),
                            new TypeReference<McpSchema.InitializeRequest>() {
                            });
                    McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
                            .startSession(initializeRequest);
                    this.sessions.put(init.session().getId(), init.session());

                    try {
                        init.initResult()
                                .thenAccept(initResult -> {
                                    try {
                                        FullHttpResponse response = new DefaultFullHttpResponse(
                                                HttpVersion.HTTP_1_1,
                                                HttpResponseStatus.OK,
                                                Unpooled.copiedBuffer(objectMapper.writeValueAsString(
                                                        new McpSchema.JSONRPCResponse(
                                                                McpSchema.JSONRPC_VERSION, jsonrpcRequest.getId(), initResult, null)),
                                                        CharsetUtil.UTF_8)
                                        );

                                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON);
                                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                        response.headers().set(HttpHeaders.MCP_SESSION_ID, init.session().getId());
                                        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

                                        ctx.writeAndFlush(response);
                                    } catch (Exception e) {
                                        logger.error("Failed to serialize init response: {}", e.getMessage());
                                        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                                new McpError("Failed to serialize response"));
                                    }
                                })
                                .exceptionally(e -> {
                                    logger.error("Failed to initialize session: {}", e.getMessage());
                                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                            new McpError("Failed to initialize session: " + e.getMessage()));
                                    return null;
                                });
                        return;
                    } catch (Exception e) {
                        logger.error("Failed to initialize session: {}", e.getMessage());
                        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                new McpError("Failed to initialize session: " + e.getMessage()));
                        return;
                    }
                }
            }

            String sessionId = request.headers().get(HttpHeaders.MCP_SESSION_ID);
            if (sessionId == null || sessionId.trim().isEmpty()) {
                badRequestErrors.add("Session ID required in mcp-session-id header");
            }

            if (!badRequestErrors.isEmpty()) {
                String combinedMessage = String.join("; ", badRequestErrors);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError(combinedMessage));
                return;
            }

            McpStreamableServerSession session = this.sessions.get(sessionId);
            if (session == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND,
                        new McpError("Session not found: " + sessionId));
                return;
            }

            if (message instanceof McpSchema.JSONRPCResponse) {
                McpSchema.JSONRPCResponse jsonrpcResponse = (McpSchema.JSONRPCResponse) message;
                session.accept(jsonrpcResponse)
                        .thenRun(() -> {
                            FullHttpResponse response = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    HttpResponseStatus.ACCEPTED
                            );
                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                            ctx.writeAndFlush(response);
                        })
                        .exceptionally(e -> {
                            logger.error("Failed to accept response: {}", e.getMessage());
                            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, new McpError("Failed to accept response"));
                            return null;
                        });
            } else if (message instanceof McpSchema.JSONRPCNotification) {
                McpSchema.JSONRPCNotification jsonrpcNotification = (McpSchema.JSONRPCNotification) message;
                session.accept(jsonrpcNotification, transportContext)
                        .thenRun(() -> {
                            FullHttpResponse response = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    HttpResponseStatus.ACCEPTED
                            );
                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                            ctx.writeAndFlush(response);
                        })
                        .exceptionally(e -> {
                            logger.error("Failed to accept notification: {}", e.getMessage());
                            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, new McpError("Failed to accept notification"));
                            return null;
                        });
            } else if (message instanceof McpSchema.JSONRPCRequest) {
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                // For streaming responses, we need to return SSE
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_EVENT_STREAM);
                response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

                ctx.writeAndFlush(response);

                NettyStreamableMcpSessionTransport sessionTransport = new NettyStreamableMcpSessionTransport(
                        sessionId, ctx);

                try {
                    session.responseStream(jsonrpcRequest, sessionTransport, transportContext)
                            .exceptionally(e -> {
                                logger.error("Failed to handle request stream: {}", e.getMessage());
                                ctx.close();
                                return null;
                            });
                } catch (Exception e) {
                    logger.error("Failed to handle request stream: {}", e.getMessage());
                    ctx.close();
                }
            } else {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        new McpError("Unknown message type"));
            }
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                    new McpError("Invalid message format: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    new McpError("Error processing message: " + e.getMessage()));
        }
    }

    /**
     * Handles DELETE requests for session deletion.
     */
    private void handleDeleteRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (this.disallowDelete) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("DELETE method not allowed"));
            return;
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        String sessionId = request.headers().get(HttpHeaders.MCP_SESSION_ID);
        if (sessionId == null) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                    new McpError("Session ID required in mcp-session-id header"));
            return;
        }

        McpStreamableServerSession session = this.sessions.get(sessionId);
        if (session == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Session not found"));
            return;
        }

        try {
            session.delete()
                    .thenRun(() -> {
                        this.sessions.remove(sessionId);
                        FullHttpResponse response = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1,
                                HttpResponseStatus.OK
                        );
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                        ctx.writeAndFlush(response);
                    })
                    .exceptionally(e -> {
                        logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
                        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                new McpError(e.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    new McpError("Error deleting session"));
        }
    }

    /**
     * Sends an error response to the client.
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, McpError mcpError) {
        try {
            String jsonError = objectMapper.writeValueAsString(mcpError);
            ByteBuf content = Unpooled.copiedBuffer(jsonError, CharsetUtil.UTF_8);

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status,
                    content
            );

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

            ctx.writeAndFlush(response);
        } catch (Exception e) {
            logger.error(FAILED_TO_SEND_ERROR_RESPONSE, e.getMessage());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response);
        }
    }

    // Implementation of McpStreamableServerTransport for Netty SSE sessions
    private class NettyStreamableMcpSessionTransport implements McpStreamableServerTransport {

        private final String sessionId;
        private final ChannelHandlerContext ctx;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final ReentrantLock lock = new ReentrantLock();

        NettyStreamableMcpSessionTransport(String sessionId, ChannelHandlerContext ctx) {
            this.sessionId = sessionId;
            this.ctx = ctx;
            logger.debug("Streamable session transport {} initialized", sessionId);
        }

        @Override
        public CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        @Override
        public CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return CompletableFuture.runAsync(() -> {
                if (this.closed.get()) {
                    logger.warn("Attempted to send message to closed session: {}", this.sessionId);
                    return;
                }
                
                // Check if channel is still active
                if (!this.ctx.channel().isActive()) {
                    logger.warn("Channel for session {} is not active, message will not be sent", this.sessionId);
                    return;
                }
                lock.lock();
                try {
                    if (this.closed.get()) {
                        logger.debug("Session {} was closed during message send attempt", this.sessionId);
                        return;
                    }

                    String jsonText = objectMapper.writeValueAsString(message);
                    logger.debug("Sending SSE message to session {}: {}", this.sessionId, 
                        jsonText.length() > 200 ? jsonText.substring(0, 200) + "..." : jsonText);
                    sendSseEvent(MESSAGE_EVENT_TYPE, jsonText, messageId != null ? messageId : this.sessionId);
                    logger.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
                } catch (Exception e) {
                    logger.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
                    this.ctx.close();
                } finally {
                    lock.unlock();
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        @Override
        public CompletableFuture<Void> closeGracefully() {
            return CompletableFuture.runAsync(this::close);
        }

        @Override
        public void close() {
            lock.lock();
            try {
                if (this.closed.get()) {
                    logger.debug("Session transport {} already closed", this.sessionId);
                    return;
                }

                this.closed.set(true);
                if (ctx.channel().isActive()) {
                    ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                            .addListener(ChannelFutureListener.CLOSE);
                }
                logger.debug("Successfully closed session transport {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to close session transport {}: {}", sessionId, e.getMessage());
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Channel getChannel() {
            return ctx.channel();
        }

        private void sendSseEvent(String eventType, String data, String id) {
            StringBuilder sseData = new StringBuilder();
            if (id != null) {
                sseData.append("id: ").append(id).append("\n");
            }
            sseData.append("event: ").append(eventType).append("\n");
            sseData.append("data: ").append(data).append("\n\n");

            ByteBuf buffer = Unpooled.copiedBuffer(sseData.toString(), CharsetUtil.UTF_8);
            this.ctx.writeAndFlush(new DefaultHttpContent(buffer));
            
            logger.debug("SSE event sent - Type: {}, ID: {}, Data length: {}", 
                eventType, id, data != null ? data.length() : 0);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating instances of {@link McpStreamableHttpRequestHandler}.
     */
    public static class Builder {

        private ObjectMapper objectMapper;
        private String mcpEndpoint = "/mcp";
        private boolean disallowDelete = false;
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (serverRequest, context) -> context;
        private Duration keepAliveInterval;

        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder mcpEndpoint(String mcpEndpoint) {
            Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        public Builder disallowDelete(boolean disallowDelete) {
            this.disallowDelete = disallowDelete;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public McpStreamableHttpRequestHandler build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            return new McpStreamableHttpRequestHandler(this.objectMapper, this.mcpEndpoint,
                    this.disallowDelete, this.contextExtractor, this.keepAliveInterval);
        }
    }

}

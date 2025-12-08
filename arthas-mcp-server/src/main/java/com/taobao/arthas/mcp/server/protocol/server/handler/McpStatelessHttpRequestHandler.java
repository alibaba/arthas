/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.protocol.server.DefaultMcpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpStatelessServerHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.util.Assert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-side HTTP request handler for stateless MCP transport.
 * This handler processes HTTP requests without maintaining client sessions.
 *
 */
public class McpStatelessHttpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpStatelessHttpRequestHandler.class);

    public static final String UTF_8 = "UTF-8";
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static final String ACCEPT = "Accept";
    private static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";

    private final ObjectMapper objectMapper;
    private final String mcpEndpoint;
    private McpStatelessServerHandler mcpHandler;
    private final McpTransportContextExtractor<FullHttpRequest> contextExtractor;
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    /**
     * Constructs a new McpStatelessHttpRequestHandler instance.
     * 
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     * @param mcpEndpoint The endpoint URI where clients should send their JSON-RPC messages
     * @param contextExtractor The extractor for transport context from the request
     */
    public McpStatelessHttpRequestHandler(ObjectMapper objectMapper, String mcpEndpoint,
                                         McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
        Assert.notNull(objectMapper, "objectMapper must not be null");
        Assert.notNull(mcpEndpoint, "mcpEndpoint must not be null");
        Assert.notNull(contextExtractor, "contextExtractor must not be null");

        this.objectMapper = objectMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.contextExtractor = contextExtractor;
    }

    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        this.mcpHandler = mcpHandler;
    }

    /**
     * Initiates a graceful shutdown of the handler.
     * 
     * @return A CompletableFuture that completes when shutdown is initiated
     */
    public CompletableFuture<Void> closeGracefully() {
        return CompletableFuture.supplyAsync(() -> {
            this.isClosing.set(true);
            return null;
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
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("GET method not allowed for stateless transport"));
        } else if (method == HttpMethod.POST) {
            handlePostRequest(ctx, request);
        } else {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, new McpError("Only POST method is supported"));
        }
    }

    /**
     * Handles POST requests for incoming JSON-RPC messages from clients.
     */
    private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        Object authSubject = McpAuthExtractor.extractAuthSubjectFromContext(ctx);
        transportContext.put(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY, authSubject);

        // 从 HTTP header 中提取 User ID
        String userId = McpAuthExtractor.extractUserIdFromRequest(request);
        transportContext.put(McpAuthExtractor.MCP_USER_ID_KEY, userId);

        String accept = request.headers().get(ACCEPT);
        if (accept == null || !(accept.contains(APPLICATION_JSON) && accept.contains(TEXT_EVENT_STREAM))) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                    new McpError("Both application/json and text/event-stream required in Accept header"));
            return;
        }

        try {
            ByteBuf content = request.content();
            String body = content.toString(CharsetUtil.UTF_8);

            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            if (message instanceof McpSchema.JSONRPCRequest) {
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                try {
                    this.mcpHandler.handleRequest(transportContext, jsonrpcRequest)
                            .thenAccept(jsonrpcResponse -> {
                                try {
                                    FullHttpResponse response = new DefaultFullHttpResponse(
                                            HttpVersion.HTTP_1_1,
                                            HttpResponseStatus.OK,
                                            Unpooled.copiedBuffer(objectMapper.writeValueAsString(jsonrpcResponse), CharsetUtil.UTF_8)
                                    );

                                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON);
                                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

                                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                } catch (Exception e) {
                                    logger.error("Failed to serialize response: {}", e.getMessage());
                                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                            new McpError("Failed to serialize response: " + e.getMessage()));
                                }
                            })
                            .exceptionally(e -> {
                                logger.error("Failed to handle request: {}", e.getMessage());
                                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                        new McpError("Failed to handle request: " + e.getMessage()));
                                return null;
                            });
                } catch (Exception e) {
                    logger.error("Failed to handle request: {}", e.getMessage());
                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            new McpError("Failed to handle request: " + e.getMessage()));
                }
            } else if (message instanceof McpSchema.JSONRPCNotification) {
                McpSchema.JSONRPCNotification jsonrpcNotification = (McpSchema.JSONRPCNotification) message;
                try {
                    this.mcpHandler.handleNotification(transportContext, jsonrpcNotification)
                            .thenRun(() -> {
                                FullHttpResponse response = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.ACCEPTED
                                );
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                            })
                            .exceptionally(e -> {
                                logger.error("Failed to handle notification: {}", e.getMessage());
                                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                        new McpError("Failed to handle notification: " + e.getMessage()));
                                return null;
                            });
                } catch (Exception e) {
                    logger.error("Failed to handle notification: {}", e.getMessage());
                    sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            new McpError("Failed to handle notification: " + e.getMessage()));
                }
            } else {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST,
                        new McpError("The server accepts either requests or notifications"));
            }
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, new McpError("Invalid message format"));
        } catch (Exception e) {
            logger.error("Unexpected error handling message: {}", e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    new McpError("Unexpected error: " + e.getMessage()));
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

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            logger.error(FAILED_TO_SEND_ERROR_RESPONSE, e.getMessage());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

}

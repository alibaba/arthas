package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpServerSession;
import com.taobao.arthas.mcp.server.protocol.spec.McpServerTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) Request Handler
 * Used to handle model context protocol requests for Arthas.
 *
 * @author Yeaury
 */
public class McpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpRequestHandler.class);

    /** Message event type */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /** Endpoint event type */
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    /** Active session mapping: session ID -> session object */
    private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    /** SSE connection mapping: session ID -> Channel */
    private final Map<String, Channel> sseChannels = new ConcurrentHashMap<>();

    /** Message endpoint path */
    private final String messageEndpoint;

    /** SSE endpoint path */
    private final String sseEndpoint;

    /** Object mapper for JSON serialization/deserialization */
    private final ObjectMapper objectMapper;

    /** Session factory */
    private McpServerSession.Factory sessionFactory;

    /**
     * Create a new McpRequestHandler instance.
     * @param messageEndpoint Message endpoint path, e.g. "/mcp/message"
     * @param sseEndpoint SSE endpoint path, e.g. "/mcp"
     * @param objectMapper Object mapper for JSON serialization/deserialization
     */
    public McpRequestHandler(String messageEndpoint,
                             String sseEndpoint, ObjectMapper objectMapper) {
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.objectMapper = objectMapper;
    }

    public void handle(ChannelHandlerContext ctx, FullHttpRequest request){
        String uri = request.uri();

        if (request.method() == HttpMethod.GET && uri.endsWith(sseEndpoint)) {
            handleSseRequest(ctx);
            return;
        }

        if (request.method() == HttpMethod.POST && uri.contains(messageEndpoint)) {
            handleMessageRequest(ctx, request);
            return;
        }

        if (request.method() == HttpMethod.OPTIONS) {
            sendOptionsResponse(ctx);
            return;
        }

        sendNotFoundResponse(ctx);
    }

    private void handleSseRequest(ChannelHandlerContext ctx) {
        String sessionId = UUID.randomUUID().toString();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");

        HttpUtil.setTransferEncodingChunked(response, true);
        ctx.writeAndFlush(response);

        sseChannels.put(sessionId, ctx.channel());

        HttpServerTransport transport = new HttpServerTransport(sessionId, ctx.channel());

        McpServerSession session = sessionFactory.create(transport);
        sessions.put(sessionId, session);

        String endpointInfo = messageEndpoint + "?sessionId=" + sessionId;
        sendSseEvent(ctx, ENDPOINT_EVENT_TYPE, endpointInfo);

        logger.debug("SSE connection established, session ID: {}", sessionId);
    }

    private void handleMessageRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String sessionIdParam = "sessionId=";
        int sessionIdStart = uri.indexOf(sessionIdParam);

        if (sessionIdStart == -1) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Missing sessionId parameter");
            return;
        }
        String sessionId = uri.substring(sessionIdStart + sessionIdParam.length());

        McpServerSession session = sessions.get(sessionId);
        if (session == null) {
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Session not found");
            return;
        }

        ByteBuf content = request.content();
        String jsonContent = content.toString(CharsetUtil.UTF_8);
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, jsonContent);

            session.handle(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Error processing message", ex);
                    sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
                }
                else {
                    sendSuccessResponse(ctx);
                }
            });
        }
        catch (Exception e) {
            logger.error("Error parsing message", e);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid message format");
        }
    }

    private void sendSseEvent(ChannelHandlerContext ctx, String eventType, String data) {
        StringBuilder eventBuilder = new StringBuilder();
        eventBuilder.append("event: ").append(eventType).append("\n");
        eventBuilder.append("data: ").append(data).append("\n\n");

        ByteBuf buffer = Unpooled.copiedBuffer(eventBuilder.toString(), CharsetUtil.UTF_8);
        ctx.writeAndFlush(new DefaultHttpContent(buffer));
    }

    private void sendOptionsResponse(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendSuccessResponse(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        ByteBuf content = Unpooled.copiedBuffer("{\"error\":\"" + message + "\"}", CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendNotFoundResponse(ChannelHandlerContext ctx) {
        ByteBuf content = Unpooled.copiedBuffer("{\"error\":\"Resource not found\"}", CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND,
                content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public CompletableFuture<Void> notifyClients(String method, Object params) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message");
            future.complete(null);
            return future;
        }

        logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

        CompletableFuture<?>[] futures = sessions.values()
                .stream()
                .map(session -> session.sendNotification(method, params).exceptionally(e -> {
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                    return null;
                }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Error broadcasting message", ex);
                future.completeExceptionally(ex);
            }
            else {
                future.complete(null);
            }
        });

        return future;
    }

    public CompletableFuture<Void> closeGracefully() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            this.shutdown();
            future.complete(null);
        }
        catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private void shutdown() {
        for (McpServerSession session : this.sessions.values()) {
            try {
                session.close();
            }
            catch (Exception e) {
                logger.warn("Error closing session: {}", e.getMessage());
            }
        }
        logger.info("MCP HTTP server closed");
    }

    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public String getMessageEndpoint() {
        return messageEndpoint;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    private class HttpServerTransport implements McpServerTransport {

        private final String sessionId;

        private final Channel channel;

        public HttpServerTransport(String sessionId, Channel channel) {
            this.sessionId = sessionId;
            this.channel = channel;
        }

        public String getSessionId() {
            return sessionId;
        }

        @Override
        public Channel getChannel() {
            return channel;
        }

        @Override
        public CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            CompletableFuture<Void> future = new CompletableFuture<>();

            if (!channel.isActive()) {
                future.completeExceptionally(new RuntimeException("Channel is not active"));
                return future;
            }

            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                logger.debug("sendMessage: {}", jsonMessage);

                StringBuilder eventBuilder = new StringBuilder();
                eventBuilder.append("event: ").append(MESSAGE_EVENT_TYPE).append("\n");
                eventBuilder.append("data: ").append(jsonMessage).append("\n\n");

                ByteBuf buffer = Unpooled.copiedBuffer(eventBuilder.toString(), CharsetUtil.UTF_8);

                channel.writeAndFlush(new DefaultHttpContent(buffer))
                    .addListener((ChannelFutureListener) channelFuture -> {
                        if (channelFuture.isSuccess()) {
                            future.complete(null);
                        }
                        else {
                            future.completeExceptionally(channelFuture.cause());
                        }
                    });
            }
            catch (Exception e) {
                logger.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                sessions.remove(sessionId);
                future.completeExceptionally(e);
            }

            return future;
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        @Override
        public CompletableFuture<Void> closeGracefully() {
            CompletableFuture<Void> future = new CompletableFuture<>();

            try {
                sessions.remove(sessionId);
                sseChannels.remove(sessionId);

                if (channel.isActive()) {
                    channel.close().addListener((ChannelFutureListener) channelFuture -> {
                        if (channelFuture.isSuccess()) {
                            future.complete(null);
                        }
                        else {
                            future.completeExceptionally(channelFuture.cause());
                        }
                    });
                }
                else {
                    future.complete(null);
                }
            }
            catch (Exception e) {
                future.completeExceptionally(e);
            }

            return future;
        }

        @Override
        public void close() {
            sessions.remove(sessionId);
            sseChannels.remove(sessionId);

            if (channel.isActive()) {
                channel.close();
            }
        }

    }

}

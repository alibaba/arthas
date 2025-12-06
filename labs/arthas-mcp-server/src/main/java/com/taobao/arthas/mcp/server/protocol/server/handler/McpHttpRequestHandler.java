package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.config.McpServerProperties.ServerProtocol;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContextExtractor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.util.Assert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.taobao.arthas.mcp.server.protocol.spec.McpSchema.*;

/**
 * MCP HTTP请求处理器，分发请求到无状态或流式处理器。
 * 
 * @author Yeaury
 */
public class McpHttpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpHttpRequestHandler.class);

    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final String ACCEPT_HEADER = "Accept";

    private final String mcpEndpoint;
    private final ObjectMapper objectMapper;
    private final McpTransportContextExtractor<FullHttpRequest> contextExtractor;
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    private McpStatelessHttpRequestHandler statelessHandler;

    private McpStreamableHttpRequestHandler streamableHandler;

    private ServerProtocol protocol;

    public McpHttpRequestHandler(String mcpEndpoint, ObjectMapper objectMapper,
                                McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
        Assert.notNull(mcpEndpoint, "mcpEndpoint must not be null");
        Assert.notNull(objectMapper, "objectMapper must not be null");
        Assert.notNull(contextExtractor, "contextExtractor must not be null");

        this.mcpEndpoint = mcpEndpoint;
        this.objectMapper = objectMapper;
        this.contextExtractor = contextExtractor;
    }

    public void setProtocol(ServerProtocol protocol) {
        this.protocol = protocol;
    }

    public void setStatelessHandler(McpStatelessHttpRequestHandler statelessHandler) {
        this.statelessHandler = statelessHandler;
    }

    public void setStreamableHandler(McpStreamableHttpRequestHandler streamableHandler) {
        this.streamableHandler = streamableHandler;
    }

    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        if (!uri.endsWith(mcpEndpoint)) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, new McpError("Endpoint not found"));
            return;
        }

        if (isClosing.get()) {
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, new McpError("Server is shutting down"));
            return;
        }

        logger.debug("Request {} {} -> using {} transport",
            request.method(), request.uri(), protocol);

        try {
            if (protocol == ServerProtocol.STREAMABLE) {
                if (streamableHandler == null) {
                    sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE,
                        new McpError("Streamable transport handler not available"));
                    return;
                }
                streamableHandler.handle(ctx, request);
            } else {
                if (statelessHandler == null) {
                    sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE,
                        new McpError("Stateless transport handler not available"));
                    return;
                }
                statelessHandler.handle(ctx, request);
            }
        } catch (Exception e) {
            logger.error("Error handling request: {}", e.getMessage(), e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                new McpError("Error processing request: " + e.getMessage()));
        }
    }


    public CompletableFuture<Void> closeGracefully() {
        return CompletableFuture.runAsync(() -> {
            this.isClosing.set(true);
            logger.debug("Initiating graceful shutdown of MCP handler");

            CompletableFuture<Void> statelessClose = CompletableFuture.completedFuture(null);
            CompletableFuture<Void> streamableClose = CompletableFuture.completedFuture(null);

            if (statelessHandler != null) {
                statelessClose = statelessHandler.closeGracefully();
            }

            if (streamableHandler != null) {
                streamableClose = streamableHandler.closeGracefully();
            }

            CompletableFuture.allOf(statelessClose, streamableClose).join();
            logger.debug("Graceful shutdown completed");
        });
    }

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
            logger.error("Failed to send error response: {}", e.getMessage());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response);
        }
    }

    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String mcpEndpoint = "/mcp";
        private ObjectMapper objectMapper;
        private McpTransportContextExtractor<FullHttpRequest> contextExtractor = (request, context) -> context;
        private ServerProtocol protocol;

        public Builder mcpEndpoint(String mcpEndpoint) {
            Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<FullHttpRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder protocol(ServerProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public McpHttpRequestHandler build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

            if (this.protocol == null) {
                this.protocol = ServerProtocol.STATELESS;
            }

            McpHttpRequestHandler handler = new McpHttpRequestHandler(this.mcpEndpoint, this.objectMapper, this.contextExtractor);
            handler.setProtocol(this.protocol);
            return handler;
        }
    }
}

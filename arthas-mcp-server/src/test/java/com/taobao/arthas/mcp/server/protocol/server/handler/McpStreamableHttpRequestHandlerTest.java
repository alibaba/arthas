/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.spec.DefaultMcpStreamableServerSessionFactory;
import com.taobao.arthas.mcp.server.protocol.spec.HttpHeaders;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class McpStreamableHttpRequestHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MCP_ENDPOINT = "/mcp";

    @Test
    void shouldSendHeartbeatCommentWhilePostResponseIsPending() throws Exception {
        CompletableFuture<Object> pendingToolCall = new CompletableFuture<>();
        McpStreamableHttpRequestHandler handler = newHandler(Duration.ofMillis(10), pendingToolCall);
        String sessionId = initializeSession(handler);

        EmbeddedChannel channel = newChannel(handler);
        channel.writeInbound(postRequest(sessionId, new McpSchema.JSONRPCRequest(
                McpSchema.JSONRPC_VERSION,
                McpSchema.METHOD_TOOLS_CALL,
                "call-1",
                new McpSchema.CallToolRequest("slow", Collections.emptyMap(), null))));

        Object headers = channel.readOutbound();
        assertThat(headers).isInstanceOf(HttpResponse.class);
        ReferenceCountUtil.release(headers);
        Object immediateContent = channel.readOutbound();
        assertThat(immediateContent).isNull();

        channel.advanceTimeBy(11, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();

        HttpContent heartbeat = readOutbound(channel, HttpContent.class);
        assertThat(heartbeat.content().toString(CharsetUtil.UTF_8)).isEqualTo(": keepalive\n\n");
        ReferenceCountUtil.release(heartbeat);

        channel.close();
        channel.finishAndReleaseAll();
    }

    private static McpStreamableHttpRequestHandler newHandler(Duration keepAliveInterval,
                                                              CompletableFuture<Object> pendingToolCall) {
        McpStreamableHttpRequestHandler handler = McpStreamableHttpRequestHandler.builder()
                .objectMapper(OBJECT_MAPPER)
                .mcpEndpoint(MCP_ENDPOINT)
                .keepAliveInterval(keepAliveInterval)
                .build();

        Map<String, McpRequestHandler<?>> requestHandlers = new HashMap<>();
        McpRequestHandler<Object> toolCallHandler = (exchange, commandContext, params) -> pendingToolCall;
        requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolCallHandler);

        handler.setSessionFactory(new DefaultMcpStreamableServerSessionFactory(
                Duration.ofSeconds(30),
                initializeRequest -> CompletableFuture.completedFuture(new McpSchema.InitializeResult(
                        initializeRequest.getProtocolVersion(),
                        McpSchema.ServerCapabilities.builder().build(),
                        new McpSchema.Implementation("test-server", "1.0.0"),
                        null)),
                requestHandlers,
                Collections.emptyMap(),
                new StubCommandExecutor(),
                null,
                null));
        return handler;
    }

    private static String initializeSession(McpStreamableHttpRequestHandler handler) throws Exception {
        EmbeddedChannel channel = newChannel(handler);
        McpSchema.InitializeRequest initializeRequest = new McpSchema.InitializeRequest(
                "2024-11-05",
                new McpSchema.ClientCapabilities(null, null, null, null),
                new McpSchema.Implementation("test-client", "1.0.0"));

        channel.writeInbound(postRequest(null, new McpSchema.JSONRPCRequest(
                McpSchema.JSONRPC_VERSION,
                McpSchema.METHOD_INITIALIZE,
                "init-1",
                initializeRequest)));

        FullHttpResponse response = readOutbound(channel, FullHttpResponse.class);
        String sessionId = response.headers().get(HttpHeaders.MCP_SESSION_ID);
        assertThat(sessionId).isNotBlank();
        ReferenceCountUtil.release(response);
        channel.finishAndReleaseAll();
        return sessionId;
    }

    private static EmbeddedChannel newChannel(McpStreamableHttpRequestHandler handler) {
        return new EmbeddedChannel(new SimpleChannelInboundHandler<FullHttpRequest>(false) {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
                handler.handle(ctx, request);
            }
        });
    }

    private static DefaultFullHttpRequest postRequest(String sessionId, McpSchema.JSONRPCMessage message)
            throws Exception {
        byte[] body = OBJECT_MAPPER.writeValueAsBytes(message);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                MCP_ENDPOINT,
                Unpooled.wrappedBuffer(body));
        request.headers().set(HttpHeaderNames.ACCEPT, "application/json, text/event-stream");
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        if (sessionId != null) {
            request.headers().set(HttpHeaders.MCP_SESSION_ID, sessionId);
        }
        return request;
    }

    private static <T> T readOutbound(EmbeddedChannel channel, Class<T> type) {
        Object message = channel.readOutbound();
        assertThat(message).isInstanceOf(type);
        return type.cast(message);
    }

    private static final class StubCommandExecutor implements CommandExecutor {

        @Override
        public Map<String, Object> executeSync(String commandLine, long timeout, String sessionId, Object authSubject,
                                               String userId) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> executeAsync(String commandLine, String sessionId) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> pullResults(String sessionId, String consumerId) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> interruptJob(String sessionId) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> createSession(boolean quiet) {
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", "arthas-session-1");
            result.put("consumerId", "consumer-1");
            return result;
        }

        @Override
        public Map<String, Object> closeSession(String sessionId) {
            return Collections.emptyMap();
        }

        @Override
        public void setSessionAuth(String sessionId, Object authSubject) {
        }

        @Override
        public void setSessionUserId(String sessionId, String userId) {
        }
    }
}

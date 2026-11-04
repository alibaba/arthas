/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.spec.HttpHeaders;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpStreamableHttpRequestHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MCP_ENDPOINT = "/mcp";
    private static final String TEXT_EVENT_STREAM = "text/event-stream";

    @Test
    void getWithLastEventIdShouldReturn404Immediately() {
        McpStreamableHttpRequestHandler handler = newHandler();

        EmbeddedChannel channel = newChannel(handler);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, MCP_ENDPOINT);
        request.headers().set(HttpHeaderNames.ACCEPT, TEXT_EVENT_STREAM);
        // Guard runs before session lookup; any session id is sufficient.
        request.headers().set(HttpHeaders.MCP_SESSION_ID, "any-session");
        // Cherry Studio reconnects with last-event-id after the streamable response closes.
        request.headers().set(HttpHeaders.LAST_EVENT_ID, "event-1");

        channel.writeInbound(request);

        FullHttpResponse response = readOutbound(channel, FullHttpResponse.class);
        assertThat(response.status()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        assertThat(response.content().toString(CharsetUtil.UTF_8))
                .contains("please re-initialize");
        ReferenceCountUtil.release(response);

        // Must not leave a hanging SSE stream open for the client to time out on.
        assertThat((Object) channel.readOutbound()).isNull();
        assertThat(channel.isActive()).isFalse();
        channel.finishAndReleaseAll();
    }

    @Test
    void getWithLastEventIdIsCaseInsensitive() {
        McpStreamableHttpRequestHandler handler = newHandler();

        EmbeddedChannel channel = newChannel(handler);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, MCP_ENDPOINT);
        request.headers().set(HttpHeaderNames.ACCEPT, TEXT_EVENT_STREAM);
        request.headers().set(HttpHeaders.MCP_SESSION_ID, "any-session");
        request.headers().set("Last-Event-ID", "event-1");

        channel.writeInbound(request);

        FullHttpResponse response = readOutbound(channel, FullHttpResponse.class);
        assertThat(response.status()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        ReferenceCountUtil.release(response);
        assertThat(channel.isActive()).isFalse();
        channel.finishAndReleaseAll();
    }

    private static McpStreamableHttpRequestHandler newHandler() {
        return McpStreamableHttpRequestHandler.builder()
                .objectMapper(OBJECT_MAPPER)
                .mcpEndpoint(MCP_ENDPOINT)
                .build();
    }

    private static EmbeddedChannel newChannel(McpStreamableHttpRequestHandler handler) {
        return new EmbeddedChannel(new SimpleChannelInboundHandler<FullHttpRequest>(false) {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
                handler.handle(ctx, request);
            }
        });
    }

    private static <T> T readOutbound(EmbeddedChannel channel, Class<T> type) {
        Object message = channel.readOutbound();
        assertThat(message).isInstanceOf(type);
        return type.cast(message);
    }
}

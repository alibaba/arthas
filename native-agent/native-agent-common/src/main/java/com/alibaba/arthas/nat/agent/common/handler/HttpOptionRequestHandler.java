package com.alibaba.arthas.nat.agent.common.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

/**
 * @description: HttpOptionRequestHandler
 * @author：flzjkl
 * @date: 2024-09-22 7:21
 */
public class HttpOptionRequestHandler {

    public FullHttpResponse handleOptionsRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                request.getProtocolVersion(),
                HttpResponseStatus.OK,
                Unpooled.EMPTY_BUFFER);

        // Set the CORS response header
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        if (origin != null) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        } else {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, 3600L);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization, X-Requested-With, Accept, Origin");

        // If the request contains an Access-Control-Request-Method, a response is required
        String accessControlRequestMethod = request.headers().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
        if (accessControlRequestMethod != null) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, accessControlRequestMethod);
        }

        // If the request contains Access-Control-Request-Headers, a response is required
        String accessControlRequestHeaders = request.headers().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS);
        if (accessControlRequestHeaders != null) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders);
        }

        return response;
    }

}

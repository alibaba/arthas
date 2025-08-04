package com.taobao.arthas.grpcweb.proxy;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * TODO 支持让用户配置更精细的 cors header
 * @author hengyunabc 2023-09-07
 *
 */
public class CorsUtils {

    public static void updateCorsHeader(HttpHeaders headers) {
//        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS,
//                StringUtils.joinWith(",", "user-agent", "cache-control", "content-type", "content-transfer-encoding",
//                        "grpc-timeout", "keep-alive"));
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");

        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        headers.set(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-grpc-web,x-user-agent");
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS,GET,POST,HEAD");

//        headers.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS,
//                StringUtils.joinWith(",", "grpc-status", "grpc-message"));
        headers.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
    }
}

package com.taobao.arthas.core.util;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.io.UnsupportedEncodingException;
import java.util.Set;

/**
 * @author gongdewei 2020/3/31
 */
public class HttpUtils {

    /**
     * Get cookie value by name
     * @param cookies request cookies
     * @param cookieName the cookie name
     */
    public static String getCookieValue(Set<Cookie> cookies, String cookieName) {
        for (Cookie cookie : cookies) {
            if(cookie.name().equals(cookieName)){
                return cookie.value();
            }
        }
        return null;
    }

    /**
     *
     * @param response
     * @param name
     * @param value
     */
    public static void setCookie(DefaultFullHttpResponse response, String name, String value) {
        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(name, value));
    }

    /**
     * Create http response with status code and content
     * @param request request
     * @param status response status code
     * @param content response content
     */
    public static DefaultHttpResponse createResponse(FullHttpRequest request, HttpResponseStatus status, String content) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        try {
            response.content().writeBytes(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        return response;
    }

    public static HttpResponse createRedirectResponse(FullHttpRequest request, String url) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, url);
        return response;
    }
}

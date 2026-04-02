package com.taobao.arthas.core.util;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.io.UnsupportedEncodingException;
import java.util.Set;

/**
 * HTTP 工具类
 * 提供 HTTP 请求和响应处理相关的工具方法，包括 Cookie 操作、响应创建等
 * 基于 Netty 的 HTTP 实现
 *
 * @author gongdewei 2020/3/31
 */
public class HttpUtils {

    /**
     * 根据名称获取 Cookie 值
     * 从 Cookie 集合中查找指定名称的 Cookie 并返回其值
     *
     * @param cookies 请求中的 Cookie 集合
     * @param cookieName 要查找的 Cookie 名称
     * @return 找到的 Cookie 值，如果未找到则返回 null
     */
    public static String getCookieValue(Set<Cookie> cookies, String cookieName) {
        // 遍历 Cookie 集合
        for (Cookie cookie : cookies) {
            // 比较 Cookie 名称
            if(cookie.name().equals(cookieName)){
                return cookie.value();
            }
        }
        return null;
    }

    /**
     * 设置 Cookie 到 HTTP 响应
     * 将指定的键值对作为 Cookie 添加到响应头中
     *
     * @param response HTTP 响应对象
     * @param name Cookie 名称
     * @param value Cookie 值
     */
    public static void setCookie(DefaultFullHttpResponse response, String name, String value) {
        // 使用严格模式编码 Cookie 并添加到响应头
        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(name, value));
    }

    /**
     * 创建带有状态码和内容的 HTTP 响应
     * 根据请求的协议版本生成响应，并设置响应内容
     *
     * @param request HTTP 请求对象，用于获取协议版本
     * @param status 响应状态码
     * @param content 响应内容
     * @return 创建的完整 HTTP 响应对象
     */
    public static DefaultHttpResponse createResponse(FullHttpRequest request, HttpResponseStatus status, String content) {
        // 创建完整响应对象，使用请求的协议版本
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), status);
        // 设置内容类型为 HTML，使用 UTF-8 编码
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        try {
            // 将内容转换为 UTF-8 字节数组并写入响应内容
            response.content().writeBytes(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 编码应该始终支持，此处不应抛出异常
        }
        return response;
    }

    /**
     * 创建 HTTP 重定向响应
     * 生成 302 重定向响应，将客户端重定向到指定 URL
     *
     * @param request HTTP 请求对象，用于获取协议版本
     * @param url 重定向的目标 URL
     * @return 重定向响应对象，状态码为 302 FOUND
     */
    public static HttpResponse createRedirectResponse(FullHttpRequest request, String url) {
        // 创建 302 重定向响应
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.FOUND);
        // 设置 Location 头部为重定向目标 URL
        response.headers().set(HttpHeaderNames.LOCATION, url);
        return response;
    }
}

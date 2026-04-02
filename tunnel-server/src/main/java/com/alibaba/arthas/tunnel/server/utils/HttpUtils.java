package com.alibaba.arthas.tunnel.server.utils;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * HTTP 工具类
 * <p>
 * 提供从 HTTP 请求头中提取客户端信息的工具方法。
 * 主要用于在反向代理场景下获取真实的客户端 IP 和端口信息。
 * </p>
 * <p>
 * 通常在 Tunnel Server 前部署 Nginx 等反向代理时，
 * 需要通过特定的请求头来获取客户端的真实 IP 和端口。
 * </p>
 *
 * @author hengyunabc 2021-02-26
 *
 */
public class HttpUtils {

    /**
     * 从 HTTP 请求头中查找客户端真实 IP 地址
     * <p>
     * 该方法从 X-Forwarded-For 请求头中获取客户端 IP。
     * X-Forwarded-For 格式通常为：clientIP, proxy1IP, proxy2IP
     * 该方法返回第一个 IP 地址，即真实的客户端 IP。
     * </p>
     *
     * @param headers HTTP 请求头
     * @return 客户端 IP 地址，如果未找到则返回 null
     */
    public static String findClientIP(HttpHeaders headers) {
        // 获取 X-Forwarded-For 请求头
        // 该头通常由反向代理（如 Nginx）添加
        String hostStr = headers.get("X-Forwarded-For");
        if (hostStr == null) {
            // 如果请求头不存在，返回 null
            return null;
        }

        // X-Forwarded-For 可能包含多个 IP，用逗号分隔
        // 第一个 IP 是客户端的真实 IP
        int index = hostStr.indexOf(',');
        if (index > 0) {
            // 只取第一个 IP 地址
            hostStr = hostStr.substring(0, index);
        }

        return hostStr;
    }

    /**
     * 从 HTTP 请求头中查找客户端真实端口
     * <p>
     * 该方法从 X-Real-Port 请求头中获取客户端端口号。
     * 这是一个自定义的请求头，需要由反向代理配置添加。
     * </p>
     *
     * @param headers HTTP 请求头
     * @return 客户端端口号，如果未找到则返回 null
     */
    public static Integer findClientPort(HttpHeaders headers) {
        // 获取 X-Real-Port 请求头
        // 这是一个自定义头，用于传递客户端的真实端口
        String portStr = headers.get("X-Real-Port");
        if (portStr != null) {
            // 将端口号字符串转换为整数并返回
            return Integer.parseInt(portStr);
        }

        // 如果请求头不存在，返回 null
        return null;
    }
}

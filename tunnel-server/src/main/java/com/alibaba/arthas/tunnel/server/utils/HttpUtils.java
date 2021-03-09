package com.alibaba.arthas.tunnel.server.utils;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * 
 * @author hengyunabc 2021-02-26
 *
 */
public class HttpUtils {

    public static String findClientIP(HttpHeaders headers) {
        String hostStr = headers.get("X-Forwarded-For");
        if (hostStr == null) {
            return null;
        }
        int index = hostStr.indexOf(',');
        if (index > 0) {
            hostStr = hostStr.substring(0, index);
        }
        return hostStr;
    }

    public static Integer findClientPort(HttpHeaders headers) {
        String portStr = headers.get("X-Real-Port");
        if (portStr != null) {
            return Integer.parseInt(portStr);
        }
        return null;
    }
}

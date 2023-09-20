package com.taobao.arthas.grpcweb.proxy.util;
public class StringUtils {
    public static String joinWith(String delimiter, String... strings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            sb.append(strings[i]);
            if (i < strings.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }
}
package com.alibaba.arthas.tunnel.common.remote.request;


import com.alibaba.arthas.tunnel.common.remote.Payload;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author qiyue.zhang@aloudata.com
 * @description Request
 * @date 2023/6/15 17:54
 */
public abstract class Request implements Payload {
    
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    private String requestId;
    
    /**
     * put header.
     *
     * @param key   key of value.
     * @param value value.
     */
    public void putHeader(String key, String value) {
        headers.put(key, value);
    }
    
    /**
     * put headers .
     *
     * @param headers headers to put.
     */
    public void putAllHeader(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        this.headers.putAll(headers);
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void clearHeaders() {
        this.headers.clear();
    }
}

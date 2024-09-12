package com.taobao.arthas.grpc.server.handler;/**
 * @author: 風楪
 * @date: 2024/9/5 02:05
 */

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/9/5 02:05
 * @description: GrpcResponse
 */
public class GrpcResponse {

    private Map<String, String> headers;

    /**
     * 二进制数据
     */
    private ByteBuf byteData;

    /**
     * 响应类型
     */
    private Class<?> clazz;

    {
        headers = new HashMap<>();
        headers.put("content-type", "application/grpc");
        headers.put("grpc-encoding", "identity");
        headers.put("grpc-accept-encoding", "identity,deflate,gzip");
    }

    public Http2Headers getEndHeader() {
        Http2Headers endHeader = new DefaultHttp2Headers().status("200");
        headers.forEach(endHeader::set);
        return endHeader;
    }

    public Http2Headers getEndStreamHeader() {
        return new DefaultHttp2Headers().set("grpc-status", "0");
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public ByteBuf getByteData() {
        return byteData;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setByteData(ByteBuf byteData) {
        this.byteData = byteData;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }
}

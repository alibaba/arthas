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
public class GrpcResponse<T> {

    private Map<String, String> headers;

    private ByteBuf data;

    private T genericsData;

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
}

package com.taobao.arthas.grpc.server.handler;


import arthas.grpc.common.ArthasGrpc;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/9/5 02:05
 * @description: GrpcResponse
 */
public class GrpcResponse<T> {

    private Map<String, String> headers;

    /**
     * 请求的 service
     */
    private String service;

    /**
     * 请求的 method
     */
    private String method;

    /**
     * 二进制数据
     */
    private ByteBuf byteData;

    /**
     * 响应class
     */
    private Class<?> clazz;

    {
        headers = new HashMap<>();
        headers.put("content-type", "application/grpc");
        headers.put("grpc-encoding", "identity");
        headers.put("grpc-accept-encoding", "identity,deflate,gzip");
    }

    public GrpcResponse() {
    }

    public GrpcResponse(Method method) {
        this.service = method.getDeclaringClass().getAnnotation(GrpcService.class).value();
        this.method = method.getAnnotation(GrpcMethod.class).value();
    }

    public Http2Headers getEndHeader() {
        Http2Headers endHeader = new DefaultHttp2Headers().status("200");
        headers.forEach(endHeader::set);
        return endHeader;
    }

    public Http2Headers getEndStreamHeader() {
        return new DefaultHttp2Headers().set("grpc-status", "0");
    }

    public static Http2Headers getDefaultEndStreamHeader() {
        return new DefaultHttp2Headers().set("grpc-status", "0");
    }

    public ByteBuf getResponseData() {
        return byteData;
    }

    public void writeResponseData(Object response) {
        byte[] encode = null;
        try {
            if (ArthasGrpc.ErrorRes.class.equals(clazz)) {
                encode = ((ArthasGrpc.ErrorRes) response).toByteArray();
            } else {
                encode = (byte[]) GrpcDispatcher.responseToByteArrayMap.get(GrpcDispatcher.generateGrpcMethodKey(service, method)).invoke(response);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        this.byteData = ByteUtil.newByteBuf();
        this.byteData.writeBoolean(false);
        this.byteData.writeInt(encode.length);
        this.byteData.writeBytes(encode);
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}

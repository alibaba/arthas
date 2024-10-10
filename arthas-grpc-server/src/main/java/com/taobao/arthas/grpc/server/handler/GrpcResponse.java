package com.taobao.arthas.grpc.server.handler;

import com.taobao.arthas.grpc.server.protobuf.ProtobufCodec;
import com.taobao.arthas.grpc.server.protobuf.ProtobufProxy;
import com.taobao.arthas.grpc.server.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
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
     * 响应class
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

    public ByteBuf getResponseData() {
        return byteData;
    }

    public void writeResponseData(Object response) {
        ProtobufCodec codec = ProtobufProxy.getCodecCacheSide(clazz);
        byte[] encode = null;
        try {
            encode = codec.encode(response);
        } catch (IOException e) {
            throw new RuntimeException("ProtobufCodec encode error");
        }
        this.byteData = ByteUtil.newByteBuf();
        this.byteData.writeBoolean(false);
        this.byteData.writeInt(encode.length);
        this.byteData.writeBytes(encode);
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }
}

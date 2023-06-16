package com.alibaba.arthas.tunnel.common.util;

import com.alibaba.arthas.tunnel.common.grpc.auto.Metadata;
import com.alibaba.arthas.tunnel.common.grpc.auto.Payload;
import com.alibaba.arthas.tunnel.common.remote.exception.ArthasException;
import com.alibaba.arthas.tunnel.common.remote.request.Request;
import com.alibaba.arthas.tunnel.common.remote.response.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author qiyue.zhang@aloudata.com
 * @description GrpcUtils
 * @date 2023/6/15 19:25
 */
public class GrpcUtils {
    
    public static Payload convert(Request request) {
        
        Metadata newMeta = Metadata.newBuilder()
                .setType(request.getClass().getSimpleName())
                .build();
        
        byte[] jsonBytes = convertRequestToByte(request);
        
        Payload.Builder builder = Payload.newBuilder();
        
        return builder
                .setBody(Any.newBuilder().setValue(UnsafeByteOperations.unsafeWrap(jsonBytes)))
                .setMetadata(newMeta).build();
        
    }
    private static byte[] convertRequestToByte(Request request) {
        try {
            Map<String, String> requestHeaders = new HashMap<>(request.getHeaders());
            request.clearHeaders();
            byte[] jsonBytes = new ObjectMapper().writeValueAsBytes(request);
            request.putAllHeader(requestHeaders);
            return jsonBytes;
        } catch (JsonProcessingException e) {
            throw new ArthasException();
        }
    }
    
    public static Payload convert(Response response) {
        try {
            byte[] jsonBytes = new ObjectMapper().writeValueAsBytes(response);
            Metadata.Builder metaBuilder = Metadata.newBuilder().setType(response.getClass().getSimpleName());
            return Payload.newBuilder()
                    .setBody(Any.newBuilder().setValue(UnsafeByteOperations.unsafeWrap(jsonBytes)))
                    .setMetadata(metaBuilder.build()).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Object parse(Payload payload, Class classType) {
        if (classType != null) {
            ByteString byteString = payload.getBody().getValue();
            ByteBuffer byteBuffer = byteString.asReadOnlyByteBuffer();
            Object obj = null;
            try {
                obj = new ObjectMapper().readValue(new ByteBufferBackedInputStream(byteBuffer), classType);
            } catch (IOException e) {
                throw new ArthasException();
            }
            if (obj instanceof Request) {
                ((Request) obj).putAllHeader(payload.getMetadata().getHeadersMap());
            }
            return obj;
        } else {
            throw new ArthasException(ArthasException.SERVER_ERROR,
                    "Unknown payload type:" + payload.getMetadata().getType());
        }
        
    }
}

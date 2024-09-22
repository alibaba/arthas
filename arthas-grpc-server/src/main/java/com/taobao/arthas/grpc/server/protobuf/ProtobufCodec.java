package com.taobao.arthas.grpc.server.protobuf;

import com.google.protobuf.CodedInputStream;

import java.io.IOException;

/**
 * @author: FengYe
 * @date: 2024/7/17 下午9:44
 * @description: Codec
 */
public interface ProtobufCodec<T> {
    byte[] encode(T t) throws IOException;

    T decode(byte[] bytes) throws IOException;

    int size(T t) throws IOException;

    T readFrom(CodedInputStream intput) throws IOException;
}

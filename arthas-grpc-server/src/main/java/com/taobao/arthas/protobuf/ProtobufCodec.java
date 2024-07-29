package com.taobao.arthas.protobuf;/**
 * @author: 風楪
 * @date: 2024/7/17 下午9:44
 */

import java.io.IOException;

/**
 * @author: FengYe
 * @date: 2024/7/17 下午9:44
 * @description: Codec
 */
public interface ProtobufCodec<T> {
    byte[] encode(T t);

    T decode(byte[] bytes);

    int size(T t) throws IOException;
}

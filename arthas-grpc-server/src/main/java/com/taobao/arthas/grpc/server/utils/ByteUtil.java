package com.taobao.arthas.grpc.server.utils;/**
 * @author: 風楪
 * @date: 2024/9/5 00:51
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * @author: FengYe
 * @date: 2024/9/5 00:51
 * @description: ByteUtil
 */
public class ByteUtil {

    public static ByteBuf getByteBuf() {
        return PooledByteBufAllocator.DEFAULT.buffer();
    }

    public static ByteBuf getByteBuf(byte[] bytes) {
        return PooledByteBufAllocator.DEFAULT.buffer(bytes.length).writeBytes(bytes);
    }

    public static byte[] getBytes(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }
}

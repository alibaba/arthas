package com.taobao.arthas.grpc.server.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * @author: FengYe
 * @date: 2024/9/5 00:51
 * @description: ByteUtil
 */
public class ByteUtil {

    public static ByteBuf newByteBuf() {
        return PooledByteBufAllocator.DEFAULT.buffer();
    }

    public static ByteBuf newByteBuf(byte[] bytes) {
        return PooledByteBufAllocator.DEFAULT.buffer(bytes.length).writeBytes(bytes);
    }

    public static byte[] getBytes(ByteBuf buf) {
        if (buf.hasArray()) {
            // 如果 ByteBuf 是一个支持底层数组的实现，直接获取数组
            return buf.array();
        } else {
            // 创建一个新的 byte 数组
            byte[] bytes = new byte[buf.readableBytes()];
            // 将 ByteBuf 的内容复制到 byte 数组中
            buf.getBytes(buf.readerIndex(), bytes);
            return bytes;
        }
    }
}

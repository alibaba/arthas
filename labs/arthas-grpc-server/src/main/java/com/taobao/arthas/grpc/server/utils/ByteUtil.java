package com.taobao.arthas.grpc.server.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * 字节缓冲区工具类
 *
 * 该工具类提供了对 Netty ByteBuf 的便捷操作方法。
 * 它封装了 ByteBuf 的创建、数据写入和读取等常用功能，
 * 并使用池化内存分配器来提高内存使用效率。
 *
 * 主要功能：
 * 1. 创建空的字节缓冲区
 * 2. 从字节数组创建字节缓冲区
 * 3. 将字节缓冲区转换为字节数组
 *
 * 设计特点：
 * - 使用池化内存分配器，减少内存分配开销
 * - 自动处理不同类型的 ByteBuf 实现
 * - 提供类型安全的字节操作方法
 *
 * @author: FengYe
 * @date: 2024/9/5 00:51
 * @description: ByteUtil
 */
public class ByteUtil {

    /**
     * 创建一个新的空字节缓冲区
     *
     * 该方法使用 Netty 的池化内存分配器创建一个新的 ByteBuf 实例。
     * 池化分配器可以从内存池中分配内存，减少系统内存分配的开销，
     * 提高性能。创建的缓冲区初始容量为默认值（256 字节），
     * 可根据需要自动扩容。
     *
     * 使用场景：
     * - 需要动态构建字节数据
     * - 准备发送的网络数据包
     * - 临时数据存储
     *
     * @return ByteBuf 新创建的空字节缓冲区，使用池化内存
     */
    public static ByteBuf newByteBuf() {
        // 使用默认的池化内存分配器创建一个新的 ByteBuf
        // DEFAULT 是 Netty 预配置的池化分配器实例
        // buffer() 方法创建一个初始容量为默认值的缓冲区
        return PooledByteBufAllocator.DEFAULT.buffer();
    }

    /**
     * 从字节数组创建字节缓冲区
     *
     * 该方法创建一个新字节缓冲区，并将指定的字节数组内容写入其中。
     * 缓冲区的初始容量会根据输入数组的大小自动调整，避免不必要的
     * 内存浪费。写入操作后，缓冲区的读写指针位于数组末尾。
     *
     * 使用场景：
     * - 将已有的字节数据封装为 ByteBuf
     * - 准备发送固定内容的消息
     * - 字节数组的批量操作
     *
     * @param bytes 要写入缓冲区的源字节数组，不能为 null
     * @return ByteBuf 包含指定字节数组内容的新字节缓冲区
     */
    public static ByteBuf newByteBuf(byte[] bytes) {
        // 创建一个容量与输入数组大小相同的缓冲区
        // 然后将整个字节数组写入缓冲区
        // writeBytes() 方法会更新 writerIndex，使其指向数据末尾
        return PooledByteBufAllocator.DEFAULT.buffer(bytes.length).writeBytes(bytes);
    }

    /**
     * 将字节缓冲区转换为字节数组
     *
     * 该方法从 ByteBuf 中提取所有可读字节并转换为字节数组。
     * 方法会智能处理不同类型的 ByteBuf 实现：
     *
     * 1. 如果 ByteBuf 有底层数组支持（hasArray() 返回 true），
     *    直接返回底层数组引用，避免数据复制，提高性能。
     *
     * 2. 如果 ByteBuf 没有底层数组支持（如直接内存缓冲区），
     *    会创建一个新的字节数组，并将数据复制到新数组中。
     *
     * 注意事项：
     * - 该方法不会修改 ByteBuf 的读写指针位置
     * - 返回的数组长度等于 ByteBuf 的可读字节数
     * - 对于堆内存 ByteBuf，返回的是底层数组引用
     *
     * @param buf 要转换的源字节缓冲区，不能为 null
     * @return byte[] 包含缓冲区所有可读数据的字节数组
     */
    public static byte[] getBytes(ByteBuf buf) {
        // 检查 ByteBuf 是否有底层数组支持
        if (buf.hasArray()) {
            // 如果 ByteBuf 是一个支持底层数组的实现（如堆内存缓冲区），
            // 直接获取底层数组引用，避免数据复制，提高性能
            return buf.array();
        } else {
            // 对于直接内存缓冲区或其他不支持底层数组的实现，
            // 需要创建一个新的 byte 数组来存储数据

            // 创建一个长度等于可读字节数的新数组
            byte[] bytes = new byte[buf.readableBytes()];

            // 从当前读指针位置开始，将 ByteBuf 的内容复制到 byte 数组中
            // getBytes() 方法不会改变 ByteBuf 的 readerIndex
            buf.getBytes(buf.readerIndex(), bytes);

            // 返回包含复制数据的新数组
            return bytes;
        }
    }
}

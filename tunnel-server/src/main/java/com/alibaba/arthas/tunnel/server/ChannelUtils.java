package com.alibaba.arthas.tunnel.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * Netty通道工具类
 * <p>
 * 该类提供了一系列用于操作Netty通道的静态工具方法。
 * 主要用于在Tunnel Server中优雅地关闭与客户端或Agent的网络连接。
 * </p>
 * <p>
 * 所有方法都是静态的，并且该类不能被实例化（私有构造函数）。
 * </p>
 */
public final class ChannelUtils {

    /**
     * 在刷新所有排队的写请求后关闭指定的通道
     * <p>
     * 该方法用于优雅地关闭网络连接。它不是立即关闭通道，而是：
     * <ol>
     * <li>首先刷新（flush）通道中所有已排队但尚未发送的写请求</li>
     * <li>发送一个空的缓冲区以确保所有数据都被推送出去</li>
     * <li>在数据发送完成后监听关闭事件</li>
     * <li>最后关闭通道连接</li>
     * </ol>
     * </p>
     * <p>
     * 这种方式可以确保在关闭连接之前，所有待发送的数据都能成功传输到对端，
     * 避免数据丢失，是网络编程中优雅关闭连接的最佳实践。
     * </p>
     *
     * @param ch 需要关闭的Netty通道对象
     */
    public static void closeOnFlush(Channel ch) {
        // 检查通道是否处于活动状态（已连接且未关闭）
        if (ch.isActive()) {
            // 写入空缓冲区并刷新，然后添加关闭监听器
            // Unpooled.EMPTY_BUFFER 是一个空的、不可变的缓冲区
            // ChannelFutureListener.CLOSE 确保在刷新完成后关闭通道
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 私有构造函数
     * <p>
     * 防止该工具类被实例化。按照最佳实践，工具类应该只包含静态方法和静态字段，
     * 并且不应该有公共的构造函数。
     * </p>
     */
    private ChannelUtils() {
    }
}

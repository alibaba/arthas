package com.alibaba.arthas.tunnel.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * Netty 通道工具类
 *
 * <p>这是一个工具类，提供 Netty Channel（通道）的常用操作方法。</p>
 * <p>主要功能：</p>
 * <ul>
 *   <li>安全地关闭通道：在刷新完所有待写入的数据后再关闭</li>
 * </ul>
 *
 * <p>该类是 final 类，不允许被继承，所有方法均为静态方法</p>
 */
public final class ChannelUtils {

    /**
     * 在刷新所有排队写入请求后关闭指定通道
     *
     * <p>该方法用于优雅地关闭 Netty 通道：</p>
     * <ol>
     *   <li>首先检查通道是否处于活动状态（isActive）</li>
     *   <li>如果通道是活动的，则写入一个空缓冲区</li>
     *   <li>刷新（flush）所有待写入的数据</li>
     *   <li>在刷新完成后添加一个监听器，监听器会在操作完成后关闭通道</li>
     * </ol>
     *
     * <p>这种关闭方式的优势：</p>
     * <ul>
     *   <li>确保所有已排队的写入请求都能发送出去</li>
     *   <li>避免数据丢失</li>
     *   <li>优雅地关闭连接，不会造成突兀的断开</li>
     * </ul>
     *
     * @param ch 要关闭的 Netty 通道对象，如果为 null 或非活动状态则不执行任何操作
     */
    public static void closeOnFlush(Channel ch) {
        // 检查通道是否处于活动状态（已连接且未关闭）
        if (ch.isActive()) {
            // 写入一个空缓冲区并刷新，添加关闭监听器
            // Unpooled.EMPTY_BUFFER 表示不写入实际数据
            // ChannelFutureListener.CLOSE 确保在刷新完成后关闭通道
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 私有构造函数
     *
     * <p>防止实例化该工具类，因为所有方法都是静态的</p>
     * <p>按照最佳实践，工具类不应该被实例化</p>
     */
    private ChannelUtils() {
    }
}

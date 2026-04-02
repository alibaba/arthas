package com.alibaba.arthas.tunnel.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 中继处理器类
 *
 * 该类是一个Netty ChannelHandler，用于在两个通道之间转发（中继）数据。
 * 它继承自ChannelInboundHandlerAdapter，实现了通道入站事件的处理。
 *
 * 主要功能：
 * 1. 当通道激活时，发送一个空缓冲区以触发后续操作
 * 2. 将接收到的数据转发到目标中继通道
 * 3. 当通道不活跃时，关闭中继通道
 * 4. 处理异常情况，确保资源正确释放
 *
 * 使用场景：
 * - 在Arthas隧道客户端中，用于将本地通道的数据转发到远程服务器
 * - 实现双向数据传输，支持客户端和服务器之间的通信
 *
 * 注意事项：
 * - 该类被声明为final，禁止继承
 * - 在转发数据时会检查目标通道是否活跃，避免向已关闭的通道写入数据
 * - 使用ReferenceCountUtil正确管理Netty的引用计数资源
 */
public final class RelayHandler extends ChannelInboundHandlerAdapter {
    /**
     * 日志记录器，用于记录中继处理器的运行状态和错误信息
     */
    private final static Logger logger = LoggerFactory.getLogger(RelayHandler.class);

    /**
     * 中继通道，数据将被转发到此通道
     *
     * 该通道是数据转发的目标端，通常是与远程服务器建立的连接。
     * 当源通道接收到数据时，会将数据写入到这个中继通道中。
     */
    private final Channel relayChannel;

    /**
     * 构造函数
     *
     * 创建一个RelayHandler实例，并指定数据转发的目标通道。
     *
     * @param relayChannel 中继通道，所有接收到的数据都将被转发到此通道
     */
    public RelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    /**
     * 处理通道激活事件
     *
     * 当通道变为活跃状态（连接建立）时，此方法被调用。
     * 它会发送一个空的缓冲区，用于触发后续的数据传输流程。
     *
     * @param ctx ChannelHandlerContext，通道处理器上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 发送空缓冲区，触发通道的写操作
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    /**
     * 处理通道读取事件
     *
     * 当从通道中读取到数据时，此方法被调用。
     * 它会将接收到的数据转发到中继通道。
     *
     * @param ctx ChannelHandlerContext，通道处理器上下文
     * @param msg 从通道中读取的消息对象
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 检查中继通道是否处于活跃状态
        if (relayChannel.isActive()) {
            // 如果中继通道活跃，将消息写入并刷新到中继通道
            relayChannel.writeAndFlush(msg);
        } else {
            // 如果中继通道不活跃，释放消息资源，避免内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 处理通道非活跃事件
     *
     * 当通道变为非活跃状态（连接断开）时，此方法被调用。
     * 如果中继通道仍然活跃，则关闭中继通道。
     *
     * @param ctx ChannelHandlerContext，通道处理器上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 检查中继通道是否活跃
        if (relayChannel.isActive()) {
            // 使用ChannelUtils工具类优雅地关闭中继通道
            ChannelUtils.closeOnFlush(relayChannel);
        }
    }

    /**
     * 处理异常事件
     *
     * 当处理过程中发生异常时，此方法被调用。
     * 它会记录错误日志，并确保所有相关通道都被正确关闭。
     *
     * @param ctx ChannelHandlerContext，通道处理器上下文
     * @param cause 异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 记录错误日志，包含异常堆栈信息
        logger.error("RelayHandler error", cause);
        try {
            // 尝试关闭中继通道
            if (relayChannel.isActive()) {
                relayChannel.close();
            }
        } finally {
            // 确保当前通道也被关闭
            ctx.close();
        }
    }
}

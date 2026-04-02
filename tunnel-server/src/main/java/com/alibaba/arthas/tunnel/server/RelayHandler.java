package com.alibaba.arthas.tunnel.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Netty通道转发处理器
 * 用于在两个通道之间转发数据，实现客户端和Agent之间的数据中继
 */
public final class RelayHandler extends ChannelInboundHandlerAdapter {

    // 日志记录器
    private final static Logger logger = LoggerFactory.getLogger(RelayHandler.class);

    // 要转发到的目标通道
    private final Channel relayChannel;

    /**
     * 构造函数
     *
     * @param relayChannel 转发的目标通道
     */
    public RelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    /**
     * 当通道变为活动状态时调用
     * 向通道写入一个空的缓冲区以触发连接建立
     *
     * @param ctx 通道处理器上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 写入空缓冲区，表示连接已建立
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    /**
     * 当从通道读取到数据时调用
     * 将读取到的数据转发到目标通道
     *
     * @param ctx 通道处理器上下文
     * @param msg 读取到的消息对象
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 检查目标通道是否仍然活动
        if (relayChannel.isActive()) {
            // 目标通道活动，直接将消息转发过去
            relayChannel.writeAndFlush(msg);
        } else {
            // 目标通道不活动，释放消息资源，避免内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 当通道变为非活动状态时调用
     * 如果目标通道仍然活动，则关闭目标通道
     *
     * @param ctx 通道处理器上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 检查目标通道是否仍然活动
        if (relayChannel.isActive()) {
            // 向目标通道发送空缓冲区，并添加关闭监听器，发送完成后关闭通道
            relayChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 当处理过程中发生异常时调用
     * 记录错误日志并关闭通道
     *
     * @param ctx 通道处理器上下文
     * @param cause 异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 记录异常信息
        logger.error("", cause);
        // 关闭通道
        ctx.close();
    }
}

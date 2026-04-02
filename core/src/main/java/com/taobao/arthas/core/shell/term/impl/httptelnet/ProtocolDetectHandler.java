package com.taobao.arthas.core.shell.term.impl.httptelnet;

import java.util.concurrent.TimeUnit;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.term.impl.http.BasicHttpAuthenticatorHandler;
import com.taobao.arthas.core.shell.term.impl.http.HttpRequestHandler;

import com.taobao.arthas.core.shell.term.impl.http.TtyWebSocketFrameHandler;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ScheduledFuture;
import io.termd.core.function.Consumer;
import io.termd.core.function.Supplier;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.netty.TelnetChannelHandler;
import io.termd.core.tty.TtyConnection;

/**
 * 协议检测处理器
 *
 * 该类用于检测客户端连接使用的是 Telnet 协议还是 HTTP/WebSocket 协议，
 * 并根据检测到的协议类型动态配置相应的处理器链。
 *
 * 工作原理：
 * 1. 当连接建立时，启动一个1秒的定时器，如果1秒内没有收到数据，则认为是 Telnet 连接
 * 2. 如果收到数据，检查前3个字节是否为 "GET"，如果是则认为是 HTTP/WebSocket 连接
 * 3. 根据协议类型，动态添加相应的处理器到 ChannelPipeline 中
 *
 * @author hengyunabc 2019-11-04
 *
 */
public class ProtocolDetectHandler extends ChannelInboundHandlerAdapter {
    /**
     * 通道组，用于管理所有活跃的连接通道
     */
    private ChannelGroup channelGroup;

    /**
     * Telnet 处理器工厂，用于创建 Telnet 连接处理器
     */
    private Supplier<TelnetHandler> handlerFactory;

    /**
     * TTY 连接工厂，用于创建 TTY 连接（HTTP/WebSocket 终端连接）
     */
    private Consumer<TtyConnection> ttyConnectionFactory;

    /**
     * 工作线程组，用于执行耗时任务
     */
    private EventExecutorGroup workerGroup;

    /**
     * HTTP 会话管理器，用于管理 HTTP 会话
     */
    private HttpSessionManager httpSessionManager;

    /**
     * 构造协议检测处理器
     *
     * @param channelGroup 通道组，用于管理所有活跃连接
     * @param handlerFactory Telnet 处理器工厂
     * @param ttyConnectionFactory TTY 连接工厂
     * @param workerGroup 工作线程组
     * @param httpSessionManager HTTP 会话管理器
     */
    public ProtocolDetectHandler(ChannelGroup channelGroup, final Supplier<TelnetHandler> handlerFactory,
                                 Consumer<TtyConnection> ttyConnectionFactory, EventExecutorGroup workerGroup,
                                 HttpSessionManager httpSessionManager) {
        this.channelGroup = channelGroup;
        this.handlerFactory = handlerFactory;
        this.ttyConnectionFactory = ttyConnectionFactory;
        this.workerGroup = workerGroup;
        this.httpSessionManager = httpSessionManager;
    }

    /**
     * Telnet 协议检测定时任务
     * 当连接建立后，如果在1秒内没有收到数据，则认为该连接是 Telnet 协议
     */
    private ScheduledFuture<?> detectTelnetFuture;

    /**
     * 当通道激活时调用
     *
     * 该方法在连接建立时被调用，会启动一个1秒的定时任务。
     * 如果1秒内没有收到任何数据，则认为客户端使用的是 Telnet 协议，
     * 此时将配置 Telnet 处理器链。
     *
     * @param ctx 通道处理器上下文
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        // 启动一个延迟1秒的定时任务
        detectTelnetFuture = ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                // 将通道添加到通道组中
                channelGroup.add(ctx.channel());
                // 创建 Telnet 通道处理器
                TelnetChannelHandler handler = new TelnetChannelHandler(handlerFactory);
                // 获取当前通道的处理器管道
                ChannelPipeline pipeline = ctx.pipeline();
                // 添加 Telnet 处理器到管道
                pipeline.addLast(handler);
                // 从管道中移除当前协议检测处理器（因为已经检测出协议类型）
                pipeline.remove(ProtocolDetectHandler.this);
                // 触发通道激活事件，初始化 TelnetChannelHandler
                ctx.fireChannelActive(); // trigger TelnetChannelHandler init
            }

        }, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 当从通道读取数据时调用
     *
     * 该方法在接收到客户端数据时被调用，通过检测数据的前3个字节来判断协议类型：
     * - 如果前3个字节是 "GET"，则认为是 HTTP/WebSocket 协议
     * - 否则认为是 Telnet 协议
     *
     * 根据检测到的协议类型，动态配置相应的处理器链。
     *
     * @param ctx 通道处理器上下文
     * @param msg 读取到的消息（ByteBuf 对象）
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 将消息转换为 ByteBuf 对象
        ByteBuf in = (ByteBuf) msg;
        // 如果可读字节数少于3个，无法判断协议类型，直接返回
        if (in.readableBytes() < 3) {
            return;
        }

        // 如果 Telnet 检测定时任务还未执行且可以取消，则取消该任务
        // 因为已经收到数据，不需要等待超时了
        if (detectTelnetFuture != null && detectTelnetFuture.isCancellable()) {
            detectTelnetFuture.cancel(false);
        }

        // 读取前3个字节用于判断协议类型
        byte[] bytes = new byte[3];
        in.getBytes(0, bytes);
        String httpHeader = new String(bytes);

        // 获取当前通道的处理器管道
        ChannelPipeline pipeline = ctx.pipeline();
        // 如果前3个字节不是 "GET"，则认为是 Telnet 协议
        if (!"GET".equalsIgnoreCase(httpHeader)) { // telnet
            // 将通道添加到通道组中
            channelGroup.add(ctx.channel());
            // 创建 Telnet 通道处理器
            TelnetChannelHandler handler = new TelnetChannelHandler(handlerFactory);
            // 添加 Telnet 处理器到管道
            pipeline.addLast(handler);
            // 触发通道激活事件，初始化 TelnetChannelHandler
            ctx.fireChannelActive(); // trigger TelnetChannelHandler init
        } else {
            // HTTP/WebSocket 协议，配置 HTTP 和 WebSocket 处理器链

            // 添加 HTTP 服务器编解码器，用于处理 HTTP 请求和响应
            pipeline.addLast(new HttpServerCodec());
            // 添加分块写入处理器，支持大文件传输
            pipeline.addLast(new ChunkedWriteHandler());
            // 添加 HTTP 消息聚合器，将 HttpContent 聚合成完整的 FullHttpRequest
            pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));
            // 添加基础 HTTP 认证处理器，用于身份验证
            pipeline.addLast(new BasicHttpAuthenticatorHandler(httpSessionManager));
            // 添加 HTTP 请求处理器，处理普通的 HTTP 请求
            pipeline.addLast(workerGroup, "HttpRequestHandler", new HttpRequestHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH));
            // 添加 WebSocket 协议处理器，处理 WebSocket 握手和协议升级
            pipeline.addLast(new WebSocketServerProtocolHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH, null, false, ArthasConstants.MAX_HTTP_CONTENT_LENGTH, false, true));
            // 添加空闲状态处理器，处理 WebSocket 连接的空闲超时
            pipeline.addLast(new IdleStateHandler(0, 0, ArthasConstants.WEBSOCKET_IDLE_SECONDS));
            // 添加 TTY WebSocket 帧处理器，处理 WebSocket 文本帧
            pipeline.addLast(new TtyWebSocketFrameHandler(channelGroup, ttyConnectionFactory));
            // 触发通道激活事件
            ctx.fireChannelActive();
        }
        // 从管道中移除当前协议检测处理器（协议已检测完成）
        pipeline.remove(this);
        // 继续传递读取到的消息给后续处理器
        ctx.fireChannelRead(in);
    }

}

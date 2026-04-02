package com.taobao.arthas.core.shell.term.impl.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

import com.taobao.arthas.common.ArthasConstants;

/**
 * 本地Tty服务器通道初始化器
 *
 * 用于初始化本地通道的处理器管道，配置HTTP和WebSocket相关的处理器
 * 支持通过本地通道进行Tty连接，主要用于同一JVM内的通信
 *
 * @author hengyunabc 2020-09-02
 *
 */
public class LocalTtyServerInitializer extends ChannelInitializer<LocalChannel> {

    // 通道组，用于管理所有活动的连接通道
    private final ChannelGroup group;

    // Tty连接处理器，当有新连接建立时会调用此处理器
    private final Consumer<TtyConnection> handler;

    // 工作线程组，用于执行耗时的业务逻辑处理
    private EventExecutorGroup workerGroup;

    /**
     * 构造函数
     *
     * @param group 通道组，用于管理所有活动的连接通道
     * @param handler Tty连接处理器，处理新建立的Tty连接
     * @param workerGroup 工作线程组，用于执行业务逻辑
     */
    public LocalTtyServerInitializer(ChannelGroup group, Consumer<TtyConnection> handler,
            EventExecutorGroup workerGroup) {
        this.group = group;
        this.handler = handler;
        this.workerGroup = workerGroup;
    }

    /**
     * 初始化通道
     *
     * 配置通道的处理器管道，按顺序添加以下处理器：
     * 1. HTTP编解码器 - 处理HTTP请求和响应的编解码
     * 2. 分块写入处理器 - 支持大文件的分块传输
     * 3. HTTP对象聚合器 - 将HTTP消息片段聚合为完整的HTTP消息
     * 4. HTTP请求处理器 - 处理HTTP请求
     * 5. WebSocket协议处理器 - 处理WebSocket握手和协议
     * 6. 空闲状态处理器 - 检测连接空闲状态
     * 7. Tty WebSocket帧处理器 - 处理Tty WebSocket通信
     *
     * @param ch 本地通道
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    protected void initChannel(LocalChannel ch) throws Exception {

        // 获取通道的处理器管道
        ChannelPipeline pipeline = ch.pipeline();

        // 添加HTTP服务器编解码器，用于处理HTTP请求和响应的编解码
        pipeline.addLast(new HttpServerCodec());

        // 添加分块写入处理器，支持大文件的分块传输，提高大文件传输性能
        pipeline.addLast(new ChunkedWriteHandler());

        // 添加HTTP对象聚合器，将HTTP消息片段聚合为完整的FullHttpRequest或FullHttpResponse
        // 参数为最大内容长度，超过此长度的请求将被拒绝
        pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));

        // 添加HTTP请求处理器，使用工作线程组执行，处理HTTP请求
        // 使用指定的WebSocket路径
        pipeline.addLast(workerGroup, "HttpRequestHandler", new HttpRequestHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH));

        // 添加WebSocket服务器协议处理器，处理WebSocket握手和协议升级
        // 参数：路径、子协议、是否允许扩展、最大帧大小、是否允许补位、是否允许以相对路径发送
        pipeline.addLast(new WebSocketServerProtocolHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH, null, false, ArthasConstants.MAX_HTTP_CONTENT_LENGTH, false, true));

        // 添加空闲状态处理器，检测连接的空闲超时
        // 参数：读空闲时间、写空闲时间、读写空闲时间（秒）
        pipeline.addLast(new IdleStateHandler(0, 0, ArthasConstants.WEBSOCKET_IDLE_SECONDS));

        // 添加Tty WebSocket帧处理器，处理Tty WebSocket的帧数据和连接管理
        pipeline.addLast(new TtyWebSocketFrameHandler(group, handler));
    }
}

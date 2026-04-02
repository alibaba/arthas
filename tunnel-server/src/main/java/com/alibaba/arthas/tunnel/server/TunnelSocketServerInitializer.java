package com.alibaba.arthas.tunnel.server;

import com.taobao.arthas.common.ArthasConstants;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Tunnel Socket服务器通道初始化器
 * <p>
 * 该类负责初始化Netty服务器的Socket通道，配置处理管道（Pipeline）。
 * 主要功能包括：
 * 1. 配置SSL/TLS支持（如果需要）
 * 2. 配置HTTP编解码器
 * 3. 配置WebSocket协议处理器
 * 4. 配置压缩和空闲检测处理器
 * 5. 添加自定义的WebSocket帧处理器
 * </p>
 *
 * @author hengyunabc 2019-08-27
 *
 */
public class TunnelSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    // SSL上下文，用于支持HTTPS/WSS连接，如果为null则不支持SSL
    private final SslContext sslCtx;

    // Tunnel服务器实例，包含服务器配置和状态信息
    private TunnelServer tunnelServer;

    /**
     * 构造函数
     *
     * @param tunnelServer Tunnel服务器实例，用于获取服务器配置信息
     * @param sslCtx SSL上下文，用于支持SSL/TLS加密连接
     */
    public TunnelSocketServerInitializer(TunnelServer tunnelServer, SslContext sslCtx) {
        this.sslCtx = sslCtx;
        this.tunnelServer = tunnelServer;
    }

    /**
     * 初始化通道
     * <p>
     * 当新的客户端连接建立时，该方法会被调用，用于配置通道的处理管道。
     * 处理管道中的处理器按顺序执行：
     * 1. SSL处理器（如果配置了SSL）
     * 2. HTTP编解码器
     * 3. HTTP消息聚合器
     * 4. WebSocket压缩处理器
     * 5. WebSocket协议处理器
     * 6. 空闲状态处理器
     * 7. Tunnel WebSocket帧处理器
     * </p>
     *
     * @param ch Socket通道
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        // 获取通道的处理管道
        ChannelPipeline pipeline = ch.pipeline();

        // 如果配置了SSL上下文，则添加SSL处理器
        if (sslCtx != null) {
            // 添加SSL处理器，用于处理SSL/TLS握手和数据加密解密
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }

        // 添加HTTP服务器编解码器，处理HTTP请求和响应的编解码
        pipeline.addLast(new HttpServerCodec());

        // 添加HTTP消息聚合器，将HTTP请求的多个部分聚合为完整的FullHttpRequest
        // 参数为最大内容长度，超过此长度的请求会被拒绝
        pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));

        // 添加WebSocket压缩处理器，用于压缩WebSocket帧数据，减少网络传输量
        pipeline.addLast(new WebSocketServerCompressionHandler());

        // 添加WebSocket协议处理器，处理WebSocket握手和协议升级
        // 参数说明：
        // - tunnelServer.getPath(): WebSocket路径
        // - null: 子协议
        // - true: 允许扩展
        // - ArthasConstants.MAX_HTTP_CONTENT_LENGTH: 最大帧大小
        // - false: 不允许直接在没有握手的情况下发送消息
        // - true: 允许在握手之前发送消息
        // - 10000L: 握手超时时间（毫秒）
        pipeline.addLast(new WebSocketServerProtocolHandler(tunnelServer.getPath(), null, true, ArthasConstants.MAX_HTTP_CONTENT_LENGTH, false, true, 10000L));

        // 添加空闲状态处理器，用于检测连接空闲状态
        // 参数说明：读空闲时间、写空闲时间、读写空闲时间（秒）
        // 当连接空闲超过指定时间时，会触发空闲事件，用于发送Ping帧保持连接活跃
        pipeline.addLast(new IdleStateHandler(0, 0, ArthasConstants.WEBSOCKET_IDLE_SECONDS));

        // 添加Tunnel WebSocket帧处理器，这是核心的业务逻辑处理器
        // 处理WebSocket消息帧、Agent注册、客户端连接等功能
        pipeline.addLast(new TunnelSocketFrameHandler(tunnelServer));
    }
}

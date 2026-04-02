package com.alibaba.arthas.tunnel.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.arthas.common.ArthasConstants;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * 隧道转发客户端
 *
 * <p>负责建立与隧道服务器（Tunnel Server）的WebSocket连接，实现远程到本地的流量转发功能。</p>
 * <p>该客户端支持ws和wss两种协议，可以自动处理SSL/TLS加密连接。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>建立与隧道服务器的WebSocket连接</li>
 *   <li>支持WS（非加密）和WSS（加密）协议</li>
 *   <li>自动处理SSL/TLS握手</li>
 *   <li>配置HTTP编解码器和聚合器</li>
 *   <li>管理连接生命周期和资源清理</li>
 * </ul>
 *
 * @author hengyunabc 2019-08-28
 *
 */
public class ForwardClient {
    // 日志记录器，用于记录客户端运行过程中的关键信息和异常
    private final static Logger logger = LoggerFactory.getLogger(ForwardClient.class);

    // 隧道服务器的URI地址，包含协议、主机、端口等信息
    private URI tunnelServerURI;

    /**
     * 构造函数，创建隧道转发客户端实例
     *
     * @param tunnelServerURI 隧道服务器的URI地址，例如：ws://server:port 或 wss://server:port
     */
    public ForwardClient(URI tunnelServerURI) {
        this.tunnelServerURI = tunnelServerURI;
    }

    /**
     * 启动转发客户端，建立与隧道服务器的WebSocket连接
     *
     * <p>该方法会执行以下操作：</p>
     * <ol>
     *   <li>解析URI参数，获取协议、主机、端口信息</li>
     *   <li>验证协议类型（仅支持ws和wss）</li>
     *   <li>如果是wss协议，配置SSL上下文</li>
     *   <li>创建并配置Netty Bootstrap</li>
     *   <li>建立与隧道服务器的连接</li>
     *   <li>等待连接关闭</li>
     * </ol>
     *
     * @throws URISyntaxException 当URI语法错误时抛出
     * @throws SSLException 当SSL配置或握手失败时抛出
     * @throws InterruptedException 当线程被中断时抛出
     */
    public void start() throws URISyntaxException, SSLException, InterruptedException {
        // 获取URI的协议部分，如果未指定则默认为ws
        String scheme = tunnelServerURI.getScheme() == null ? "ws" : tunnelServerURI.getScheme();
        // 获取URI的主机部分，如果未指定则默认为本地地址
        final String host = tunnelServerURI.getHost() == null ? "127.0.0.1" : tunnelServerURI.getHost();
        final int port;

        // 根据URI中的端口设置确定连接端口
        if (tunnelServerURI.getPort() == -1) {
            // 如果URI中未指定端口，根据协议类型使用默认端口
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;  // WebSocket默认端口
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443; // WebSocket Secure默认端口
            } else {
                port = -1;  // 未知协议
            }
        } else {
            // 使用URI中指定的端口
            port = tunnelServerURI.getPort();
        }

        // 验证协议类型，只支持ws和wss
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            logger.error("Only WS(S) is supported, uri: {}", tunnelServerURI);
            return;
        }

        // 判断是否需要SSL加密
        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            // 如果是wss协议，创建SSL上下文，使用不安全的信任管理器（仅用于开发/测试环境）
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            // ws协议不需要SSL上下文
            sslCtx = null;
        }

        // connect to local server
        // 创建WebSocket客户端协议配置，设置服务器URI和最大帧载荷长度
        WebSocketClientProtocolConfig clientProtocolConfig = WebSocketClientProtocolConfig.newBuilder()
                .webSocketUri(tunnelServerURI)
                .maxFramePayloadLength(ArthasConstants.MAX_HTTP_CONTENT_LENGTH).build();

        // 创建WebSocket客户端协议处理器，用于处理WebSocket握手和数据帧
        final WebSocketClientProtocolHandler websocketClientHandler = new WebSocketClientProtocolHandler(
                clientProtocolConfig);

        // 创建转发客户端Socket处理器，用于处理WebSocket帧的转发逻辑
        final ForwardClientSocketClientHandler forwardClientSocketClientHandler = new ForwardClientSocketClientHandler();

        // 创建事件循环组，用于处理IO操作，线程数设置为1，使用守护线程
        final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-ForwardClient", true));
        ChannelFuture closeFuture = null;
        try {
            // 创建Netty Bootstrap，用于配置和启动客户端
            Bootstrap b = new Bootstrap();
            // 设置连接超时时间为5秒
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            // 配置Bootstrap：设置事件循环组、通道类型和处理器
            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    // 获取通道的Pipeline，用于添加处理器链
                    ChannelPipeline p = ch.pipeline();
                    // 如果需要SSL，添加SSL处理器
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                    }
                    // 添加HTTP编解码器、HTTP聚合器、WebSocket协议处理器和转发客户端处理器
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH), websocketClientHandler,
                            forwardClientSocketClientHandler);
                }
            });

            // 连接到隧道服务器，同步等待连接建立，然后获取关闭Future
            closeFuture = b.connect(tunnelServerURI.getHost(), port).sync().channel().closeFuture();
            logger.info("forward client connect to server success, uri: " + tunnelServerURI);
        } finally {
            // 确保资源被正确清理
            if (closeFuture != null) {
                // 如果连接成功，添加关闭监听器，在连接关闭时优雅地关闭事件循环组
                closeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        group.shutdownGracefully();
                    }
                });
            } else {
                // 如果连接失败，直接关闭事件循环组
                group.shutdownGracefully();
            }
        }

    }

}

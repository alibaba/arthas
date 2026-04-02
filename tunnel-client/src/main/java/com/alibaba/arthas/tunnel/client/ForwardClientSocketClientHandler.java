package com.alibaba.arthas.tunnel.client;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.arthas.common.ArthasConstants;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 隧道转发客户端Socket处理器
 *
 * <p>负责处理与隧道服务器的WebSocket连接，并在握手完成后连接本地Arthas服务器，实现双向数据转发。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>监听WebSocket握手完成事件</li>
 *   <li>在握手完成后自动连接本地Arthas服务器</li>
 *   <li>建立隧道服务器和本地服务器之间的数据转发通道</li>
 *   <li>处理连接异常和资源清理</li>
 * </ul>
 *
 * @author hengyunabc 2019-08-28
 */
public class ForwardClientSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    // 日志记录器，用于记录处理器运行过程中的关键信息和异常
    private static final Logger logger = LoggerFactory.getLogger(ForwardClientSocketClientHandler.class);

    // WebSocket握手Future，用于通知握手完成状态
    private ChannelPromise handshakeFuture;

    /**
     * 通道激活时的回调方法
     *
     * <p>当通道变为活跃状态（即连接建立）时调用。</p>
     * <p>当前实现为空，不需要特殊处理。</p>
     *
     * @param ctx 通道处理器上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    /**
     * 通道非活跃时的回调方法
     *
     * <p>当通道变为非活跃状态（即连接断开）时调用。</p>
     * <p>记录断开连接的日志信息。</p>
     *
     * @param ctx 通道处理器上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("WebSocket Client disconnected!");
    }

    /**
     * 用户事件触发时的回调方法
     *
     * <p>当WebSocket握手完成事件触发时，该方法会尝试连接本地Arthas服务器。</p>
     * <p>这是建立隧道服务器到本地服务器转发通道的关键步骤。</p>
     *
     * @param ctx 通道处理器上下文
     * @param evt 触发的事件对象
     */
    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) {
        // 检查是否是WebSocket握手完成事件
        if (evt.equals(ClientHandshakeStateEvent.HANDSHAKE_COMPLETE)) {
            try {
                // 握手完成后，连接本地Arthas服务器
                connectLocalServer(ctx);
            } catch (Throwable e) {
                // 记录连接本地服务器时的错误
                logger.error("ForwardClientSocketClientHandler connect local arthas server error", e);
            }
        } else {
            // 如果不是握手完成事件，传递给下一个处理器
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * 连接本地Arthas服务器
     *
     * <p>该方法执行以下操作：</p>
     * <ol>
     *   <li>创建新的事件循环组和Bootstrap</li>
     *   <li>配置WebSocket客户端协议处理器</li>
     *   <li>使用LocalChannel连接本地Arthas服务器</li>
     *   <li>等待握手完成</li>
     *   <li>替换处理器，建立双向转发通道</li>
     * </ol>
     *
     * <p>注意：实际使用的是LocalChannel（本地传输），而不是TCP连接，因此URI参数仅用于配置协议。</p>
     *
     * @param ctx 通道处理器上下文，与隧道服务器的连接上下文
     * @throws InterruptedException 当线程被中断时抛出
     * @throws URISyntaxException 当URI语法错误时抛出（虽然当前不使用）
     */
    private void connectLocalServer(final ChannelHandlerContext ctx) throws InterruptedException, URISyntaxException {
        // 创建新的事件循环组，用于处理本地服务器的IO操作
        final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-forward-client-connect-local", true));
        ChannelFuture closeFuture = null;
        try {
            logger.info("ForwardClientSocketClientHandler star connect local arthas server");
            // 入参URI实际无意义，只为了程序不出错，因为使用的是LocalChannel，而不是真正的网络连接
            WebSocketClientProtocolConfig clientProtocolConfig = WebSocketClientProtocolConfig.newBuilder()
                    .webSocketUri("ws://127.0.0.1:8563/ws")
                    .maxFramePayloadLength(ArthasConstants.MAX_HTTP_CONTENT_LENGTH).build();

            // 创建WebSocket协议处理器，用于处理WebSocket协议握手
            final WebSocketClientProtocolHandler websocketClientHandler = new WebSocketClientProtocolHandler(
                    clientProtocolConfig);

            // 创建本地帧处理器，用于处理本地连接的WebSocket帧
            final LocalFrameHandler localFrameHandler = new LocalFrameHandler();

            // 创建Bootstrap，配置本地连接
            Bootstrap b = new Bootstrap();
            // 设置连接超时时间为5秒
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            // 配置为使用本地传输（LocalChannel），而不是网络传输
            b.group(group).channel(LocalChannel.class)
                    .handler(new ChannelInitializer<LocalChannel>() {
                        @Override
                        protected void initChannel(LocalChannel ch) {
                            // 获取本地通道的Pipeline
                            ChannelPipeline p = ch.pipeline();
                            // 添加HTTP编解码器、HTTP聚合器、WebSocket协议处理器和本地帧处理器
                            p.addLast(new HttpClientCodec(), new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH), websocketClientHandler,
                                    localFrameHandler);
                        }
                    });

            // 创建本地地址，用于连接本地Arthas服务器
            LocalAddress localAddress = new LocalAddress(ArthasConstants.NETTY_LOCAL_ADDRESS);
            // 连接到本地服务器，同步等待连接建立
            Channel localChannel = b.connect(localAddress).sync().channel();
            // Channel localChannel = b.connect(localServerURI.getHost(), localServerURI.getPort()).sync().channel();
            // 获取握手Future，用于等待握手完成
            this.handshakeFuture = localFrameHandler.handshakeFuture();
            // 添加握手完成的监听器，握手完成后替换处理器
            handshakeFuture.addListener(new GenericFutureListener<ChannelFuture>() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            // 获取本地通道的Pipeline
                            ChannelPipeline pipeline = future.channel().pipeline();
                            // 移除本地帧处理器
                            pipeline.remove(localFrameHandler);
                            // 添加转发处理器，将数据转发到隧道服务器连接
                            pipeline.addLast(new RelayHandler(ctx.channel()));
                        }
                    });

            // 同步等待握手完成
            handshakeFuture.sync();
            // 从隧道服务器连接的Pipeline中移除当前处理器
            ctx.pipeline().remove(ForwardClientSocketClientHandler.this);
            // 添加转发处理器到隧道服务器连接，将数据转发到本地服务器连接
            ctx.pipeline().addLast(new RelayHandler(localChannel));
            logger.info("ForwardClientSocketClientHandler connect local arthas server success");

            // 获取本地通道的关闭Future
            closeFuture = localChannel.closeFuture();
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

    /**
     * 读取WebSocket帧的回调方法
     *
     * <p>当从通道读取到WebSocket帧时调用。</p>
     * <p>当前实现为空，因为数据转发由RelayHandler处理。</p>
     *
     * @param ctx 通道处理器上下文
     * @param msg 读取到的WebSocket帧对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
    }

    /**
     * 异常捕获的回调方法
     *
     * <p>当处理过程中发生异常时调用。</p>
     * <p>该方法会记录错误日志，如果握手未完成则标记握手失败，并关闭通道。</p>
     *
     * @param ctx 通道处理器上下文
     * @param cause 捕获的异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 记录异常信息，包含通道详情
        logger.error("ForwardClientSocketClient channel: {}" , ctx.channel(), cause);
        // 如果握手还未完成，标记握手失败
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        // 关闭通道
        ctx.close();
    }
}

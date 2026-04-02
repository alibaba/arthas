package com.alibaba.arthas.tunnel.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.URIConstans;
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
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * 隧道客户端
 *
 * <p>负责与Arthas隧道服务器建立WebSocket连接，用于远程连接和管理Arthas代理。</p>
 * <p>主要功能包括：</p>
 * <ul>
 *   <li>向隧道服务器注册Arthas代理</li>
 *   <li>维护与隧道服务器的WebSocket长连接</li>
 *   <li>支持断线自动重连</li>
 *   <li>支持WS和WSS协议</li>
 * </ul>
 *
 * @author hengyunabc 2019-08-28
 *
 */
public class TunnelClient {
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(TunnelClient.class);

    /**
     * 隧道服务器URL
     * <p>例如：ws://127.0.0.1:7777/ws 或 wss://example.com/ws</p>
     */
    private String tunnelServerUrl;

    /**
     * 重连延迟时间（秒）
     * <p>当与隧道服务器断开连接后，等待多少秒再尝试重连</p>
     */
    private int reconnectDelay = 5;

    /**
     * 事件循环组
     * <p>用于处理网络连接的IO操作</p>
     * <p>连接到代理服务器，使用两个线程是因为需要支持重连功能（#1284）</p>
     */
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new DefaultThreadFactory("arthas-TunnelClient", true));

    /**
     * 应用名称
     * <p>用于标识不同的应用实例</p>
     */
    private String appName;

    /**
     * 代理ID
     * <p>由隧道服务器生成。如果是重连场景，会复用之前的ID</p>
     * <p>使用volatile修饰确保多线程可见性</p>
     */
    volatile private String id;

    /**
     * Arthas版本号
     */
    private String version = "unknown";

    /**
     * 连接状态标识
     * <p>true表示已连接到隧道服务器，false表示未连接</p>
     * <p>使用volatile修饰确保多线程可见性</p>
     */
    private volatile boolean connected = false;

    /**
     * 启动隧道客户端
     *
     * <p>向隧道服务器发起连接请求，完成代理注册</p>
     *
     * @return 连接的Future对象，可用于监听连接结果
     * @throws IOException 如果发生IO异常
     * @throws InterruptedException 如果连接被中断
     * @throws URISyntaxException 如果URL格式错误
     */
    public ChannelFuture start() throws IOException, InterruptedException, URISyntaxException {
        return connect(false);
    }

    /**
     * 连接到隧道服务器
     *
     * <p>建立WebSocket连接并注册Arthas代理到隧道服务器</p>
     *
     * @param reconnect 是否为重连操作。true表示重连，false表示首次连接
     * @return 注册成功的Future对象
     * @throws SSLException 如果SSL配置出错
     * @throws URISyntaxException 如果URL格式错误
     * @throws InterruptedException 如果连接过程被中断
     */
    public ChannelFuture connect(boolean reconnect) throws SSLException, URISyntaxException, InterruptedException {
        // 构建查询参数，包含注册方法、版本号、应用名和代理ID
        QueryStringEncoder queryEncoder = new QueryStringEncoder(this.tunnelServerUrl);
        queryEncoder.addParam(URIConstans.METHOD, MethodConstants.AGENT_REGISTER);
        queryEncoder.addParam(URIConstans.ARTHAS_VERSION, this.version);
        if (appName != null) {
            queryEncoder.addParam(URIConstans.APP_NAME, appName);
        }
        if (id != null) {
            queryEncoder.addParam(URIConstans.ID, id);
        }
        // 生成注册URI，格式例如：ws://127.0.0.1:7777/ws?method=agentRegister
        final URI agentRegisterURI = queryEncoder.toUri();

        logger.info("Try to register arthas agent, uri: {}", agentRegisterURI);

        // 解析URI的协议、主机和端口
        String scheme = agentRegisterURI.getScheme() == null ? "ws" : agentRegisterURI.getScheme();
        final String host = agentRegisterURI.getHost() == null ? "127.0.0.1" : agentRegisterURI.getHost();
        final int port;
        if (agentRegisterURI.getPort() == -1) {
            // 根据协议设置默认端口
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = agentRegisterURI.getPort();
        }

        // 只支持WS和WSS协议
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only WS(S) is supported. tunnelServerUrl: " + tunnelServerUrl);
        }

        // 判断是否需要SSL
        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            // 如果是WSS协议，创建SSL上下文（使用不安全的信任管理器，用于测试环境）
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // 创建WebSocket客户端协议配置
        WebSocketClientProtocolConfig clientProtocolConfig = WebSocketClientProtocolConfig.newBuilder()
                .webSocketUri(agentRegisterURI)
                .maxFramePayloadLength(ArthasConstants.MAX_HTTP_CONTENT_LENGTH).build();

        // 创建WebSocket协议处理器
        final WebSocketClientProtocolHandler websocketClientHandler = new WebSocketClientProtocolHandler(
                clientProtocolConfig);
        // 创建自定义的Socket处理器
        final TunnelClientSocketClientHandler handler = new TunnelClientSocketClientHandler(TunnelClient.this);

        // 创建Netty Bootstrap
        Bootstrap bs = new Bootstrap();

        // 配置Bootstrap参数
        bs.group(eventLoopGroup)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 设置连接超时为5秒
        .option(ChannelOption.TCP_NODELAY, true)  // 禁用Nagle算法，减少延迟
        .channel(NioSocketChannel.class).remoteAddress(host, port)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // 如果使用WSS协议，添加SSL处理器
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                        }

                        // 添加处理器到管道：
                        // 1. HttpClientCodec: HTTP编解码器
                        // 2. HttpObjectAggregator: HTTP消息聚合器
                        // 3. websocketClientHandler: WebSocket协议处理器
                        // 4. IdleStateHandler: 空闲状态处理器，用于心跳检测
                        // 5. handler: 自定义的消息处理器
                        p.addLast(new HttpClientCodec(), new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH), websocketClientHandler,
                                new IdleStateHandler(0, 0, ArthasConstants.WEBSOCKET_IDLE_SECONDS),
                                handler);
                    }
                });

        // 发起连接
        ChannelFuture connectFuture = bs.connect();
        if (reconnect) {
            // 如果是重连，添加连接监听器处理连接失败的情况
            connectFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.cause() != null) {
                        logger.error("connect to tunnel server error, uri: {}", tunnelServerUrl, future.cause());
                    }
                }
            });
        }
        // 等待连接完成
        connectFuture.sync();

        // 返回注册Future，可以通过它监听注册结果
        return handler.registerFuture();
    }

    /**
     * 停止隧道客户端
     *
     * <p>优雅地关闭事件循环组，释放所有资源</p>
     */
    public void stop() {
        eventLoopGroup.shutdownGracefully();
    }

    /**
     * 获取隧道服务器URL
     *
     * @return 隧道服务器URL
     */
    public String getTunnelServerUrl() {
        return tunnelServerUrl;
    }

    /**
     * 设置隧道服务器URL
     *
     * @param tunnelServerUrl 隧道服务器URL，例如：ws://127.0.0.1:7777/ws
     */
    public void setTunnelServerUrl(String tunnelServerUrl) {
        this.tunnelServerUrl = tunnelServerUrl;
    }

    /**
     * 获取重连延迟时间
     *
     * @return 重连延迟时间（秒）
     */
    public int getReconnectDelay() {
        return reconnectDelay;
    }

    /**
     * 设置重连延迟时间
     *
     * @param reconnectDelay 重连延迟时间（秒）
     */
    public void setReconnectDelay(int reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    /**
     * 获取代理ID
     *
     * @return 代理ID，由隧道服务器分配
     */
    public String getId() {
        return id;
    }

    /**
     * 设置代理ID
     *
     * @param id 代理ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取Arthas版本号
     *
     * @return Arthas版本号
     */
    public String getVersion() {
        return version;
    }

    /**
     * 设置Arthas版本号
     *
     * @param version Arthas版本号
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 设置应用名称
     *
     * @param appName 应用名称
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * 获取连接状态
     *
     * @return 如果已连接到隧道服务器返回true，否则返回false
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * 设置连接状态
     *
     * @param connected 连接状态，true表示已连接，false表示未连接
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}

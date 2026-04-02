package com.alibaba.arthas.tunnel.server;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;
import com.taobao.arthas.common.ArthasConstants;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Promise;

/**
 * Arthas隧道服务器
 * 负责管理Agent和客户端之间的WebSocket连接，实现命令的转发和结果的返回
 * 支持单机和集群两种部署模式
 *
 * @author hengyunabc 2019-08-09
 *
 */
public class TunnelServer {
    // 日志记录器
    private final static Logger logger = LoggerFactory.getLogger(TunnelServer.class);

    // 是否启用SSL加密
    private boolean ssl;
    // 服务器监听的主机地址
    private String host;
    // 服务器监听的端口号
    private int port;
    // WebSocket访问路径，使用Arthas默认路径
    private String path = ArthasConstants.DEFAULT_WEBSOCKET_PATH;

    // Agent信息Map，key为agentId，value为AgentInfo对象
    // 使用ConcurrentHashMap保证线程安全
    private Map<String, AgentInfo> agentInfoMap = new ConcurrentHashMap<>();

    // 客户端连接信息Map，key为客户端连接ID，value为ClientConnectionInfo对象
    private Map<String, ClientConnectionInfo> clientConnectionInfoMap = new ConcurrentHashMap<>();

    /**
     * 代理请求Promise映射表
     * 记录所有正在进行的代理请求，用于异步获取响应结果
     * key为requestId，value为Promise对象
     */
    private Map<String, Promise<SimpleHttpResponse>> proxyRequestPromiseMap = new ConcurrentHashMap<>();

    // Netty的Boss事件循环组，负责接收连接，使用1个线程
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-TunnelServer-boss", true));
    // Netty的Worker事件循环组，负责处理连接的I/O操作，线程数默认为CPU核心数的2倍
    private EventLoopGroup workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-TunnelServer-worker", true));

    // Netty服务器Channel
    private Channel channel;

    /**
     * 隧道集群存储对象
     * 在集群部署时，用于保存agentId和tunnel server host的映射关系
     */
    private TunnelClusterStore tunnelClusterStore;

    /**
     * 客户端连接的主机地址
     * 集群部署时，客户端连接使用的外部可访问的host
     */
    private String clientConnectHost;

    /**
     * 启动Tunnel服务器
     * 初始化Netty服务器，配置SSL，开始监听端口
     *
     * @throws Exception 启动过程中可能抛出的异常
     */
    public void start() throws Exception {
        // 配置SSL上下文
        final SslContext sslCtx;
        if (ssl) {
            // 如果启用SSL，生成自签名证书
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            // 未启用SSL，设置为null
            sslCtx = null;
        }

        // 创建Netty服务器启动引导类
        ServerBootstrap b = new ServerBootstrap();
        // 配置服务器参数：
        // 1. 设置Boss和Worker事件循环组
        // 2. 设置通道类型为NIO非阻塞通道
        // 3. 添加日志处理器，记录INFO级别的日志
        // 4. 设置子通道初始化器，配置ChannelPipeline
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new TunnelSocketServerInitializer(this, sslCtx));

        // 绑定端口并启动服务器
        if (StringUtils.isBlank(host)) {
            // 如果未指定host，绑定到所有网络接口
            channel = b.bind(port).sync().channel();
        } else {
            // 如果指定了host，绑定到特定地址
            channel = b.bind(host, port).sync().channel();
        }

        // 记录服务器启动日志
        logger.info("Tunnel server listen at {}:{}", host, port);

        // 启动定时任务，每60秒执行一次
        workerGroup.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                // 清理已断开的Agent连接
                agentInfoMap.entrySet().removeIf(e -> !e.getValue().getChannelHandlerContext().channel().isActive());
                // 清理已断开的客户端连接
                clientConnectionInfoMap.entrySet()
                        .removeIf(e -> !e.getValue().getChannelHandlerContext().channel().isActive());

                // 更新集群中的Agent信息
                if (tunnelClusterStore != null && clientConnectHost != null) {
                    try {
                        // 遍历所有Agent，更新其在集群存储中的信息
                        for (Entry<String, AgentInfo> entry : agentInfoMap.entrySet()) {
                            // 保存Agent信息，过期时间为1小时
                            tunnelClusterStore.addAgent(entry.getKey(), new AgentClusterInfo(entry.getValue(), clientConnectHost, port), 60 * 60, TimeUnit.SECONDS);
                        }
                    } catch (Throwable t) {
                        logger.error("update tunnel info error", t);
                    }
                }
            }

        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 停止Tunnel服务器
     * 关闭Channel，优雅地关闭事件循环组
     */
    public void stop() {
        // 如果Channel不为空，关闭它
        if (channel != null) {
            channel.close();
        }
        // 优雅地关闭Boss事件循环组
        bossGroup.shutdownGracefully();
        // 优雅地关闭Worker事件循环组
        workerGroup.shutdownGracefully();
    }

    /**
     * 根据ID查找Agent信息
     *
     * @param id Agent的唯一标识
     * @return 包含Agent信息的Optional对象，如果不存在则返回空的Optional
     */
    public Optional<AgentInfo> findAgent(String id) {
        return Optional.ofNullable(this.agentInfoMap.get(id));
    }

    /**
     * 添加Agent信息到服务器
     * 同时更新集群存储（如果配置了集群）
     *
     * @param id Agent的唯一标识
     * @param agentInfo Agent信息对象
     */
    public void addAgent(String id, AgentInfo agentInfo) {
        // 将Agent信息存入本地Map
        agentInfoMap.put(id, agentInfo);
        // 如果配置了集群存储，同时更新集群中的信息
        if (this.tunnelClusterStore != null) {
            this.tunnelClusterStore.addAgent(id, new AgentClusterInfo(agentInfo, clientConnectHost, port), 60 * 60, TimeUnit.SECONDS);
        }
    }

    /**
     * 从服务器中移除Agent信息
     * 同时从集群存储中移除（如果配置了集群）
     *
     * @param id 要移除的Agent的唯一标识
     * @return 被移除的Agent信息对象，如果不存在则返回null
     */
    public AgentInfo removeAgent(String id) {
        // 从本地Map中移除Agent信息
        AgentInfo agentInfo = agentInfoMap.remove(id);
        // 如果配置了集群存储，同时从集群中移除
        if (this.tunnelClusterStore != null) {
            this.tunnelClusterStore.removeAgent(id);
        }
        return agentInfo;
    }

    /**
     * 根据ID查找客户端连接信息
     *
     * @param id 客户端连接的唯一标识
     * @return 包含客户端连接信息的Optional对象，如果不存在则返回空的Optional
     */
    public Optional<ClientConnectionInfo> findClientConnection(String id) {
        return Optional.ofNullable(this.clientConnectionInfoMap.get(id));
    }

    /**
     * 添加客户端连接信息到服务器
     *
     * @param id 客户端连接的唯一标识
     * @param clientConnectionInfo 客户端连接信息对象
     */
    public void addClientConnectionInfo(String id, ClientConnectionInfo clientConnectionInfo) {
        clientConnectionInfoMap.put(id, clientConnectionInfo);
    }

    /**
     * 从服务器中移除客户端连接信息
     *
     * @param id 要移除的客户端连接的唯一标识
     * @return 被移除的客户端连接信息对象，如果不存在则返回null
     */
    public ClientConnectionInfo removeClientConnectionInfo(String id) {
        return this.clientConnectionInfoMap.remove(id);
    }

    /**
     * 添加代理请求Promise
     * 用于异步获取代理请求的响应结果
     * 同时设置定时任务，60秒后自动清理过期的Promise
     *
     * @param requestId 请求的唯一标识
     * @param promise Promise对象，用于异步获取响应
     */
    public void addProxyRequestPromise(String requestId, Promise<SimpleHttpResponse> promise) {
        // 将Promise存入Map
        this.proxyRequestPromiseMap.put(requestId, promise);
        // 创建定时任务，60秒后自动清理该Promise，防止内存泄漏
        workerGroup.schedule(new Runnable() {

            @Override
            public void run() {
                removeProxyRequestPromise(requestId);
            }

        }, 60, TimeUnit.SECONDS);
    }

    /**
     * 移除代理请求Promise
     *
     * @param requestId 要移除的请求的唯一标识
     */
    public void removeProxyRequestPromise(String requestId) {
        this.proxyRequestPromiseMap.remove(requestId);
    }

    /**
     * 根据请求ID查找代理请求Promise
     *
     * @param requestId 请求的唯一标识
     * @return Promise对象，如果不存在则返回null
     */
    public Promise<SimpleHttpResponse> findProxyRequestPromise(String requestId) {
        return this.proxyRequestPromiseMap.get(requestId);
    }

    /**
     * 获取是否启用SSL
     *
     * @return true表示启用SSL，false表示不启用
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * 设置是否启用SSL
     *
     * @param ssl true表示启用SSL，false表示不启用
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * 获取服务器监听的主机地址
     *
     * @return 主机地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置服务器监听的主机地址
     *
     * @param host 主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取服务器监听的端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置服务器监听的端口号
     *
     * @param port 端口号
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取Agent信息Map
     *
     * @return Agent信息Map，key为agentId，value为AgentInfo对象
     */
    public Map<String, AgentInfo> getAgentInfoMap() {
        return agentInfoMap;
    }

    /**
     * 设置Agent信息Map
     *
     * @param agentInfoMap Agent信息Map
     */
    public void setAgentInfoMap(Map<String, AgentInfo> agentInfoMap) {
        this.agentInfoMap = agentInfoMap;
    }

    /**
     * 获取客户端连接信息Map
     *
     * @return 客户端连接信息Map，key为连接ID，value为ClientConnectionInfo对象
     */
    public Map<String, ClientConnectionInfo> getClientConnectionInfoMap() {
        return clientConnectionInfoMap;
    }

    /**
     * 设置客户端连接信息Map
     *
     * @param clientConnectionInfoMap 客户端连接信息Map
     */
    public void setClientConnectionInfoMap(Map<String, ClientConnectionInfo> clientConnectionInfoMap) {
        this.clientConnectionInfoMap = clientConnectionInfoMap;
    }

    /**
     * 获取隧道集群存储对象
     *
     * @return 隧道集群存储对象
     */
    public TunnelClusterStore getTunnelClusterStore() {
        return tunnelClusterStore;
    }

    /**
     * 设置隧道集群存储对象
     *
     * @param tunnelClusterStore 隧道集群存储对象
     */
    public void setTunnelClusterStore(TunnelClusterStore tunnelClusterStore) {
        this.tunnelClusterStore = tunnelClusterStore;
    }

    /**
     * 获取客户端连接的主机地址
     *
     * @return 客户端连接的主机地址
     */
    public String getClientConnectHost() {
        return clientConnectHost;
    }

    /**
     * 设置客户端连接的主机地址
     *
     * @param clientConnectHost 客户端连接的主机地址
     */
    public void setClientConnectHost(String clientConnectHost) {
        this.clientConnectHost = clientConnectHost;
    }

    /**
     * 获取WebSocket访问路径
     *
     * @return WebSocket访问路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 设置WebSocket访问路径
     * 会自动处理路径格式，确保以"/"开头
     *
     * @param path WebSocket访问路径
     */
    public void setPath(String path) {
        // 去除首尾空格
        path = path.trim();
        // 检查路径是否以"/"开头，如果不是则自动添加
        if (!path.startsWith("/")) {
            logger.warn("tunnel server path should start with / ! path: {}, try to auto add / .", path);
            path = "/" + path;
        }
        this.path = path;
    }
}

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
 * 
 * @author hengyunabc 2019-08-09
 *
 */
public class TunnelServer {
    private final static Logger logger = LoggerFactory.getLogger(TunnelServer.class);

    private boolean ssl;
    private String host;
    private int port;
    private String path = ArthasConstants.DEFAULT_WEBSOCKET_PATH;

    private Map<String, AgentInfo> agentInfoMap = new ConcurrentHashMap<String, AgentInfo>();

    private Map<String, ClientConnectionInfo> clientConnectionInfoMap = new ConcurrentHashMap<String, ClientConnectionInfo>();
    
    /**
     * 记录 proxy request
     */
    private Map<String, Promise<SimpleHttpResponse>> proxyRequestPromiseMap = new ConcurrentHashMap<String, Promise<SimpleHttpResponse>>();

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-TunnelServer-boss", true));
    private EventLoopGroup workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-TunnelServer-worker", true));

    private Channel channel;

    /**
     * 在集群部署时，保存agentId和host关系
     */
    private TunnelClusterStore tunnelClusterStore;
    
    /**
     * 集群部署时外部连接的host
     */
    private String clientConnectHost;

    public void start() throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (ssl) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new TunnelSocketServerInitializer(this, sslCtx));

        if (StringUtils.isBlank(host)) {
            channel = b.bind(port).sync().channel();
        } else {
            channel = b.bind(host, port).sync().channel();
        }

        logger.info("Tunnel server listen at {}:{}", host, port);

        workerGroup.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                agentInfoMap.entrySet().removeIf(e -> !e.getValue().getChannelHandlerContext().channel().isActive());
                clientConnectionInfoMap.entrySet()
                        .removeIf(e -> !e.getValue().getChannelHandlerContext().channel().isActive());
                
                // 更新集群key信息
                if (tunnelClusterStore != null && clientConnectHost != null) {
                    try {
                        for (Entry<String, AgentInfo> entry : agentInfoMap.entrySet()) {
                            tunnelClusterStore.addAgent(entry.getKey(), new AgentClusterInfo(entry.getValue(), clientConnectHost), 60 * 60, TimeUnit.SECONDS);
                        }
                    } catch (Throwable t) {
                        logger.error("update tunnel info error", t);
                    }
                }
            }

        }, 60, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public Optional<AgentInfo> findAgent(String id) {
        return Optional.ofNullable(this.agentInfoMap.get(id));
    }

    public void addAgent(String id, AgentInfo agentInfo) {
        agentInfoMap.put(id, agentInfo);
        if (this.tunnelClusterStore != null) {
            this.tunnelClusterStore.addAgent(id, new AgentClusterInfo(agentInfo, clientConnectHost), 60 * 60, TimeUnit.SECONDS);
        }
    }

    public AgentInfo removeAgent(String id) {
        AgentInfo agentInfo = agentInfoMap.remove(id);
        if (this.tunnelClusterStore != null) {
            this.tunnelClusterStore.removeAgent(id);
        }
        return agentInfo;
    }
    
    public Optional<ClientConnectionInfo> findClientConnection(String id) {
        return Optional.ofNullable(this.clientConnectionInfoMap.get(id));
    }

    public void addClientConnectionInfo(String id, ClientConnectionInfo clientConnectionInfo) {
        clientConnectionInfoMap.put(id, clientConnectionInfo);
    }

    public ClientConnectionInfo removeClientConnectionInfo(String id) {
        return this.clientConnectionInfoMap.remove(id);
    }
    
    public void addProxyRequestPromise(String requestId, Promise<SimpleHttpResponse> promise) {
        this.proxyRequestPromiseMap.put(requestId, promise);
        // 把过期的proxy 请求删掉
        workerGroup.schedule(new Runnable() {

            @Override
            public void run() {
                removeProxyRequestPromise(requestId);
            }

        }, 60, TimeUnit.SECONDS);
    }

    public void removeProxyRequestPromise(String requestId) {
        this.proxyRequestPromiseMap.remove(requestId);
    }
    
    public Promise<SimpleHttpResponse> findProxyRequestPromise(String requestId) {
        return this.proxyRequestPromiseMap.get(requestId);
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Map<String, AgentInfo> getAgentInfoMap() {
        return agentInfoMap;
    }

    public void setAgentInfoMap(Map<String, AgentInfo> agentInfoMap) {
        this.agentInfoMap = agentInfoMap;
    }

    public Map<String, ClientConnectionInfo> getClientConnectionInfoMap() {
        return clientConnectionInfoMap;
    }

    public void setClientConnectionInfoMap(Map<String, ClientConnectionInfo> clientConnectionInfoMap) {
        this.clientConnectionInfoMap = clientConnectionInfoMap;
    }

    public TunnelClusterStore getTunnelClusterStore() {
        return tunnelClusterStore;
    }

    public void setTunnelClusterStore(TunnelClusterStore tunnelClusterStore) {
        this.tunnelClusterStore = tunnelClusterStore;
    }

    public String getClientConnectHost() {
        return clientConnectHost;
    }

    public void setClientConnectHost(String clientConnectHost) {
        this.clientConnectHost = clientConnectHost;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        path = path.trim();
        if (!path.startsWith("/")) {
            logger.warn("tunnel server path should start with / ! path: {}, try to auto add / .", path);
            path = "/" + path;
        }
        this.path = path;
    }
}

package com.alibaba.arthas.tunnel.server.app.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.arthas.tunnel.server.utils.InetAddressUtil;
import com.taobao.arthas.common.ArthasConstants;

/**
 * Arthas 配置属性类
 *
 * <p>这是一个 Spring Boot 配置属性类，用于从配置文件中读取 arthas 相关的配置。</p>
 * <p>支持的配置前缀为 "arthas"，例如在 application.yml 中：</p>
 * <pre>
 * arthas:
 *   server:
 *     host: 0.0.0.0
 *     port: 8080
 *   enable-detail-pages: true
 * </pre>
 *
 * <p>该类包含以下配置：</p>
 * <ul>
 *   <li>server：服务器配置（端口、主机、SSL 等）</li>
 *   <li>embeddedRedis：内嵌 Redis 配置</li>
 *   <li>enableDetailPages：是否启用详细页面</li>
 *   <li>enableIframeSupport：是否启用 iframe 支持</li>
 * </ul>
 *
 * @author hengyunabc 2019-08-29
 */
@Component
@ConfigurationProperties(prefix = "arthas")
public class ArthasProperties {

    /**
     * 服务器配置
     *
     * <p>包含隧道服务器的网络配置信息</p>
     */
    private Server server;

    /**
     * 内嵌 Redis 配置
     *
     * <p>用于测试环境的内嵌 Redis 服务器配置</p>
     */
    private EmbeddedRedis embeddedRedis;

    /**
     * 是否启用详细页面
     *
     * <p>启用后支持访问 apps.html 和 agents.html 等详细页面</p>
     * <p>默认值为 false</p>
     */
    private boolean enableDetailPages = false;

    /**
     * 是否启用 iframe 支持
     *
     * <p>控制是否允许在 iframe 中嵌入 Arthas 页面</p>
     * <p>默认值为 true</p>
     */
    private boolean enableIframeSupport = true;

    /**
     * 获取服务器配置对象
     *
     * @return 服务器配置对象，如果未配置则返回 null
     */
    public Server getServer() {
        return server;
    }

    /**
     * 设置服务器配置对象
     *
     * @param server 服务器配置对象
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * 获取内嵌 Redis 配置对象
     *
     * @return 内嵌 Redis 配置对象，如果未配置则返回 null
     */
    public EmbeddedRedis getEmbeddedRedis() {
        return embeddedRedis;
    }

    /**
     * 设置内嵌 Redis 配置对象
     *
     * @param embeddedRedis 内嵌 Redis 配置对象
     */
    public void setEmbeddedRedis(EmbeddedRedis embeddedRedis) {
        this.embeddedRedis = embeddedRedis;
    }

    /**
     * 判断是否启用详细页面
     *
     * @return 如果启用详细页面返回 true，否则返回 false
     */
    public boolean isEnableDetailPages() {
        return enableDetailPages;
    }

    /**
     * 设置是否启用详细页面
     *
     * @param enableDetailPages true 表示启用详细页面，false 表示禁用
     */
    public void setEnableDetailPages(boolean enableDetailPages) {
        this.enableDetailPages = enableDetailPages;
    }

    /**
     * 判断是否启用 iframe 支持
     *
     * @return 如果启用 iframe 支持返回 true，否则返回 false
     */
    public boolean isEnableIframeSupport() {
        return enableIframeSupport;
    }

    /**
     * 设置是否启用 iframe 支持
     *
     * @param enableIframeSupport true 表示启用 iframe 支持，false 表示禁用
     */
    public void setEnableIframeSupport(boolean enableIframeSupport) {
        this.enableIframeSupport = enableIframeSupport;
    }

    /**
     * 服务器配置内部类
     *
     * <p>封装隧道服务器的网络相关配置，包括监听地址、端口、SSL 等信息</p>
     */
    public static class Server {
        /**
         * 隧道服务器监听的主机地址
         *
         * <p>例如：0.0.0.0 表示监听所有网卡，127.0.0.1 表示仅本地访问</p>
         */
        private String host;

        /**
         * 隧道服务器监听的端口号
         */
        private int port;

        /**
         * 是否启用 SSL/TLS
         *
         * <p>true 表示使用安全的 WebSocket 连接（wss://），false 表示使用普通连接（ws://）</p>
         */
        private boolean ssl;

        /**
         * WebSocket 连接路径
         *
         * <p>默认值为 Arthas 常量中定义的默认 WebSocket 路径</p>
         */
        private String path = ArthasConstants.DEFAULT_WEBSOCKET_PATH;

        /**
         * 客户端连接的地址
         *
         * <p>该地址用于：</p>
         * <ol>
         *   <li>Agent 客户端连接到服务器时使用</li>
         *   <li>在部署 Tunnel Server 集群时，保存到 Redis 中供其他节点使用</li>
         * </ol>
         * <p>如果不配置，则会自动获取本机的网络地址</p>
         */
        private String clientConnectHost = InetAddressUtil.getInetAddress();

        /**
         * 获取监听的主机地址
         *
         * @return 监听的主机地址
         */
        public String getHost() {
            return host;
        }

        /**
         * 设置监听的主机地址
         *
         * @param host 要监听的主机地址
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * 获取监听的端口号
         *
         * @return 监听的端口号
         */
        public int getPort() {
            return port;
        }

        /**
         * 设置监听的端口号
         *
         * @param port 要监听的端口号
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * 判断是否启用 SSL
         *
         * @return 启用 SSL 返回 true，否则返回 false
         */
        public boolean isSsl() {
            return ssl;
        }

        /**
         * 设置是否启用 SSL
         *
         * @param ssl true 表示启用 SSL，false 表示不启用
         */
        public void setSsl(boolean ssl) {
            this.ssl = ssl;
        }

        /**
         * 获取客户端连接的主机地址
         *
         * @return 客户端连接时使用的主机地址
         */
        public String getClientConnectHost() {
            return clientConnectHost;
        }

        /**
         * 设置客户端连接的主机地址
         *
         * <p>在集群部署时，应设置为外网可访问的地址</p>
         *
         * @param clientConnectHost 客户端连接时使用的主机地址
         */
        public void setClientConnectHost(String clientConnectHost) {
            this.clientConnectHost = clientConnectHost;
        }

        /**
         * 获取 WebSocket 连接路径
         *
         * @return WebSocket 连接路径
         */
        public String getPath() {
            return path;
        }

        /**
         * 设置 WebSocket 连接路径
         *
         * @param path WebSocket 连接路径
         */
        public void setPath(String path) {
            this.path = path;
        }

    }

    /**
     * 内嵌 Redis 配置内部类
     *
     * <p>主要用于测试环境，避免依赖外部 Redis 服务</p>
     * <p>该配置允许在测试时启动一个内嵌的 Redis 服务器</p>
     *
     * @author hengyunabc 2020-11-03
     */
    public static class EmbeddedRedis {
        /**
         * 是否启用内嵌 Redis
         *
         * <p>默认值为 false，即不启用</p>
         */
        private boolean enabled = false;

        /**
         * Redis 监听的主机地址
         *
         * <p>默认值为 127.0.0.1，仅本地访问</p>
         */
        private String host = "127.0.0.1";

        /**
         * Redis 监听的端口号
         *
         * <p>默认值为 Redis 标准端口 6379</p>
         */
        private int port = 6379;

        /**
         * Redis 配置参数列表
         *
         * <p>用于传递额外的 Redis 配置参数</p>
         * <p>例如：maxmemory、maxmemory-policy 等</p>
         */
        private List<String> settings = new ArrayList<String>();

        /**
         * 判断是否启用内嵌 Redis
         *
         * @return 启用返回 true，否则返回 false
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用内嵌 Redis
         *
         * @param enabled true 表示启用，false 表示禁用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 Redis 监听的主机地址
         *
         * @return Redis 监听地址
         */
        public String getHost() {
            return host;
        }

        /**
         * 设置 Redis 监听的主机地址
         *
         * @param host Redis 监听地址
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * 获取 Redis 监听的端口号
         *
         * @return Redis 端口号
         */
        public int getPort() {
            return port;
        }

        /**
         * 设置 Redis 监听的端口号
         *
         * @param port Redis 端口号
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * 获取 Redis 配置参数列表
         *
         * @return Redis 配置参数列表
         */
        public List<String> getSettings() {
            return settings;
        }

        /**
         * 设置 Redis 配置参数列表
         *
         * @param settings Redis 配置参数列表
         */
        public void setSettings(List<String> settings) {
            this.settings = settings;
        }
    }

}

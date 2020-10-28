package com.alibaba.arthas.tunnel.server.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.utils.InetAddressUtil;

/**
 * 
 * @author hengyunabc 2019-08-29
 *
 */
@Component
@ConfigurationProperties(prefix = "arthas")
public class ArthasProperties {

    private Server server;

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public static class Server {
        /**
         * tunnel server listen host
         */
        private String host;
        private int port;
        private boolean ssl;
        private String path = TunnelServer.DEFAULT_WEBSOCKET_PATH;

        /**
         * 客户端连接的地址。也用于保存到redis里，当部署tunnel server集群里需要。不配置则会自动获取
         */
        private String clientConnectHost = InetAddressUtil.getInetAddress();

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

        public boolean isSsl() {
            return ssl;
        }

        public void setSsl(boolean ssl) {
            this.ssl = ssl;
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
            this.path = path;
        }

    }

}

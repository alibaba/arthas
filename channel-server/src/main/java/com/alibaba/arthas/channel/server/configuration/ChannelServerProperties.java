package com.alibaba.arthas.channel.server.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "channel.server")
public class ChannelServerProperties {

    private Server websocket;
    private Server backend;

    public Server getWebsocket() {
        return websocket;
    }

    public void setWebsocket(Server websocket) {
        this.websocket = websocket;
    }

    public Server getBackend() {
        return backend;
    }

    public void setBackend(Server backend) {
        this.backend = backend;
    }

    public static class Server {
        private String host;
        private int port;
        private boolean ssl;
        private boolean enabled = true;

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

}

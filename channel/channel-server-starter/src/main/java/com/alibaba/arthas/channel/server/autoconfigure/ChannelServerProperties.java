package com.alibaba.arthas.channel.server.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "channel.server")
public class ChannelServerProperties {

    private Server websocket = new Server();
    private Server backend = new Server();

    private Agent agent = new Agent();

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

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public static class Server {
        private String host;
        private int port;
        private boolean ssl;
        private boolean enabled;

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

    public static class Agent {
        private int cleanIntervalMills = 5000;
        private int outOfServiceTimeoutMills = 15*1000;
        private int downTimeoutMills = 30*1000;
        private int removingTimeoutMills = 600*1000;

        public int getCleanIntervalMills() {
            return cleanIntervalMills;
        }

        public void setCleanIntervalMills(int cleanIntervalMills) {
            this.cleanIntervalMills = cleanIntervalMills;
        }

        public int getOutOfServiceTimeoutMills() {
            return outOfServiceTimeoutMills;
        }

        public void setOutOfServiceTimeoutMills(int outOfServiceTimeoutMills) {
            this.outOfServiceTimeoutMills = outOfServiceTimeoutMills;
        }

        public int getDownTimeoutMills() {
            return downTimeoutMills;
        }

        public void setDownTimeoutMills(int downTimeoutMills) {
            this.downTimeoutMills = downTimeoutMills;
        }

        public int getRemovingTimeoutMills() {
            return removingTimeoutMills;
        }

        public void setRemovingTimeoutMills(int removingTimeoutMills) {
            this.removingTimeoutMills = removingTimeoutMills;
        }
    }

}

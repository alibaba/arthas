package com.alibaba.arthas.channel.server.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ChannelServerProperties.PREFIX)
public class ChannelServerProperties {

    public static final String PREFIX = "arthas.channel.server";
    private Server websocket = new Server();
    private Server backend = new Server();
    private MessageExchange messageExchange = new MessageExchange();

    private AgentCleaner agentCleaner = new AgentCleaner();

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

    public AgentCleaner getAgentCleaner() {
        return agentCleaner;
    }

    public void setAgentCleaner(AgentCleaner agentCleaner) {
        this.agentCleaner = agentCleaner;
    }

    public MessageExchange getMessageExchange() {
        return messageExchange;
    }

    public void setMessageExchange(MessageExchange messageExchange) {
        this.messageExchange = messageExchange;
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

    public static class AgentCleaner {
        private int cleanIntervalMills = 5000;
        private int outOfServiceTimeoutMills = 15000;
        private int downTimeoutMills = 30000;
        private int removingTimeoutMills = 1800000;
        private boolean enabled = true;

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class MessageExchange {
        private int topicSurvivalTimeMills = 60000;
        private int topicCapacity = 1000;

        public int getTopicSurvivalTimeMills() {
            return topicSurvivalTimeMills;
        }

        /**
         * message topic survival seconds
         */
        public void setTopicSurvivalTimeMills(int topicSurvivalTimeMills) {
            this.topicSurvivalTimeMills = topicSurvivalTimeMills;
        }

        public int getTopicCapacity() {
            return topicCapacity;
        }

        /**
         * message queue capacity
         */
        public void setTopicCapacity(int topicCapacity) {
            this.topicCapacity = topicCapacity;
        }
    }

}

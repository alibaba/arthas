package com.alibaba.arthas.channel.server.message;

import com.alibaba.arthas.channel.server.message.topic.Topic;

/**
 * @author gongdewei 2020/8/10
 */
public interface MessageExchangeService {

    void createTopic(Topic topic) throws MessageExchangeException;

    void removeTopic(Topic topic) throws MessageExchangeException;

    /**
     * clean all topics of agent
     * @param agentId
     */
    void removeTopicsOfAgent(String agentId);

    void pushMessage(Topic topic, byte[] messageBytes) throws MessageExchangeException;

    byte[] pollMessage(Topic topic, int timeout) throws MessageExchangeException;

    void subscribe(Topic topic, MessageHandler messageHandler) throws MessageExchangeException;

    void subscribe(Topic topic, int timeout, MessageHandler messageHandler) throws MessageExchangeException;

    void unsubscribe(Topic topic, MessageHandler messageHandler) throws MessageExchangeException;

    interface MessageHandler {
        /**
         * handle message
         * @param messageBytes message bytes
         * @return return true if process next message, false if break processing
         */
        boolean onMessage(byte[] messageBytes);

        void onTimeout();
    }
}

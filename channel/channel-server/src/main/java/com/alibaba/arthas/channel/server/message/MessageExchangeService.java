package com.alibaba.arthas.channel.server.message;

import com.alibaba.arthas.channel.server.message.topic.Topic;
import reactor.core.publisher.Mono;

/**
 * Message exchange service for channel-client and channel-server
 * @author gongdewei 2020/8/10
 */
public interface MessageExchangeService {

    void start() throws MessageExchangeException;

    void stop() throws MessageExchangeException;

    void createTopic(Topic topic) throws MessageExchangeException;

    void removeTopic(Topic topic) throws MessageExchangeException;

    /**
     * clean all topics of agent
     * @param agentId
     */
    void removeTopicsOfAgent(String agentId);

    void pushMessage(Topic topic, byte[] messageBytes) throws MessageExchangeException;

    Mono<byte[]> pollMessage(Topic topic, int timeout);

    void subscribe(Topic topic, MessageHandler messageHandler) throws MessageExchangeException;

    void subscribe(Topic topic, int timeout, MessageHandler messageHandler) throws MessageExchangeException;

    interface MessageHandler {
        /**
         * handle message
         * @param messageBytes message bytes
         * @return return true if process next message, false if break processing
         */
        boolean onMessage(byte[] messageBytes);

        boolean onTimeout();
    }
}

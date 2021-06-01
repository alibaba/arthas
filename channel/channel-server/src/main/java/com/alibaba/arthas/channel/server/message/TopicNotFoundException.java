package com.alibaba.arthas.channel.server.message;

/**
 * Topic not found exception
 */
public class TopicNotFoundException extends MessageExchangeException {

    public TopicNotFoundException(String message) {
        super(message);
    }

}

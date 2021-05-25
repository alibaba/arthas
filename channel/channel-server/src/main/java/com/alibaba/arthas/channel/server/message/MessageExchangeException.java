package com.alibaba.arthas.channel.server.message;

public class MessageExchangeException extends Exception {
    public MessageExchangeException(String message) {
        super(message);
    }

    public MessageExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}

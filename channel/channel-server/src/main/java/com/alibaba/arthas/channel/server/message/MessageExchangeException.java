package com.alibaba.arthas.channel.server.message;

/**
 * @author gongdewei 2020/8/13
 */
public class MessageExchangeException extends Exception {
    public MessageExchangeException(String message) {
        super(message);
    }

    public MessageExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}

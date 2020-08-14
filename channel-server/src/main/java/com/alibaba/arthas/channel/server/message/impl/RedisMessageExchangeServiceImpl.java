package com.alibaba.arthas.channel.server.message.impl;

import com.alibaba.arthas.channel.server.message.MessageExchangeException;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.topic.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Message exchange for cluster channel server
 * @author gongdewei 2020/8/10
 */
public class RedisMessageExchangeServiceImpl implements MessageExchangeService {

    @Autowired
    private RedisTemplate<String, byte[]> redisTemplate;

    @Autowired
    private ScheduledExecutorService executorService;

    @Override
    public void createTopic(Topic topic) throws MessageExchangeException {
        //redisTemplate.opsForList().
    }

    @Override
    public void removeTopic(Topic topic) throws MessageExchangeException {

    }

    @Override
    public void pushMessage(Topic topic, byte[] messageBytes) throws MessageExchangeException {
        redisTemplate.opsForList().leftPush(topic.getTopic(), messageBytes);
    }

    @Override
    public byte[] pollMessage(Topic topic, int timeout) throws MessageExchangeException {
        return redisTemplate.opsForList().rightPop(topic.getTopic());
    }

    @Override
    public void subscribe(Topic topic, MessageHandler messageHandler) throws MessageExchangeException {
        this.subscribe(topic, 30000, messageHandler);
    }

    @Override
    public void subscribe(final Topic topic, final int timeout, final MessageHandler messageHandler) throws MessageExchangeException {

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] messageBytes = redisTemplate.opsForList().rightPop(topic.getTopic(), timeout, TimeUnit.MILLISECONDS);
                    if (messageBytes != null) {
                        boolean next = messageHandler.onMessage(messageBytes);
                        if (!next) {
                            break;
                        }
                    }else {
                        messageHandler.onTimeout();
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void unsubscribe(Topic topic, MessageHandler messageHandler) throws MessageExchangeException {
        //TODO unsubscribe
    }
}

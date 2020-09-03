package com.alibaba.arthas.channel.server.redis;

import com.alibaba.arthas.channel.server.message.MessageExchangeException;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.topic.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Message exchange for cluster channel server
 * @author gongdewei 2020/8/10
 */
public class RedisMessageExchangeServiceImpl implements MessageExchangeService {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageExchangeServiceImpl.class);

    private static final String topicPrefix = "arthas:channel:topics:agent:";


    @Autowired
    private RedisTemplate<String, byte[]> redisTemplate;

    @Autowired
    private ScheduledExecutorService executorService;

    @Override
    public void createTopic(Topic topic) throws MessageExchangeException {
        //do nothing
    }

    @Override
    public void removeTopic(Topic topic) throws MessageExchangeException {
        redisTemplate.delete(topic.getTopic());
    }

    @Override
    public void removeTopicsOfAgent(String agentId) {
        List<Topic> removingTopics = new ArrayList<>();

        //TODO scan topic and clean
        for (Topic topic : removingTopics) {
            try {
                removeTopic(topic);
            } catch (Exception e) {
                logger.error("remove topic failure: {}", topic, e);
            }
        }
    }

    @Override
    public void pushMessage(Topic topic, byte[] messageBytes) throws MessageExchangeException {
        String key = topic.getTopic();
        redisTemplate.opsForList().leftPush(key, messageBytes);
        redisTemplate.expire(key, 10000, TimeUnit.MILLISECONDS);
    }

    @Override
    public byte[] pollMessage(Topic topic, int timeout) throws MessageExchangeException {
        return redisTemplate.opsForList().rightPop(topic.getTopic(), timeout, TimeUnit.MILLISECONDS);
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
                    try {
                        byte[] messageBytes = redisTemplate.opsForList().rightPop(topic.getTopic(), timeout, TimeUnit.MILLISECONDS);
                        if (messageBytes != null) {
                            boolean next = messageHandler.onMessage(messageBytes);
                            if (!next) {
                                logger.debug("message handler interrupted: {}", topic);
                                break;
                            }
                        }else {
                            messageHandler.onTimeout();
                            logger.debug("subscribe message timeout: {}", topic);
                            break;
                        }
                    } catch (Throwable e) {
                        if (e instanceof QueryTimeoutException) {
                            //ignore Redis command timed out
                        } else {
                            logger.error("blocking pop message failure: {}", topic, e);
                        }
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

package com.alibaba.arthas.channel.server.redis;

import com.alibaba.arthas.channel.server.conf.ScheduledExecutorConfig;
import com.alibaba.arthas.channel.server.message.MessageExchangeException;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.topic.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Message exchange for cluster channel server
 * @author gongdewei 2020/8/10
 */
public class RedisMessageExchangeServiceImpl implements MessageExchangeService {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageExchangeServiceImpl.class);

    private static final String topicPrefix = "arthas:channel:topics:agent:";

    // topic survival time ms
    private int topicSurvivalTimeMills = 60*1000;

    // topic message queue capacity
    private int topicCapacity = 1000;

    public RedisMessageExchangeServiceImpl() {
    }

    public RedisMessageExchangeServiceImpl(int topicSurvivalTimeMills, int topicCapacity) {
        this.topicSurvivalTimeMills = topicSurvivalTimeMills;
        this.topicCapacity = topicCapacity;
    }

    @Autowired
    private ReactiveRedisTemplate<String, byte[]> redisTemplate;

    @Autowired
    private ScheduledExecutorConfig executorServiceConfig;

    @Override
    public void start() throws MessageExchangeException {
        //do nothing
    }

    @Override
    public void stop() throws MessageExchangeException {
        //do nothing
    }

    @Override
    public void createTopic(Topic topic) throws MessageExchangeException {
        //do nothing
    }

    @Override
    public void removeTopic(Topic topic) throws MessageExchangeException {
        redisTemplate.delete(topic.getTopic()).subscribe();
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
    public boolean containsTopic(Topic topic) {
        return redisTemplate.hasKey(topic.getTopic()).block();
    }

    @Override
    public void pushMessage(Topic topic, byte[] messageBytes) throws MessageExchangeException {
        String key = topic.getTopic();
        redisTemplate.opsForList().leftPush(key, messageBytes).doOnSuccess(value -> {
            redisTemplate.expire(key, Duration.ofMillis(topicSurvivalTimeMills)).subscribe();
        }).subscribe();

        //TODO check topic capacity
    }

    @Override
    public Mono<byte[]> pollMessage(Topic topic, int timeout) {
        return redisTemplate.opsForList().rightPop(topic.getTopic(), Duration.ofMillis(timeout));
    }

    @Override
    public void subscribe(Topic topic, MessageHandler messageHandler) {
        this.subscribe(topic, 30000, messageHandler);
    }

    @Override
    public void subscribe(final Topic topic, final int timeout, final MessageHandler messageHandler) {

        Mono<byte[]> mono = redisTemplate.opsForList().rightPop(topic.getTopic(), Duration.ofMillis(timeout));
        mono.doOnSuccess(messageBytes -> {
            //schedule running, avoid blocking redis reactive in messageHandler callback
            executorServiceConfig.getExecutorService().submit(() -> {
                if (messageBytes != null) {
                    boolean next = messageHandler.onMessage(messageBytes);
                    if (next) {
                        subscribe(topic, timeout, messageHandler);
                    }
                } else {
                    boolean next = messageHandler.onTimeout();
                    if (next) {
                        subscribe(topic, timeout, messageHandler);
                    }
                }
            });
        }).doOnError(throwable -> {
            executorServiceConfig.getExecutorService().submit(() -> {
                if (throwable instanceof QueryTimeoutException) {
                    //ignore Redis command timed out
                    subscribe(topic, timeout, messageHandler);
                } else {
                    logger.error("blocking pop message failure: {}", topic, throwable);
                }
            });
        }).subscribe();

    }

    public int getTopicSurvivalTimeMills() {
        return topicSurvivalTimeMills;
    }

    public void setTopicSurvivalTimeMills(int topicSurvivalTimeMills) {
        this.topicSurvivalTimeMills = topicSurvivalTimeMills;
    }

    public int getTopicCapacity() {
        return topicCapacity;
    }

    public void setTopicCapacity(int topicCapacity) {
        this.topicCapacity = topicCapacity;
    }
}

package com.alibaba.arthas.channel.server.message.impl;

import com.alibaba.arthas.channel.server.conf.ScheduledExecutorConfig;
import com.alibaba.arthas.channel.server.message.MessageExchangeException;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.TopicNotFoundException;
import com.alibaba.arthas.channel.server.message.topic.ActionRequestTopic;
import com.alibaba.arthas.channel.server.message.topic.Topic;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Message exchange for standalone channel server
 * @author gongdewei 2020/8/10
 */
public class MessageExchangeServiceImpl implements MessageExchangeService {

    private static final Logger logger = LoggerFactory.getLogger(MessageExchangeServiceImpl.class);

    private Map<Topic, TopicData> topicMap = new ConcurrentHashMap<Topic, TopicData>();

    @Autowired
    private ScheduledExecutorConfig executorServiceConfig;
    private ScheduledFuture<?> scheduledFuture;

    // topic survival time ms
    private int topicSurvivalTimeMills = 60*1000;

    // topic message queue capacity
    private int topicCapacity = 1000;

    public MessageExchangeServiceImpl() {
    }

    public MessageExchangeServiceImpl(int topicCapacity, int topicSurvivalTimeMills) {
        this.topicCapacity = topicCapacity;
        this.topicSurvivalTimeMills = topicSurvivalTimeMills;
    }

    @Override
    public void start() throws MessageExchangeException {
        scheduledFuture = executorServiceConfig.getExecutorService().scheduleWithFixedDelay(() -> {
            try {
                cleanIdleTopics(topicSurvivalTimeMills);
            } catch (Exception e) {
                logger.error("clean idle topics failure", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void stop() throws MessageExchangeException {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    public void createTopic(Topic topic) throws MessageExchangeException {
        if (!topicMap.containsKey(topic)) {
            topicMap.put(topic, new TopicData(topic));
        }
    }

    @Override
    public void removeTopic(Topic topic) throws MessageExchangeException {
        topicMap.remove(topic);
        logger.debug("remove topic: {}", topic);
    }

    @Override
    public void removeTopicsOfAgent(String agentId) {
        List<Topic> removingTopics = new ArrayList<>();
        for (Topic topic : topicMap.keySet()) {
            if (StringUtils.equals(topic.getAgentId(), agentId)) {
                removingTopics.add(topic);
            }
        }
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
        return topicMap.containsKey(topic);
    }

    @Override
    public void pushMessage(Topic topic, byte[] messageBytes) throws MessageExchangeException {
        TopicData topicData = topicMap.get(topic);
        if (topicData == null) {
            createTopic(topic);
            topicData = getAndCheckTopicExists(topic);
        }
        try {
            topicData.pushMessage(messageBytes);
        } catch (Throwable e) {
            throw new MessageExchangeException("push message failure", e);
        }
    }

    private TopicData getAndCheckTopicExists(Topic topic) throws MessageExchangeException {
        TopicData topicData = topicMap.get(topic);
        if (topicData == null) {
            throw new TopicNotFoundException("topic is not exists: " + topic);
        }
        return topicData;
    }

    @Override
    public Mono<byte[]> pollMessage(Topic topic, int timeout) {
        try {
            TopicData topicData = getAndCheckTopicExists(topic);
            return Mono.justOrEmpty(topicData.messageQueue.poll(timeout, TimeUnit.MILLISECONDS));
        } catch (Throwable e) {
            if (e instanceof TopicNotFoundException) {
                return Mono.error(e);
            } else {
                return Mono.error(new MessageExchangeException("poll message failure: " + e.getMessage(), e));
            }
        }
    }

    @Override
    public void subscribe(Topic topic, MessageHandler messageHandler) throws MessageExchangeException {
        this.subscribe(topic, 30000, messageHandler);
    }

    @Override
    public void subscribe(final Topic topic, final int timeout, final MessageHandler messageHandler) throws MessageExchangeException {
        TopicData topicData = topicMap.get(topic);
        if (topicData == null) {
            createTopic(topic);
            topicData = getAndCheckTopicExists(topic);
        }

        TopicData finalTopicData = topicData;
        topicData.toFlux(timeout)
                .takeWhile(bytes -> messageHandler.onMessage(bytes))
                .doOnError(throwable -> {
                    //must unsubscribe before new subscribe
                    finalTopicData.unsubscribe();
                    if (throwable instanceof TimeoutException) {
                        boolean next = messageHandler.onTimeout();
                        if (next) {
                            executorServiceConfig.getExecutorService().submit(() -> {
                                try {
                                    subscribe(topic, timeout, messageHandler);
                                } catch (MessageExchangeException e) {
                                    logger.error("subscribe message error", throwable);
                                }
                            });
                        }
                    } else {
                        logger.error("subscribe message error", throwable);
                    }
                }).doOnComplete(() -> {
                    finalTopicData.unsubscribe();
                }).doOnCancel(() -> {
                    finalTopicData.unsubscribe();
                }).subscribe();

//                final TopicData finalTopicData = topicData;
//        executorServiceConfig.getExecutorService().submit(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        byte[] messageBytes = finalTopicData.messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
//                        if (messageBytes != null) {
//                            boolean next = messageHandler.onMessage(messageBytes);
//                            if (!next) {
//                                break;
//                            }
//                        }else {
//                            boolean next = messageHandler.onTimeout();
//                            if (!next) {
//                                break;
//                            }
//                        }
//                    } catch (InterruptedException e) {
//                        logger.warn("subscribe message is interrupted");
//                        break;
//                    }
//                }
//                // remove message handler
//                finalTopicData.unregisterMessageHandler(messageHandler);
//            }
//        });

    }

    public void cleanIdleTopics(int timeout) {
        long now = System.currentTimeMillis();
        List<TopicData> topicDataList = new ArrayList<>(topicMap.values());
        for (TopicData topicData : topicDataList) {
//            if (topicData.topic instanceof ActionRequestTopic) {
//                continue;
//            }
            long idle = now - topicData.getLastActiveTime();
            if (!topicData.isSubscribed() && idle > timeout) {
                try {
                    logger.debug("cleaning idle topic: {}, idle time: {}", topicData.topic, idle);
                    removeTopic(topicData.topic);
                } catch (Exception e) {
                    logger.error("clean topic failure, topic: "+ topicData.topic, e);
                }
            }
        }
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

    class TopicData {
        private BlockingQueue<byte[]> messageQueue;
        private Topic topic;
        private long createTime;
        private long lastActiveTime;
        private volatile FluxSink<byte[]> emitter;

        public TopicData(Topic topic) {
            this.topic = topic;
            messageQueue = new LinkedBlockingQueue<byte[]>(topicCapacity);
            createTime = System.currentTimeMillis();
            lastActiveTime = createTime;
        }

        synchronized public void pushMessage(byte[] bytes) throws InterruptedException {
            if (this.emitter != null) {
                this.emitter.next(bytes);
            } else {
                messageQueue.put(bytes);
            }
            lastActiveTime = System.currentTimeMillis();
        }

        public Flux<byte[]> toFlux(int timeout) {
            lastActiveTime = System.currentTimeMillis();
            logger.debug("subscribe topic: {}", topic);
            return Flux.fromIterable(messageQueue)
                    .concatWith(Flux.create(emitter -> {
                        synchronized (TopicData.this) {
                            this.emitter = emitter;
                        }
                    }))
                    .timeout(Duration.ofMillis(timeout));
        }

        synchronized public void unsubscribe() {
            lastActiveTime = System.currentTimeMillis();
            this.emitter = null;
            logger.debug("unsubscribe topic: {}", topic);
        }

        public boolean isSubscribed() {
            return this.emitter != null;
        }

        public long getCreateTime() {
            return createTime;
        }

        public long getLastActiveTime() {
            return lastActiveTime;
        }

    }
}

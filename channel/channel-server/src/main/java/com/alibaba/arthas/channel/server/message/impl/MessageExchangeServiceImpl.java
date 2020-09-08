package com.alibaba.arthas.channel.server.message.impl;

import com.alibaba.arthas.channel.server.conf.ScheduledExecutorConfig;
import com.alibaba.arthas.channel.server.message.MessageExchangeException;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.topic.Topic;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Message exchange for standalone channel server
 * @author gongdewei 2020/8/10
 */
public class MessageExchangeServiceImpl implements MessageExchangeService {

    private static final Logger logger = LoggerFactory.getLogger(MessageExchangeServiceImpl.class);

    private Map<Topic, TopicData> topicMap = new ConcurrentHashMap<Topic, TopicData>();

    @Autowired
    private ScheduledExecutorConfig executorServiceConfig;

    @Override
    public void createTopic(Topic topic) throws MessageExchangeException {
        if (!topicMap.containsKey(topic)) {
            topicMap.put(topic, new TopicData(topic));
        }
    }

    @Override
    public void removeTopic(Topic topic) throws MessageExchangeException {
        topicMap.remove(topic);
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
    public void pushMessage(Topic topic, byte[] messageBytes) throws MessageExchangeException {
        TopicData topicData = topicMap.get(topic);
        if (topicData == null) {
            createTopic(topic);
            topicData = getAndCheckTopicExists(topic);
        }
        try {
            topicData.messageQueue.put(messageBytes);
        } catch (Throwable e) {
            throw new MessageExchangeException("push message failure", e);
        }
    }

    private TopicData getAndCheckTopicExists(Topic topic) throws MessageExchangeException {
        TopicData topicData = topicMap.get(topic);
        if (topicData == null) {
            throw new MessageExchangeException("topic is not exists");
        }
        return topicData;
    }

    @Override
    public byte[] pollMessage(Topic topic, int timeout) throws MessageExchangeException {
        TopicData topicData = getAndCheckTopicExists(topic);
        try {
            return topicData.messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            throw new MessageExchangeException("poll message failure", e);
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

        topicData.setMessageHandler(messageHandler);
        topicData.setTimeout(timeout);

        final TopicData finalTopicData = topicData;
        executorServiceConfig.getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] messageBytes = finalTopicData.messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
                        if (messageBytes != null) {
                            boolean next = messageHandler.onMessage(messageBytes);
                            if (!next) {
                                break;
                            }
                        }else {
                            messageHandler.onTimeout();
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // remove message handler
                    if (finalTopicData.getMessageHandler() == messageHandler){
                        finalTopicData.setMessageHandler(null);
                    }
                }
            }
        });
    }

    @Override
    public void unsubscribe(Topic topic, MessageHandler messageHandler) throws MessageExchangeException {
        TopicData topicData = topicMap.get(topic);
        //TODO CAS lock
        if (topicData != null && topicData.getMessageHandler() == messageHandler) {
            topicData.setMessageHandler(null);
        }
    }

    static class TopicData {
        private BlockingQueue<byte[]> messageQueue;
        private MessageHandler messageHandler;
        private Topic topic;
        private long createTime;
        private long updatedTime;
        private long timeout;

        public TopicData(Topic topic) {
            this.topic = topic;
            messageQueue = new LinkedBlockingQueue<byte[]>(1000);
            createTime = System.currentTimeMillis();
            updatedTime = createTime;
            timeout = -1;
        }

        public BlockingQueue<byte[]> getMessageQueue() {
            return messageQueue;
        }

        public MessageHandler getMessageHandler() {
            return messageHandler;
        }

        public void setMessageHandler(MessageHandler messageHandler) {
            this.messageHandler = messageHandler;
        }

        public long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }

        public long getUpdatedTime() {
            return updatedTime;
        }

        public void setUpdatedTime(long updatedTime) {
            this.updatedTime = updatedTime;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }
}

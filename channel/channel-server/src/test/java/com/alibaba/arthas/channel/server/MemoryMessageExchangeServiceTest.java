package com.alibaba.arthas.channel.server;

import com.alibaba.arthas.channel.server.message.MessageExchangeException;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.impl.MessageExchangeServiceImpl;
import com.alibaba.arthas.channel.server.message.topic.ActionRequestTopic;
import com.alibaba.arthas.channel.server.message.topic.Topic;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2021/7/22
 */
public class MemoryMessageExchangeServiceTest {

    @Test
    public void testPushMessages() throws MessageExchangeException {
        Topic topic = new ActionRequestTopic("agent_1234324");

        List<byte[]> recvRequests = new ArrayList<>();
        MessageExchangeServiceImpl messageExchangeService = new MessageExchangeServiceImpl();
        messageExchangeService.subscribe(topic, new MessageExchangeService.MessageHandler() {
            @Override
            public boolean onMessage(byte[] messageBytes) {
                recvRequests.add(messageBytes);
                System.out.println("recv: " + new String(messageBytes));
                return true;
            }

            @Override
            public boolean onTimeout() {
                return false;
            }
        });

        byte[] requestBytes1 = "Request1".getBytes();
        byte[] requestBytes2 = "Request2".getBytes();

        System.out.println("send: "+new String(requestBytes1));
        messageExchangeService.pushMessage(topic, requestBytes1);

        System.out.println("send: "+new String(requestBytes2));
        messageExchangeService.pushMessage(topic, requestBytes2);

        System.out.println(" total recv: " + recvRequests.size());
    }
}

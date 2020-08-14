package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.ResponseStatus;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author gongdewei 2020/8/14
 */
public class ChannelClientTest {

    public static void main(String[] args) throws IOException {

        final ChannelClient channelClient = new ChannelClient("localhost", 7700);
        channelClient.setAgentService(new TestAgentServiceImpl());
        channelClient.setExecutorService(getExecutorService());
        channelClient.setRequestListener(new ChannelClient.RequestListener() {
            @Override
            public void onRequest(ActionRequest request) {
                System.out.println("request: "+request);

                try {
                    channelClient.submitResponse(ActionResponse.newBuilder()
                            .setStatus(ResponseStatus.SUCCEEDED)
                            .setAgentId(request.getAgentId())
                            .setRequestId(request.getRequestId())
                            .setSessionId(request.getSessionId())
                            .setConsumerId(request.getConsumerId())
                            .build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        channelClient.start();

        System.in.read();
    }

    private static ScheduledExecutorService getExecutorService() {
        return Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "arthas-channel-client-schedule");
                t.setDaemon(true);
                return t;
            }
        });
    }
}

package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.ExecuteParams;
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
        ScheduledExecutorService executorService = getExecutorService();
        channelClient.setExecutorService(executorService);
        channelClient.setRequestListener(new RequestHandler(channelClient, executorService));

        channelClient.start();

        System.in.read();
    }

    private static class RequestHandler implements ChannelClient.RequestListener {

        private ChannelClient channelClient;
        private ScheduledExecutorService executorService;

        public RequestHandler(ChannelClient channelClient, ScheduledExecutorService executorService) {
            this.channelClient = channelClient;
            this.executorService = executorService;
        }

        @Override
        public void onRequest(ActionRequest request) {
            System.out.println("request: " + request);

            switch (request.getAction()) {
                case EXECUTE:
                    handleExec(request);
                    return;
//                case INTERRUPT_JOB:
//                    return;
//                case INIT_SESSION:
//                    return;
            }
            mockOneTimeCommand(request);
        }

        private void handleExec(ActionRequest request) {
            ExecuteParams executeParams = request.getExecuteParams();
            String commandLine = executeParams.getCommandLine();

            if (commandLine.startsWith("watch")) {
                mockStreamCommand(request);
            } else {
                mockOneTimeCommand(request);
            }
        }

        private void mockOneTimeCommand(ActionRequest request) {
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

        private void mockStreamCommand(final ActionRequest request) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {

                    for (int i = 0; i < 30; i++) {
                        try {
                            channelClient.submitResponse(ActionResponse.newBuilder()
                                    .setStatus(ResponseStatus.CONTINUOUS)
                                    .setAgentId(request.getAgentId())
                                    .setRequestId(request.getRequestId())
                                    .setSessionId(request.getSessionId())
                                    .setConsumerId(request.getConsumerId())
                                    .build());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        }

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

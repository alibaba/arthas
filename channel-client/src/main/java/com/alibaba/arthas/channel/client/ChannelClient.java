package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.ArthasServiceGrpc;
import com.alibaba.arthas.channel.proto.GeneralResult;
import com.alibaba.arthas.channel.proto.RegisterResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author gongdewei 2020/8/14
 */
public class ChannelClient {

    private static final Logger logger = LoggerFactory.getLogger(ChannelClient.class);

    private AgentService agentService;
    private RequestListener requestListener;
    private StreamObserver<ActionResponse> responseStreamObserver;
    private ScheduledExecutorService executorService;
    private volatile boolean isError;
    private String host;
    private int port;

    public ChannelClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        scheduleReconnectTask();

        try {
            connect();
        } catch (Exception e) {
            logger.error("connect failure", e);
        }
    }

    public void stop() {
        //TODO stop
        // cancel schedule task
    }

    private void connect() {
        logger.info("Connecting to channel server [{}:{}] ..", host, port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
        ArthasServiceGrpc.ArthasServiceStub arthasServiceStub = ArthasServiceGrpc.newStub(channel);

        //register
        AgentInfo agentInfo = agentService.getAgentInfo();
        arthasServiceStub.register(agentInfo, new StreamObserver<RegisterResult>() {
            @Override
            public void onNext(RegisterResult value) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });


        //acquireRequest
        arthasServiceStub.acquireRequest(agentInfo, new StreamObserver<ActionRequest>() {
            @Override
            public void onNext(ActionRequest value) {
                try {
                    requestListener.onRequest(value);
                } catch (Throwable e) {
                    logger.error("handle request failure", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                onClientError("acquireRequest error", t);
            }

            @Override
            public void onCompleted() {
                onClientError("acquireRequest completed", null);
            }
        });

        //submit result
        responseStreamObserver = arthasServiceStub.submitResponse(new StreamObserver<GeneralResult>() {
            @Override
            public void onNext(GeneralResult value) {

            }

            @Override
            public void onError(Throwable t) {
                onClientError("submitResponse on error", t);
            }

            @Override
            public void onCompleted() {
                onClientError("submitResponse completed", null);
            }
        });

        isError = false;
        logger.info("Channel client is ready.");
    }

    public void submitResponse(ActionResponse response) throws Exception {
        checkConnection();

        try {
            responseStreamObserver.onNext(response);
        } catch (Exception e) {
            logger.error("submit response failure", e);
            onClientError("send response failure", e);
            throw e;
        }
    }

    private void scheduleReconnectTask() {
        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isError) {
                        connect();
                    }
                } catch (Throwable e) {
                    logger.error("reconnect failure", e);
                }
            }
        }, 10, 1, TimeUnit.SECONDS);

    }

    private void onClientError(String message, Throwable t) {
        isError = true;
        logger.error("Channel client is error: "+message, t);
    }

    private void checkConnection() throws Exception {
        if (responseStreamObserver == null || isError) {
            throw new Exception("Channel is not ready");
        }
    }

    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public interface RequestListener {
        void onRequest(ActionRequest request);
    }
}

package com.alibaba.arthas.channel.client;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.proto.ArthasServiceGrpc;
import com.alibaba.arthas.channel.proto.GeneralResult;
import com.alibaba.arthas.channel.proto.HeartbeatRequest;
import com.alibaba.arthas.channel.proto.HeartbeatResponse;
import com.alibaba.arthas.channel.proto.RegisterResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
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

    private AgentInfoService agentInfoService;
    private RequestListener requestListener;
    private ScheduledExecutorService executorService;
    private volatile boolean isError;
    private String host;
    private int port;
    private ArthasServiceGrpc.ArthasServiceStub arthasServiceStub;
    private StreamObserver<ActionResponse> responseStreamObserver;
    private ManagedChannel channel;
    private ScheduledFuture<?> reconnectFuture;

    public ChannelClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        isError = true;
        try {
            connect();
        } catch (Exception e) {
            logger.error("connect failure", e);
        }
        scheduleReconnectTask();
    }

    public void stop() {
        isError = true;
        // cancel reconnect task
        if (reconnectFuture != null) {
            try {
                reconnectFuture.cancel(true);
            } catch (Exception e) {
                logger.warn("Cancel reconnect task error", e);
            }
        }
        if (channel != null) {
            channel.shutdown();
            try {
                channel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                //ignore ex
            }
        }
    }

    private void connect() throws Exception {
        logger.info("Connecting to channel server [{}:{}] ..", host, port);
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
        //TODO support ssl & plain text
        channelBuilder.usePlaintext(true);
        channel = channelBuilder.build();

        //register
        AgentInfo agentInfo = agentInfoService.getAgentInfo();

//        ArthasServiceGrpc.ArthasServiceFutureStub arthasServiceFutureStub = ArthasServiceGrpc.newFutureStub(channel);
//        RegisterResult registerResult;
//        try {
//            registerResult = arthasServiceFutureStub.register(agentInfo).get(10, TimeUnit.SECONDS);
//        } catch (Exception e) {
//            logger.error("Agent registration error: " + e.toString(), e);
//            throw e;
//        }

        arthasServiceStub = ArthasServiceGrpc.newStub(channel);
        final Promise<RegisterResult> promise = GlobalEventExecutor.INSTANCE.newPromise();
        arthasServiceStub.register(agentInfo, new StreamObserver<RegisterResult>() {
            @Override
            public void onNext(RegisterResult value) {
                logger.info("register result: " + value);
                promise.setSuccess(value);
            }

            @Override
            public void onError(Throwable t) {
                promise.setFailure(t);
            }

            @Override
            public void onCompleted() {
            }
        });

        RegisterResult registerResult = null;
        try {
            registerResult = promise.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Agent registration error: " + e.toString(), e);
            throw e;
        }

        if (registerResult.getStatus() != 0) {
            logger.error("Agent registration failed, status: {}, message: {}", registerResult.getStatus(), registerResult.getMessage());
            throw new Exception("Agent registration failed");
        } else {
            logger.info("Agent registered successfully.");
        }

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

        isError = false;
        agentInfoService.updateAgentStatus(AgentStatus.IN_SERVICE);

        sendHeartbeat(arthasServiceStub);
        logger.info("Channel client is ready.");
    }

    private void sendHeartbeat(final ArthasServiceGrpc.ArthasServiceStub arthasServiceStub) {
        AgentInfo agentInfo = agentInfoService.getAgentInfo();
        HeartbeatRequest heartbeatRequest = HeartbeatRequest.newBuilder()
                .setAgentId(agentInfo.getAgentId())
                .setAgentStatus(agentInfo.getAgentStatus())
                .setAgentVersion(agentInfo.getAgentVersion())
                .build();
        logger.info("sending heartbeat: " + heartbeatRequest);

        arthasServiceStub.heartbeat(heartbeatRequest, new StreamObserver<HeartbeatResponse>() {
            @Override
            public void onNext(HeartbeatResponse value) {
                logger.info("heartbeat result: " + value);

                //schedule next heartbeat
                executorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        sendHeartbeat(arthasServiceStub);
                    }
                }, 10, TimeUnit.SECONDS);
            }

            @Override
            public void onError(Throwable t) {
                onClientError("send heartbeat error", t);
            }

            @Override
            public void onCompleted() {
            }
        });

    }

    public void submitResponse(ActionResponse response) {
        //TODO 添加response缓存队列，重连成功发送
        try {
            checkConnection();
            responseStreamObserver.onNext(response);
        } catch (Exception e) {
            logger.error("submit response failure", e);
            onClientError("submit response failure", e);
            //throw e;
        }
    }

    private void scheduleReconnectTask() {
        reconnectFuture = executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isError) {
                        //stop previous channel
                        if (channel != null) {
                            try {
                                channel.shutdown();
                                channel.awaitTermination(1, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                //ignore ex
                            }
                            channel = null;
                        }

                        connect();
                    }
                } catch (Throwable e) {
                    logger.error("reconnect failure", e);
                }
            }
        }, 10, 1, TimeUnit.SECONDS);

    }

    private void onClientError(String message, Throwable ex) {
        isError = true;
        agentInfoService.updateAgentStatus(AgentStatus.OUT_OF_SERVICE);

        if (ex == null) {
            logger.error("Channel client is error: " + message);
        } else {
            logger.error("Channel client is error: " + message, ex);
        }
    }

    private void checkConnection() throws Exception {
        if (responseStreamObserver == null || isError) {
            throw new Exception("Channel is not ready");
        }
    }

    public void setAgentInfoService(AgentInfoService agentInfoService) {
        this.agentInfoService = agentInfoService;
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

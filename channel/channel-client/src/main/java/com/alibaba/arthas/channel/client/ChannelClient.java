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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
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
    private String channelServerAddress;
    private int reconnectDelay = 5;
    private int heartbeatInterval = 5;
    private EventLoopGroup group;

    //channel info
    private String channelVersion = "1.0.0";
    private List<String> channelFeatures = Arrays.asList("WebConsole", "ExecuteCommand");
    private long lastHeartbeatTime;
    private int workThreads = 2;

    public ChannelClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.channelServerAddress = host + ":" + port;
        init();
    }

    public ChannelClient(String channelServer) {
        setServerAddress(channelServer);
        init();
    }

    private void init() {
        group = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-ChannelWebsocketClient", true));
        executorService = Executors.newScheduledThreadPool(workThreads, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "arthas-channel-client");
                t.setDaemon(true);
                return t;
            }
        });
    }

    private void setServerAddress(String channelServer) {
        String[] strs = channelServer.split(":");
        if (strs.length != 2) {
            throw new IllegalArgumentException("server address format must be 'host:port' or 'ip:port'.");
        }

        this.host = strs[0].trim();
        if (this.host.length() == 0) {
            throw new IllegalArgumentException("server host is invalid");
        }

        try {
            this.port = Integer.parseInt(strs[1].trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("server port is invalid");
        }
        this.channelServerAddress = host + ":" + port;
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
            channel.shutdownNow();
            try {
                channel.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                //ignore ex
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        if (group != null) {
            group.shutdownGracefully();
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
                if (!promise.isDone()) {
                    promise.setFailure(t);
                }
            }

            @Override
            public void onCompleted() {
            }
        });

        RegisterResult registerResult = null;
        try {
            registerResult = promise.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (isWellKnownError(e)) {
                logger.error("Agent registration error: " + e.toString());
            } else {
                logger.error("Agent registration error: " + e.toString(), e);
            }
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
        logger.debug("sending heartbeat: {}", heartbeatRequest);

        arthasServiceStub.heartbeat(heartbeatRequest, new StreamObserver<HeartbeatResponse>() {
            @Override
            public void onNext(HeartbeatResponse value) {
                logger.debug("heartbeat result: {}", value);
            }

            @Override
            public void onError(Throwable t) {
                onClientError("send heartbeat error", t);
            }

            @Override
            public void onCompleted() {
            }
        });
        lastHeartbeatTime = System.currentTimeMillis();
    }

    //多线程发送数据貌似会出现错误，加上synchronized后没有出现错误
    public synchronized void submitResponse(ActionResponse response) throws Exception {
        //TODO 添加response缓存队列，重连成功发送
        try {
            checkConnection();
            responseStreamObserver.onNext(response);
        } catch (Exception e) {
            logger.error("submit response failure", e);
            onClientError("submit response failure", e);
            throw e;
        }
    }

    private void scheduleReconnectTask() {
        logger.info("Agent reconnect delay seconds: "+reconnectDelay+", heartbeat interval seconds: "+heartbeatInterval);
        reconnectFuture = executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (isError) {
                    // reconnect
                    try {
                        //stop previous channel
                        if (channel != null) {
                            try {
                                channel.shutdownNow();
                                channel.awaitTermination(1, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                //ignore ex
                            }
                            channel = null;
                        }

                        connect();
                    } catch (Throwable e) {
                        if (isWellKnownError(e)) {
                            logger.error("Agent reconnect failure: " + e.toString());
                        } else {
                            logger.error("Agent reconnect failure: " + e.toString(), e);
                        }
                    }
                } else {
                    // send heartbeat
                    long delta = System.currentTimeMillis() - lastHeartbeatTime;
                    if (delta >= heartbeatInterval * 1000) {
                        try {
                            sendHeartbeat(arthasServiceStub);
                        } catch (Throwable e) {
                            logger.error("send heartbeat failure", e);
                        }
                    }
                }
            }
        }, reconnectDelay, reconnectDelay, TimeUnit.SECONDS);

    }

    private void onClientError(String message, Throwable ex) {
        isError = true;
        agentInfoService.updateAgentStatus(AgentStatus.OUT_OF_SERVICE);

        if (ex == null) {
            logger.error("Channel client is error: " + message);
        } else {
            if (isWellKnownError(ex)) {
                logger.error("Channel client is error: " + message + ", error:" + ex.toString());
            } else {
                logger.error("Channel client is error: " + message, ex);
            }
        }
    }

    protected boolean isWellKnownError(Throwable ex) {
        String error = ex.toString();
        if (error.contains("UNAVAILABLE: Channel shutdownNow invoked")) {
            return true;
        } else if (error.contains("UNAVAILABLE: io exception")) {
            return true;
        }
        return false;
    }

    private void checkConnection() throws Exception {
        if (responseStreamObserver == null || isError) {
            throw new Exception("Channel is not ready");
        }
    }

    public String getChannelVersion() {
        return channelVersion;
    }

    public List<String> getChannelFeatures() {
        return channelFeatures;
    }

    public void setAgentInfoService(AgentInfoService agentInfoService) {
        this.agentInfoService = agentInfoService;
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public String getChannelServerAddress() {
        return channelServerAddress;
    }

    public int getReconnectDelay() {
        return reconnectDelay;
    }

    /**
     * Set reconnect delay seconds
     * @param reconnectDelay
     */
    public void setReconnectDelay(int reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * Set heartbeat interval seconds
     * @param heartbeatInterval
     */
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public EventLoopGroup getGroup() {
        return group;
    }

    public interface RequestListener {
        void onRequest(ActionRequest request);
    }
}

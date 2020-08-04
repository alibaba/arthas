package com.alibaba.arthas.channel.server;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.proto.ArthasServiceGrpc;
import com.alibaba.arthas.channel.proto.ExecuteParams;
import com.alibaba.arthas.channel.proto.ExecuteResult;
import com.alibaba.arthas.channel.proto.GeneralResult;
import com.alibaba.arthas.channel.proto.ResponseStatus;
import com.alibaba.arthas.channel.proto.SessionResult;
import com.alibaba.arthas.channel.proto.StatusResult;
import com.alibaba.arthas.channel.proto.SystemEnvResult;
import com.alibaba.arthas.channel.proto.VersionResult;
import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author gongdewei 2020/8/3
 */
public class ArthasServiceAcquireRequestTest {
    public static void main(String[] args) {

        ArthasServiceClient arthasServiceClient = new ArthasServiceClient();
        arthasServiceClient.start();

        startClientReconnectThread(arthasServiceClient);

        try {
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void startClientReconnectThread(final ArthasServiceClient arthasServiceClient) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (arthasServiceClient.isError()) {
                            arthasServiceClient.start();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    static class ArthasServiceClient {

        private volatile StreamObserver<ActionResponse> actionResponseStreamObserver;
        private int jobId;
        private volatile boolean isError;

        public void start() {
            if (this.actionResponseStreamObserver != null) {
                System.out.println("client is started.");
                return;
            }
            System.out.println("start client ..");
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 7700).usePlaintext(true).build();
            ArthasServiceGrpc.ArthasServiceStub arthasServiceStub = ArthasServiceGrpc.newStub(channel);

            final AtomicReference<StreamObserver<ActionResponse>> actionResponseStreamObserverHolder = new AtomicReference<StreamObserver<ActionResponse>>();
            this.actionResponseStreamObserver = arthasServiceStub.submitResponse(new StreamObserver<GeneralResult>() {

                @Override
                public void onNext(GeneralResult value) {
                    System.out.println("submitResponse result: " + value);
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("submitResponse error");
                    handleActionResponseStreamObserverError(t, actionResponseStreamObserverHolder.get());
                }

                @Override
                public void onCompleted() {
                    System.out.println("submitResponse onCompleted");
                }
            });
            actionResponseStreamObserverHolder.set(this.actionResponseStreamObserver);

            arthasServiceStub.acquireRequest(AgentInfo.newBuilder()
                    .setAgentId("agent-001")
                    .setAgentStatus(AgentStatus.UP)
                    .setAgentVersion("1.0.0").build(), new StreamObserver<ActionRequest>() {

                @Override
                public void onNext(ActionRequest request) {
                    handleRequest(request);
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("acquireRequest onError");
                    handleActionResponseStreamObserverError(t, actionResponseStreamObserverHolder.get());
                }

                @Override
                public void onCompleted() {
                    System.out.println("onCompleted");
                }
            });

            isError = false;
        }

        private void handleActionResponseStreamObserverError(Throwable t, StreamObserver<ActionResponse> tmpActionResponseStreamObserver) {
            if (this.actionResponseStreamObserver == tmpActionResponseStreamObserver) {
                this.actionResponseStreamObserver = null;
                System.out.println("Channel is broken: " + t.toString());
                this.isError = true;
            }
        }

        private void handleRequest(ActionRequest request) {
            System.out.println("action request: " + request);

            if (request.hasExecuteParams()) {
                ExecuteParams executeParams = request.getExecuteParams();
                String commandLine = executeParams.getCommandLine();

                //mock process command
                List<Any> results = new ArrayList<Any>();
                if (commandLine.startsWith("sysenv")) {
                    Map<String, String> env = System.getenv();
                    results.add(Any.pack(SystemEnvResult.newBuilder()
                            .setType("sysenv")
                            .putAllEnv(env).build()));

                    results.add(Any.pack(StatusResult.newBuilder()
                            .setStatusCode(0)
                            .build()));
                } else if (commandLine.equals("version")) {
                    results.add(Any.pack(VersionResult.newBuilder()
                            .setType("version")
                            .setVersion("1.0.0")
                            .build()));

                    results.add(Any.pack(StatusResult.newBuilder()
                            .setStatusCode(0)
                            .build()));
                } else {
                    results.add(Any.pack(SessionResult.newBuilder()
                            .setJavaPid(1923)
                            .setSessionId("sessionId-222")
                            .build()));
                    results.add(Any.pack(StatusResult.newBuilder()
                            .setStatusCode(-1)
                            .setMessage("Not supported command")
                            .build()));
                }

                actionResponseStreamObserver.onNext(ActionResponse.newBuilder()
                        .setAgentId(request.getAgentId())
                        .setOriginId(request.getOriginId())
                        .setSessionId(request.getSessionId())
                        .setStatus(ResponseStatus.SUCCEEDED)
                        .setExecuteResult(ExecuteResult.newBuilder()
                                .setJobId(++jobId)
                                .setJobStatus("TERMINATED")
                                .addAllResults(results)
                                .build()).build());
            }
        }

        public boolean isError() {
            return isError;
        }
    }

}

package com.alibaba.arthas.channel.server;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.ArthasServiceGrpc;
import com.alibaba.arthas.channel.proto.ExecuteParams;
import com.alibaba.arthas.channel.proto.ExecuteResult;
import com.alibaba.arthas.channel.proto.GeneralResult;
import com.alibaba.arthas.channel.proto.HeartbeatRequest;
import com.alibaba.arthas.channel.proto.HeartbeatResponse;
import com.alibaba.arthas.channel.proto.RegisterResult;
import com.alibaba.arthas.channel.proto.RequestAction;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author gongdewei 2020/8/3
 */
public class ArthasServiceServerTest {

    public static void main(String[] args) {
        TestArthasService arthasService = new TestArthasService();

        try {
            Server server = ServerBuilder.forPort(7700)
                    .addService(arthasService)
                    //enable server-reflect
                    .addService(ProtoReflectionService.newInstance())
                    .build();
            server.start();

            startRequestProducerThread(arthasService);

            server.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startRequestProducerThread(final TestArthasService arthasService) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    try {
                        String commandLine = i % 3 == 0 ? "sysenv" : (i % 3 == 1 ? "version" : "cat");
                        arthasService.putActionRequest(ActionRequest.newBuilder()
                                .setAction(RequestAction.EXECUTE)
                                .setAgentId("agent-1002")
                                .setOriginId("origin-" + i)
                                .setSessionId("xxx-xxx-xx")
                                .setExecuteParams(ExecuteParams.newBuilder()
                                        .setCommandLine(commandLine)
                                        .setExecTimeout(10000)
                                        .build())
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
        thread.start();
    }

    static class TestArthasService extends ArthasServiceGrpc.ArthasServiceImplBase {

        private StreamObserver<ActionRequest> actionRequestStreamObserver;

        private BlockingQueue<ActionRequest> requestQueue;
        private BlockingQueue<ActionResponse> responseQueue;

        private volatile boolean isError;
        private Thread requestSendingThread;

        public TestArthasService() {
            this.requestQueue = new LinkedBlockingQueue<ActionRequest>();
            this.responseQueue = new LinkedBlockingQueue<ActionResponse>();
        }

        @Override
        public void acquireRequest(AgentInfo request, StreamObserver<ActionRequest> responseObserver) {
            System.out.println("call: acquireRequest: " + request);
            this.actionRequestStreamObserver = responseObserver;

            startRequestSendingThread();
        }

        private synchronized void startRequestSendingThread() {
            if (requestSendingThread == null) {
                requestSendingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendingRequestLoop();
                    }
                });
                requestSendingThread.setDaemon(true);
                requestSendingThread.start();
            }
        }

        private void sendingRequestLoop() {
            while (true) {
                try {
                    synchronized (requestQueue) {
                        while (requestQueue.isEmpty())
                            requestQueue.wait(200); //wait for the queue to become empty

                        StreamObserver<ActionRequest> actionRequestStreamObserver = this.actionRequestStreamObserver;
                        if (actionRequestStreamObserver != null) {
                            ActionRequest request = requestQueue.peek();
                            if (request != null) {
                                try {
                                    actionRequestStreamObserver.onNext(request);
                                    System.out.println("send request: " + request);
                                    requestQueue.poll();

                                    Thread.sleep(50);
                                } catch (Exception e) {
                                    System.out.println("send request failure: " + e.toString());
                                    e.printStackTrace();
                                    //TODO getLock
                                    if (this.actionRequestStreamObserver == actionRequestStreamObserver) {
                                        this.actionRequestStreamObserver = null;
                                    }
                                }
                            }
                        } else {
                            Thread.sleep(1000);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public StreamObserver<ActionResponse> submitResponse(final StreamObserver<GeneralResult> responseObserver) {
            System.out.println("call: submitResponse");
            return new StreamObserver<ActionResponse>() {
                @Override
                public void onNext(ActionResponse response) {
                    System.out.println("submitResponse: " + response);
                    if (response.hasExecuteResult()) {
                        ExecuteResult executeResult = response.getExecuteResult();
                        executeResult.getJobId();
                        executeResult.getJobStatus();
                        List<Any> resultsList = executeResult.getResultsList();
                        for (Any result : resultsList) {
                            String clazzName = result.getTypeUrl().split("/")[1];;
                            Message resultMessage = null;
                            //catch unknown message type error
                            try {
                                Class<Message> resultClass = (Class<Message>) Class.forName(clazzName);
                                resultMessage = result.unpack(resultClass);
                            } catch (Throwable e) {
                                System.out.println("parse result failure, clazzName: " + clazzName +", error: " + e.toString());
                                e.printStackTrace();
                            }

                            if (resultMessage != null) {
                                handleResultMessage(resultMessage);
                            }
                        }
                    }
                    try {
                        responseQueue.put(response);

                        responseObserver.onNext(GeneralResult.newBuilder()
                                .setStatus(0)
                                .build());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    isError = true;
                    System.out.println("onError");
                    t.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    System.out.println("onCompleted");
                }
            };
        }

        @Override
        public void register(AgentInfo agentInfo, StreamObserver<RegisterResult> responseObserver) {
            System.out.println("Register agent info: " + agentInfo);
            responseObserver.onNext(RegisterResult.newBuilder()
                    .setStatus(0)
                    .setMessage("Register agent successfully")
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void heartbeat(HeartbeatRequest heartbeatRequest, StreamObserver<HeartbeatResponse> responseObserver) {
            System.out.println("heartbeat: " + heartbeatRequest);
            responseObserver.onNext(HeartbeatResponse.newBuilder()
                    .setStatus(0)
                    .build());
            responseObserver.onCompleted();
        }

        public StreamObserver<ActionRequest> getActionRequestStreamObserver() {
            return actionRequestStreamObserver;
        }

        public BlockingQueue<ActionRequest> getRequestQueue() {
            return requestQueue;
        }

        public BlockingQueue<ActionResponse> getResponseQueue() {
            return responseQueue;
        }

        public void putActionRequest(ActionRequest actionRequest) throws InterruptedException {
            requestQueue.put(actionRequest);
            synchronized (requestQueue) {
                requestQueue.notify();
            }
        }
    }

    private static void handleResultMessage(Message resultMessage) {

        System.out.println("result message: "+resultMessage.getClass().getName());

    }
}

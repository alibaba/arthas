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
import com.alibaba.arthas.channel.proto.ResultFormat;
import com.alibaba.arthas.channel.proto.SessionResult;
import com.alibaba.arthas.channel.proto.StatusResult;
import com.alibaba.arthas.channel.proto.SystemEnvResult;
import com.alibaba.arthas.channel.proto.UnknownResult;
import com.alibaba.arthas.channel.proto.VersionResult;
import com.alibaba.fastjson.JSON;
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.SessionModel;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.command.model.SystemEnvModel;
import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.command.model.VersionModel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author gongdewei 2020/8/3
 */
public class ArthasServiceClientTest {
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
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 7700).usePlaintext().build();
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
                t.printStackTrace();
                this.isError = true;
            }
            try {
                tmpActionResponseStreamObserver.onCompleted();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        private void handleRequest(ActionRequest request) {
            System.out.println("action request: " + request);

            if (request.hasExecuteParams()) {
                ExecuteParams executeParams = request.getExecuteParams();
                String commandLine = executeParams.getCommandLine();

                //mock process command
                List<ResultModel> resultModels = processCommand(commandLine);

                //convert results
                boolean isProtoFormat = executeParams.getResultFormat().equals(ResultFormat.PROTO);
                ExecuteResult.Builder resultBuilder = ExecuteResult.newBuilder();
                if (isProtoFormat) {
                    try {
                        List<Any> results = convertToProtoResults(resultModels);
                        resultBuilder = resultBuilder.addAllResults(results);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    resultBuilder = resultBuilder.setResultsJson(StringValue.newBuilder()
                                    .setValue(JSON.toJSONString(resultModels))
                                    .build());
                }

                actionResponseStreamObserver.onNext(ActionResponse.newBuilder()
                        .setAgentId(request.getAgentId())
                        .setRequestId(request.getRequestId())
                        .setSessionId(request.getSessionId())
                        .setStatus(ResponseStatus.SUCCEEDED)
                        .setExecuteResult(resultBuilder.build()).build());
            }
        }

        private List<Any> convertToProtoResults(List<ResultModel> resultModels) {
            List<Any> results = new ArrayList<Any>(resultModels.size());
            for (int i = 0; i < resultModels.size(); i++) {
                ResultModel resultModel = resultModels.get(i);
                if (resultModel instanceof SystemEnvModel) {
                    SystemEnvModel systemEnvModel = (SystemEnvModel) resultModel;
                    results.add(Any.pack(SystemEnvResult.newBuilder()
                            .setType(systemEnvModel.getType())
                            .putAllEnv(systemEnvModel.getEnv()).build()));
                } else if (resultModel instanceof SessionModel) {
                    SessionModel sessionModel = (SessionModel) resultModel;
                    results.add(Any.pack(SessionResult.newBuilder()
                            .setType(sessionModel.getType())
                            .setJavaPid(sessionModel.getJavaPid())
                            .setSessionId(sessionModel.getSessionId())
                            .build()));
                } else if (resultModel instanceof VersionModel) {
                    VersionModel versionModel = (VersionModel) resultModel;
                    results.add(Any.pack(VersionResult.newBuilder()
                            .setType(versionModel.getType())
                            .setVersion(versionModel.getVersion())
                            .build()));

                } else if (resultModel instanceof StatusModel) {
                    StatusModel statusModel = (StatusModel) resultModel;
                    results.add(Any.pack(StatusResult.newBuilder()
                            .setType(statusModel.getType())
                            .setStatusCode(statusModel.getStatusCode())
                            .setMessage(statusModel.getMessage()!=null?statusModel.getMessage():"")
                            .build()));
                } else {
                    // not supported proto format
                    results.add(Any.pack(UnknownResult.newBuilder()
                            .setType(resultModel.getType())
                            .setMessage("unsupported proto format")
                            .build()));
                }
            }
            return results;
        }

        private List<ResultModel> processCommand(String commandLine) {
            List<ResultModel> commandResults = new ArrayList<ResultModel>();
            if (commandLine.startsWith("sysenv")) {
                Map<String, String> env = System.getenv();
                SystemEnvModel systemEnvModel = new SystemEnvModel(env);
                commandResults.add(systemEnvModel);
                commandResults.add(new StatusModel(0));
            } else if (commandLine.equals("version")) {
                VersionModel versionModel = new VersionModel();
                versionModel.setVersion("1.0.0");
                commandResults.add(versionModel);
                commandResults.add(new StatusModel(0));
            } else if (commandLine.equals("session")) {
                SessionModel sessionModel = new SessionModel();
                sessionModel.setJavaPid(1234);
                sessionModel.setSessionId(UUID.randomUUID().toString());
                commandResults.add(sessionModel);
                commandResults.add(new StatusModel(0));
            } else if (commandLine.equals("sysprop")){
                SystemPropertyModel systemPropertyModel = new SystemPropertyModel();
                systemPropertyModel.putAll(System.getProperties());
                commandResults.add(systemPropertyModel);
                commandResults.add(new StatusModel(0));
            } else {
                commandResults.add(new StatusModel(-1, "Not supported command"));
            }
            return commandResults;
        }

        public boolean isError() {
            return isError;
        }
    }

}

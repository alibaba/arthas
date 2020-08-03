package com.alibaba.arthas.channel.server;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.proto.ArthasServiceGrpc;
import com.alibaba.arthas.channel.proto.ExecuteResult;
import com.alibaba.arthas.channel.proto.GeneralResult;
import com.alibaba.arthas.channel.proto.ResponseStatus;
import com.alibaba.arthas.channel.proto.SystemEnvResult;
import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Map;

/**
 * @author gongdewei 2020/8/3
 */
public class ArthasServiceAcquireRequestTest {
    public static void main(String[] args) {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext(true).build();
        ArthasServiceGrpc.ArthasServiceStub arthasServiceStub = ArthasServiceGrpc.newStub(channel);

        final StreamObserver<ActionResponse> actionResponseStreamObserver = arthasServiceStub.submitResponse(new StreamObserver<GeneralResult>() {
            @Override
            public void onNext(GeneralResult value) {
                System.out.println("submitResponse result: "+ value);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("submitResponse error");
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("submitResponse onCompleted");
            }
        });

        arthasServiceStub.acquireRequest(AgentInfo.newBuilder()
                .setAgentId("agent-001")
                .setAgentStatus(AgentStatus.UP)
                .setAgentVersion("1.0.0").build(), new StreamObserver<ActionRequest>() {
            private int jobId;

            @Override
            public void onNext(ActionRequest request) {
                System.out.println("action request: " + request);

                Map<String, String> env = System.getenv();
                actionResponseStreamObserver.onNext(ActionResponse.newBuilder()
                        .setAgentId(request.getAgentId())
                        .setOriginId(request.getOriginId())
                        .setSessionId(request.getSessionId())
                        .setStatus(ResponseStatus.SUCCEEDED)
                        .setExecuteResult(ExecuteResult.newBuilder()
                                .setJobId(++jobId)
                                .setJobStatus("TERMINATED")
                                .addResults(Any.pack(SystemEnvResult.newBuilder()
                                        .setType("sysenv")
                                        .putAllEnv(env).build()))
                                .build()).build());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError");
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
            }
        });

        try {
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}

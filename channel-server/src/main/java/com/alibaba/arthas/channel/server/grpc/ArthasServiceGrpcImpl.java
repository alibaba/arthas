package com.alibaba.arthas.channel.server.grpc;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.ArthasServiceGrpc;
import com.alibaba.arthas.channel.proto.GeneralResult;
import com.alibaba.arthas.channel.proto.HeartbeatRequest;
import com.alibaba.arthas.channel.proto.HeartbeatResponse;
import com.alibaba.arthas.channel.proto.RegisterResult;
import com.alibaba.arthas.channel.server.message.topic.ActionRequestTopic;
import com.alibaba.arthas.channel.server.message.topic.ActionResponseTopic;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentBizSerivce;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author gongdewei 2020/8/10
 */
public class ArthasServiceGrpcImpl extends ArthasServiceGrpc.ArthasServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ArthasServiceGrpcImpl.class);

    @Autowired
    private AgentManageService agentManageService;

    @Autowired
    private AgentBizSerivce agentBizSerivce ;

    @Autowired
    private MessageExchangeService messageExchangeService;


    public ArthasServiceGrpcImpl() {
    }

    /**
     * Send action request to arthas agent
     * @param request
     * @param responseObserver
     */
    @Override
    public void acquireRequest(AgentInfo request, final StreamObserver<ActionRequest> responseObserver) {

        final String agentId = request.getAgentId();
        AgentVO agentVO = agentManageService.findAgentById(agentId);
        if (agentVO == null) {
            logger.warn("Agent not found: "+agentId);
            responseObserver.onError(new RuntimeException("Agent not found: "+agentId));
            return;
        }

        final ActionRequestTopic requestTopic = new ActionRequestTopic(agentId);
        try {
            messageExchangeService.subscribe(requestTopic, 30*60*1000, new MessageExchangeService.MessageHandler() {
                @Override
                public boolean onMessage(byte[] messageBytes) {
                    //convert to pb message, then send it to arthas agent
                    ActionRequest actionRequest = null;
                    try {
                        actionRequest = ActionRequest.parseFrom(messageBytes);
                    } catch (Throwable e) {
                        logger.error("parse action request message failure", e);
                        return true;
                    }

                    try {
                        responseObserver.onNext(actionRequest);
                        return true;
                    } catch (Throwable e) {
                        logger.error("send action request message to arthas agent failure", e);
                        //TODO 如何通知请求来源方发送请求失败？

                        // 可能是网络异常，不能再使用本连接的StreamObserver发送请求
                        try {
                            messageExchangeService.unsubscribe(requestTopic, this);
                        } catch (Throwable ex) {
                            logger.error("unsubscribe action request topic failure: "+requestTopic, e);
                        }

                        //TODO 通知网络异常
                        return false;
                    }
                }

                @Override
                public void onTimeout() {

                }
            });
        } catch (Throwable e) {
            logger.error("subscribe action request topic failure: "+requestTopic, e);
        }
    }

    /**
     * Receive action response from arthas agent
     * @param responseObserver
     * @return
     */
    @Override
    public StreamObserver<ActionResponse> submitResponse(final StreamObserver<GeneralResult> responseObserver) {

        return new StreamObserver<ActionResponse>() {
            @Override
            public void onNext(ActionResponse actionResponse) {
                String agentId = actionResponse.getAgentId();
                byte[] messageBytes = actionResponse.toByteArray();
                GeneralResult.Builder resultBuilder = GeneralResult.newBuilder();
                ActionResponseTopic responseTopic = new ActionResponseTopic(agentId, actionResponse.getRequestId());

                try {
                    messageExchangeService.pushMessage(responseTopic, messageBytes);
                    resultBuilder.setStatus(0);
                } catch (Throwable e) {
                    resultBuilder
                            .setStatus(1000)
                            .setMessage("push message failure");
                    logger.error("push action response message failure: "+responseTopic, e);
                }

                try {
                    responseObserver.onNext(resultBuilder.build());
                } catch (Throwable e) {
                    logger.error("send response result failure", e);
                    responseObserver.onError(e);
                    //TODO 通知网络链路异常
                }
            }

            @Override
            public void onError(Throwable t) {
                //TODO 通知网络链路异常
            }

            @Override
            public void onCompleted() {
                //TODO 通知网络链路异常
            }
        };
    }

    @Override
    public void register(AgentInfo request, StreamObserver<RegisterResult> responseObserver) {
        long now = System.currentTimeMillis();
        AgentVO agentVO = agentManageService.findAgentById(request.getAgentId());
        if (agentVO != null) {
            agentVO.setAgentStatus(request.getAgentStatus().name());
            agentVO.setAgentVersion(request.getAgentVersion());
            agentVO.setModifiedTime(now);
            agentVO.setHeartbeatTime(now);
            agentManageService.updateAgent(agentVO);
            responseObserver.onNext(RegisterResult.newBuilder()
                    .setStatus(0)
                    .setMessage("Agent info has been updated: "+request.getAgentId())
                    .build());
        } else {
            agentVO = new AgentVO();
            agentVO.setAgentId(request.getAgentId());
            agentVO.setAgentVersion(request.getAgentVersion());
            agentVO.setAgentStatus(request.getAgentStatus().name());
            agentVO.setHostname(request.getHostname());
            agentVO.setIp(request.getIp());
            agentVO.setOsVersion(request.getOsVersion());
            agentVO.setAppName(request.getAppName());
            agentVO.setCreatedTime(now);
            agentVO.setModifiedTime(now);
            agentVO.setHeartbeatTime(now);
            agentManageService.addAgent(agentVO);
            responseObserver.onNext(RegisterResult.newBuilder()
                    .setStatus(0)
                    .setMessage("Agent info has been added: "+request.getAgentId())
                    .build());
        }
        logger.info("register agent: "+agentVO.getAgentId());
    }

    @Override
    public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        AgentVO agentVO = agentManageService.findAgentById(request.getAgentId());
        if (agentVO == null) {
            responseObserver.onNext(HeartbeatResponse.newBuilder()
                    .setStatus(1001)
                    .setMessage("Agent not found: "+request.getAgentId())
                    .build());
            return;
        }

        agentBizSerivce.heartbeat(request.getAgentId(), request.getAgentStatus().name(), request.getAgentVersion());
        responseObserver.onNext(HeartbeatResponse.newBuilder()
                .setStatus(0)
                .build());
    }
}

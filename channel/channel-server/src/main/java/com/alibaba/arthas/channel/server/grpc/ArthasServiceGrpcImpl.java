package com.alibaba.arthas.channel.server.grpc;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;
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

import java.util.Optional;

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
        subscribeAgentRequests(agentId, responseObserver);
    }

    private void subscribeAgentRequests(String agentId, StreamObserver<ActionRequest> responseObserver) {
        Optional<AgentVO> optionalAgentVO = agentManageService.findAgentById(agentId).block();
        if (!optionalAgentVO.isPresent()) {
            logger.info("Agent not found: "+agentId);
            responseObserver.onError(new RuntimeException("Agent not found: "+agentId));
            return;
        }

        AgentVO agentVO = optionalAgentVO.get();
        if (AgentStatus.DOWN.name().equals(agentVO.getAgentStatus())) {
            logger.info("Agent status is not ready, stop processing agent requests. agentId: {}", agentId);
            responseObserver.onError(new RuntimeException("Agent status is not ready: "+agentId));
            return;
        }

        final ActionRequestTopic requestTopic = new ActionRequestTopic(agentId);
        try {
            messageExchangeService.subscribe(requestTopic, 30*1000, new MessageExchangeService.MessageHandler() {
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
                        //TODO 如何通知请求来源方发送请求失败？通知打开的WebConsole
                        agentBizSerivce.compareAndUpdateAgentStatus(agentId, AgentStatus.IN_SERVICE, AgentStatus.OUT_OF_SERVICE);
                        //responseObserver.onError(e);
                        //TODO 通知网络异常
                        return false;
                    }
                }

                @Override
                public boolean onTimeout() {
                    subscribeAgentRequests(agentId, responseObserver);
                    return false;
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
        Optional<AgentVO> optionalAgentVO = agentManageService.findAgentById(request.getAgentId()).block();
        AgentVO agentVO;
        if (optionalAgentVO.isPresent()) {
            agentVO = optionalAgentVO.get();
            copyAgentVO(request, agentVO);
            agentVO.setModifiedTime(now);
            agentVO.setHeartbeatTime(now);
            agentManageService.updateAgent(agentVO);
            responseObserver.onNext(RegisterResult.newBuilder()
                    .setStatus(0)
                    .setMessage("Agent info has been updated: "+request.getAgentId())
                    .build());
        } else {
            agentVO = new AgentVO();
            copyAgentVO(request, agentVO);
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

    private void copyAgentVO(AgentInfo agentInfo, AgentVO agentVO) {
        agentVO.setAgentId(agentInfo.getAgentId());
        agentVO.setAgentVersion(agentInfo.getAgentVersion());
        agentVO.setAgentStatus(agentInfo.getAgentStatus().name());
        agentVO.setHostname(agentInfo.getHostname());
        agentVO.setIp(agentInfo.getIp());
        agentVO.setOsVersion(agentInfo.getOsVersion());
        agentVO.setAppName(agentInfo.getAppName());
        agentVO.setChannelServer(agentInfo.getChannelServer());
        agentVO.setClassPath(agentInfo.getClassPath());
        agentVO.setChannelVersion(agentInfo.getChannelVersion());
        agentVO.setChannelFeatures(agentInfo.getChannelFeaturesList());
    }

    @Override
    public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        Optional<AgentVO> optionalAgentVO = agentManageService.findAgentById(request.getAgentId()).block();
        if (!optionalAgentVO.isPresent()) {
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

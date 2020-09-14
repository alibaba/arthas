package com.alibaba.arthas.channel.server.service.impl;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.ConsoleParams;
import com.alibaba.arthas.channel.proto.RequestAction;
import com.alibaba.arthas.channel.proto.ResponseStatus;
import com.alibaba.arthas.channel.server.message.MessageExchangeException;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.topic.ActionRequestTopic;
import com.alibaba.arthas.channel.server.message.topic.ActionResponseTopic;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
import com.google.protobuf.StringValue;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author gongdewei 2020/8/11
 */
public class ApiActionDelegateServiceImpl implements ApiActionDelegateService {

    private static final Logger logger = LoggerFactory.getLogger(ApiActionDelegateServiceImpl.class);

    @Autowired
    private AgentManageService agentManageService;

    @Autowired
    private MessageExchangeService messageExchangeService;


    @Override
    public Mono<ActionResponse> initSession(String agentId) throws Exception {
        return sendRequestAndSubscribe(agentId, ActionRequest.newBuilder()
                .setAction(RequestAction.INIT_SESSION));
    }

    @Override
    public Mono<ActionResponse> closeSession(String agentId, String sessionId) throws Exception {
        return sendRequestAndSubscribe(agentId, ActionRequest.newBuilder()
                .setAction(RequestAction.CLOSE_SESSION)
                .setSessionId(sessionId));
    }

    @Override
    public Mono<ActionResponse> interruptJob(String agentId, String sessionId) throws Exception {
        return sendRequestAndSubscribe(agentId, ActionRequest.newBuilder()
                .setAction(RequestAction.INTERRUPT_JOB)
                .setSessionId(sessionId));
    }

    /**
     * exec command and get results.
     *
     * NOTE: This method do not support streaming command results, all results are received after the command is executed.
     * @param agentId
     * @param request
     * @return
     */
    @Override
    public Mono<ActionResponse> execCommand(String agentId, ActionRequest request) throws Exception {
        return sendRequestAndSubscribe(agentId, request.toBuilder());
    }

    /**
     * Send request and process command results with responseListener.
     * Support streaming command results.
     * @param agentId
     * @param request
     * @return
     */
    @Override
    public Mono<ActionResponse> asyncExecCommand(final String agentId, ActionRequest request) throws Exception {
        //send request
        String requestId = generateRandomRequestId();
        sendRequest(agentId, requestId, request.toBuilder());

        // 获取JobId？
        return Mono.just(ActionResponse.newBuilder()
                .setAgentId(agentId)
                .setRequestId(requestId)
                .setStatus(ResponseStatus.CONTINUOUS)
                .build());
    }

    @Override
    public Mono<ActionResponse> openConsole(String agentId, int timeout) throws Exception {
        //send request
        String requestId = generateRandomRequestId();
        sendRequest(agentId, requestId, ActionRequest.newBuilder()
                .setAction(RequestAction.OPEN_CONSOLE));

        //subscribe response
        return subscribeResponse(agentId, requestId, timeout);
    }

    @Override
    public void consoleInput(String agentId, String consoleId, String inputData) throws Exception {
        //send request
        sendRequest(agentId, consoleId, ActionRequest.newBuilder()
                .setAction(RequestAction.CONSOLE_INPUT)
                .setConsoleParams(ConsoleParams.newBuilder()
                        .setConsoleId(consoleId)
                        .setInputData(inputData)));
    }

    @Override
    public void closeConsole(String agentId, String consoleId) throws Exception {
        //send request
        sendRequest(agentId, consoleId, ActionRequest.newBuilder()
                .setAction(RequestAction.CLOSE_CONSOLE)
                .setConsoleParams(ConsoleParams.newBuilder()
                        .setConsoleId(consoleId)));
    }

    @Override
    public Mono<ActionResponse> pullResults(final String agentId, String requestId, int timeout) {
        //subscribe response
        ActionResponseTopic topic = new ActionResponseTopic(agentId, requestId);
        Mono<ActionResponse> responseMono = messageExchangeService.pollMessage(topic, timeout)
                .flatMap((Function<byte[], Mono<ActionResponse>>) messageBytes -> {
                    try {
                        ActionResponse actionResponse = ActionResponse.parseFrom(messageBytes);
                        return Mono.just(actionResponse);
                    } catch (Throwable e) {
                        logger.error("process action response message failure", e);
                        ActionResponse actionResponse = ActionResponse.newBuilder()
                                .setAgentId(agentId)
                                .setRequestId(requestId)
                                .setStatus(ResponseStatus.FAILED)
                                .setMessage("process action response message failure")
                                .build();
                        return Mono.just(actionResponse);
                    }
                }).switchIfEmpty(Mono.just(ActionResponse.newBuilder()
                        .setAgentId(agentId)
                        .setRequestId(requestId)
                        .setStatus(ResponseStatus.FAILED)
                        .setMessage("Timeout")
                        .build()))
                .onErrorResume(throwable -> {
                    logger.error("pull results error", throwable);
                    return Mono.just(ActionResponse.newBuilder()
                            .setAgentId(agentId)
                            .setRequestId(requestId)
                            .setStatus(ResponseStatus.FAILED)
                            .setMessage(throwable.getMessage())
                            .build());
                });

        return responseMono;
    }

    @Override
    public void subscribeResults(final String agentId, final String requestId, int timeout, final ResponseListener responseListener) throws Exception {
        //subscribe response
        ActionResponseTopic topic = new ActionResponseTopic(agentId, requestId);

        messageExchangeService.subscribe(topic, timeout, new MessageExchangeService.MessageHandler() {
            @Override
            public boolean onMessage(byte[] messageBytes) {
                ActionResponse actionResponse;
                try {
                    actionResponse = ActionResponse.parseFrom(messageBytes);
                } catch (Throwable e) {
                    logger.error("process action response message failure", e);
                    actionResponse = ActionResponse.newBuilder()
                            .setAgentId(agentId)
                            .setRequestId(requestId)
                            .setStatus(ResponseStatus.FAILED)
                            .setMessage("process action response message failure")
                            .build();
                }
                boolean next = responseListener.onMessage(actionResponse)
                    && actionResponse.getStatus().equals(ResponseStatus.CONTINUOUS);
                return next;
            }

            @Override
            public boolean onTimeout() {
                // subscribe timeout
                return responseListener.onTimeout();
            }
        });
    }

    /**
     * Send one-time request and subscribe it's response.
     * NOTE: This method do not support streaming results.
     * @param agentId
     * @param requestBuilder
     * @return
     */
    private Mono<ActionResponse> sendRequestAndSubscribe(String agentId, ActionRequest.Builder requestBuilder) throws Exception {
        //send request
        String requestId = generateRandomRequestId();
        ActionRequest actionRequest = sendRequest(agentId, requestId, requestBuilder);

        int execTimeout = 30000;
        if (actionRequest.hasExecuteParams()) {
            int timeout = actionRequest.getExecuteParams().getExecTimeout();
            if (timeout > 0) {
                execTimeout = timeout;
            }
        }

        //subscribe response
        return subscribeResponse(agentId, requestId, execTimeout);
    }

    private Mono<ActionResponse> subscribeResponse(String agentId, String requestId, int timeout) throws MessageExchangeException {

        final ActionResponseTopic responseTopic = new ActionResponseTopic(agentId, requestId);
        return Mono.create(monoSink -> {
            try {
                messageExchangeService.subscribe(responseTopic, timeout, new MessageExchangeService.MessageHandler() {
                    @Override
                    public boolean onMessage(byte[] messageBytes) {
                        try {
                            ActionResponse actionResponse = ActionResponse.parseFrom(messageBytes);

                            monoSink.success(actionResponse);
                        } catch (Throwable e) {
                            logger.error("process response message failure: "+e.getMessage(), e);
                            monoSink.success(ActionResponse.newBuilder()
                                    .setStatus(ResponseStatus.FAILED)
                                    .setMessage("process response message failure: "+e.getMessage())
                                    .build());
                        }

                        //one-time subscribe, just remove it after received message
                        try {
                            messageExchangeService.removeTopic(responseTopic);
                        } catch (Throwable e) {
                            logger.error("remove topic failure", e);
                        }
                        return false;
                    }

                    @Override
                    public boolean onTimeout() {
                        monoSink.success(ActionResponse.newBuilder()
                                .setStatus(ResponseStatus.FAILED)
                                .setMessage("timeout")
                                .build());
                        return false;
                    }
                });
            } catch (MessageExchangeException e) {
                monoSink.error(e);
            }

        });
    }

    private ActionRequest sendRequest(String agentId, String requestId, ActionRequest.Builder actionRequestBuilder) throws MessageExchangeException {
        ActionRequest actionRequest = actionRequestBuilder
                .setAgentId(agentId)
                .setRequestId(requestId)
                .build();
        messageExchangeService.pushMessage(new ActionRequestTopic(agentId), actionRequest.toByteArray());
        return actionRequest;
    }

    private String generateRandomRequestId() {
        //return UUID.randomUUID().toString().replaceAll("-", "");
        return RandomStringUtils.random(12, true, true);
    }

}

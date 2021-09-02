package com.alibaba.arthas.channel.server.service;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import reactor.core.publisher.Mono;

/**
 * @author gongdewei 2020/8/10
 */
public interface ApiActionDelegateService {

    Mono<ActionResponse> initSession(String agentId) throws Exception;

    Mono<ActionResponse> closeSession(String agentId, String sessionId) throws Exception;

    Mono<ActionResponse> interruptJob(String agentId, String sessionId) throws Exception;

    Mono<ActionResponse> execCommand(String agentId, ActionRequest request) throws Exception;

    Mono<ActionResponse> asyncExecCommand(String agentId, ActionRequest request) throws Exception;


    /**
     * Open WebConsole and create new session
     * @return
     */
    Mono<ActionResponse> openConsole(String agentId, int timeout) throws Exception;

    /**
     * proxy pass WebConsole input
     */
    void consoleInput(String agentId, String consoleId, String inputData) throws Exception;

    /**
     * Close WebConsole
     */
    void closeConsole(String agentId, String consoleId) throws Exception;

    Mono<ActionResponse> pullResults(String agentId, String requestId, int timeout);

    void subscribeResults(String agentId, String requestId, int timeout, ResponseListener responseListener) throws Exception;


    interface ResponseListener {
        /**
         * process message
         * @param response
         * @return true - processing next message, false - stop processing
         */
        boolean onMessage(ActionResponse response);

        /**
         * subscribe message timeout
         * @return true - continue subscribing, false - stop subscribing
         */
        boolean onTimeout();
    }
}

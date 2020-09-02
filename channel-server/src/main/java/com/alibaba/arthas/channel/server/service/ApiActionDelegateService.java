package com.alibaba.arthas.channel.server.service;

import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.server.api.ApiRequest;
import com.alibaba.arthas.channel.server.api.ApiResponse;
import io.netty.util.concurrent.Promise;

/**
 * @author gongdewei 2020/8/10
 */
public interface ApiActionDelegateService {

    Promise<ApiResponse> initSession(String agentId) throws Exception;

//    Promise<ApiResponse> joinSession(String agentId, String sessionId) throws Exception;

    Promise<ApiResponse> closeSession(String agentId, String sessionId) throws Exception;

    Promise<ApiResponse> interruptJob(String agentId, String sessionId) throws Exception;

    Promise<ApiResponse> execCommand(String agentId, ApiRequest request) throws Exception;

    ApiResponse asyncExecCommand(String agentId, ApiRequest request) throws Exception;


    /**
     * Open WebConsole and create new session
     * @return
     */
    Promise<ActionResponse> openConsole(String agentId, int timeout) throws Exception;

    /**
     * proxy pass WebConsole input
     */
    void consoleInput(String agentId, String consoleId, String inputData) throws Exception;

    /**
     * Close WebConsole
     */
    void closeConsole(String agentId, String consoleId) throws Exception;

    ApiResponse pullResults(String agentId, String requestId, int timeout) throws Exception;

    void subscribeResults(String agentId, String requestId, int timeout, ResponseListener responseListener) throws Exception;


    interface ResponseListener {
        boolean onMessage(ActionResponse response);
    }
}

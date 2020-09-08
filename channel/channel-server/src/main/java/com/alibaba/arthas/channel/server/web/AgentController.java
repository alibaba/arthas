package com.alibaba.arthas.channel.server.web;

import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.ExecuteResult;
import com.alibaba.arthas.channel.proto.ResponseStatus;
import com.alibaba.arthas.channel.server.api.ApiException;
import com.alibaba.arthas.channel.server.conf.ScheduledExecutorConfig;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.api.ApiRequest;
import com.alibaba.arthas.channel.server.api.ApiResponse;
import com.alibaba.arthas.channel.server.api.ApiState;
import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.fastjson.JSON;
import io.netty.util.concurrent.Promise;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author gongdewei 2020/8/10
 */
@RestController
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentManageService agentManageService;

    @Autowired
    private ApiActionDelegateService apiActionDelegateService;

    @Autowired
    private ScheduledExecutorConfig executorServiceConfig;


    @RequestMapping("/agents")
    public Mono<List<AgentVO>> listAgents() {
        return agentManageService.listAgents();
    }

    @RequestMapping("/agent/{agentId}")
    public AgentVO getAgent(@PathVariable String agentId) {
        AgentVO agentVO = checkAgentExists(agentId);
        return agentVO;
    }

    @RequestMapping("/agent/{agentId}/init_session")
    public ApiResponse initSession(@PathVariable String agentId) {
        checkAgentExists(agentId);
        try {
            Promise<ApiResponse> responsePromise = apiActionDelegateService.initSession(agentId);
            ApiResponse apiResponse = responsePromise.get(30, TimeUnit.SECONDS);
            return apiResponse;
        } catch (Throwable e) {
            logger.error("create session failure: " + e.toString(), e);
            return new ApiResponse()
                    .setAgentId(agentId)
                    .setState(ApiState.FAILED)
                    .setMessage("create session failure: " + e.toString());
        }
    }

    @RequestMapping("/agent/{agentId}/close_session/{sessionId}")
    public ApiResponse closeSession(@PathVariable String agentId, @PathVariable String sessionId) {
        checkAgentExists(agentId);
        try {
            Promise<ApiResponse> responsePromise = apiActionDelegateService.closeSession(agentId, sessionId);
            return responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Throwable e) {
            logger.error("close session failure: " + e.toString(), e);
            return new ApiResponse()
                    .setAgentId(agentId)
                    .setSessionId(sessionId)
                    .setState(ApiState.FAILED)
                    .setMessage("close session failure: " + e.toString());
        }
    }

    @RequestMapping("/agent/{agentId}/interrupt_job/{sessionId}")
    public ApiResponse interruptJob(@PathVariable String agentId, @PathVariable String sessionId) {
        checkAgentExists(agentId);
        try {
            Promise<ApiResponse> responsePromise = apiActionDelegateService.interruptJob(agentId, sessionId);
            return responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Throwable e) {
            logger.error("interrupt job failure: " + e.toString(), e);
            return new ApiResponse()
                    .setAgentId(agentId)
                    .setSessionId(sessionId)
                    .setState(ApiState.FAILED)
                    .setMessage("interrupt job failure: " + e.toString());
        }
    }

    @PostMapping("/agent/{agentId}/exec")
    public ApiResponse execCommand(@PathVariable String agentId, @RequestBody String requestBody) {
        try {
            ApiRequest apiRequest = parseRequest(requestBody);
            long execTimeout = 30000;
            if (apiRequest.getExecTimeout() != null && apiRequest.getExecTimeout() > 0) {
                execTimeout = apiRequest.getExecTimeout();
            }

            Promise<ApiResponse> responsePromise = apiActionDelegateService.execCommand(agentId, apiRequest);
            return responsePromise.get(execTimeout, TimeUnit.MILLISECONDS);

        } catch (Throwable e) {
            logger.error("exec command failure: " + e.toString(), e);
            return new ApiResponse()
                    .setState(ApiState.FAILED)
                    .setMessage("exec command failure: " + e.toString());
        }
    }

    @PostMapping("/agent/{agentId}/async_exec")
    public ApiResponse asyncExecCommand(@PathVariable String agentId, @RequestBody String requestBody) {
        try {
            ApiRequest apiRequest = parseRequest(requestBody);
            return apiActionDelegateService.asyncExecCommand(agentId, apiRequest);
        } catch (Throwable e) {
            logger.error("async exec command failure: " + e.getMessage(), e);
            return new ApiResponse()
                    .setState(ApiState.FAILED)
                    .setMessage("async exec command failure: " + e.getMessage());
        }
    }

    @RequestMapping("/agent/{agentId}/results/{requestId}")
    public ApiResponse pullResults(@PathVariable String agentId, @PathVariable String requestId,
                                   @RequestParam(value = "timeout", defaultValue = "30000") final int timeout) {
        try {
            ApiResponse apiResponse = apiActionDelegateService.pullResults(agentId, requestId, timeout);
            return apiResponse;
        } catch (Exception e) {
            logger.error("pull results failure: " + e.toString(), e);
            return new ApiResponse()
                    .setAgentId(agentId)
                    .setState(ApiState.FAILED)
                    .setMessage("pull results failure: " + e.toString());
        }
    }

    @GetMapping("/agent/{agentId}/sse_results/{requestId}")
    public SseEmitter sse_results(@PathVariable final String agentId,
                                  @PathVariable final String requestId,
                                  @RequestParam(value = "timeout", defaultValue = "300000") final int timeout) {
        final SseEmitter emitter = new SseEmitter(Long.valueOf(timeout));
        subscribeResults(agentId, requestId, timeout, emitter);
        return emitter;
    }

    @PostMapping("/agent/{agentId}/sse_async_exec")
    public SseEmitter sseAsyncExecCommand(@PathVariable final String agentId,
                                          @RequestBody final String requestBody,
                                          @RequestParam(value = "timeout", defaultValue = "300000") final int timeout) {
        final SseEmitter emitter = new SseEmitter(Long.valueOf(timeout));
        executorServiceConfig.getExecutorService().submit(new Runnable() {
            public void run() {
                ApiResponse apiResponse;
                try {
                    ApiRequest apiRequest = parseRequest(requestBody);
                    apiResponse = apiActionDelegateService.asyncExecCommand(agentId, apiRequest);
                    //check
                } catch (Throwable e) {
                    logger.error("async exec command failure: " + e.getMessage(), e);
                    ApiResponse response = new ApiResponse()
                            .setState(ApiState.FAILED)
                            .setMessage("async exec command failure: " + e.getMessage());
                    try {
                        emitter.send(JSON.toJSONString(response));
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                    return;
                }

                String requestId = apiResponse.getRequestId();
                subscribeResults(agentId, requestId, timeout, emitter);
            }
        });
        return emitter;
    }

    private void subscribeResults(String agentId, String requestId, int timeout, final SseEmitter emitter) {
        try {
            apiActionDelegateService.subscribeResults(agentId, requestId, timeout, new ApiActionDelegateService.ResponseListener() {
                @Override
                public boolean onMessage(ActionResponse response) {
                    try {
                        //TODO convert pb message to json
                        ApiResponse apiResponse = convertApiResponse(response);
                        emitter.send(JSON.toJSONString(apiResponse));
                        if (!response.getStatus().equals(ResponseStatus.CONTINUOUS)) {
                            emitter.complete();
                            return false;
                        } else {
                            return true;
                        }
                    } catch (IOException e) {
                        logger.error("send response failure", e);
                        emitter.completeWithError(e);
                        return false;
                    } catch (Throwable e) {
                        logger.error("process response failure", e);
                        emitter.completeWithError(e);
                        return false;
                    }
                }

                @Override
                public boolean onTimeout() {
                    //stop subscribing
                    return false;
                }
            });
            // we could send more events
            //emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    private ApiResponse convertApiResponse(ActionResponse actionResponse) {
        ApiResponse apiResponse = new ApiResponse()
                .setState(getState(actionResponse.getStatus()))
                .setAgentId(actionResponse.getAgentId())
                .setRequestId(actionResponse.getRequestId());

        if (actionResponse.hasSessionId()) {
            apiResponse.setSessionId(actionResponse.getSessionId().getValue());
        }
//        if (actionResponse.hasConsumerId()) {
//            apiResponse.setConsumerId(actionResponse.getConsumerId().getValue());
//        }
        if (actionResponse.hasMessage()) {
            apiResponse.setMessage(actionResponse.getMessage().getValue());
        }
        if (actionResponse.hasExecuteResult()) {
            ExecuteResult executeResult = actionResponse.getExecuteResult();
            if (executeResult.hasResultsJson()) {
                apiResponse.setResult(executeResult.getResultsJson().getValue());
            }
        }
        return apiResponse;
    }

    private ApiState getState(ResponseStatus status) {
        switch (status) {
            case SUCCEEDED:
                return ApiState.SUCCEEDED;
            case REFUSED:
                return ApiState.REFUSED;
            case CONTINUOUS:
                return ApiState.CONTINUOUS;
            case INTERRUPTED:
                return ApiState.INTERRUPTED;

            case FAILED:
            case UNRECOGNIZED:
                return ApiState.FAILED;
        }
        return ApiState.FAILED;
    }

    private ApiRequest parseRequest(String requestBody) throws ApiException {
        if (StringUtils.isBlank(requestBody)) {
            throw new ApiException("parse request failed: request body is empty");
        }
        try {
            return JSON.parseObject(requestBody, ApiRequest.class);
        } catch (Exception e) {
            throw new ApiException("parse request failed: " + e.getMessage(), e);
        }
    }

    private AgentVO checkAgentExists(String agentId) {
        Optional<AgentVO> optionalAgentVO = agentManageService.findAgentById(agentId).block();
        if (!optionalAgentVO.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
        }
        return optionalAgentVO.get();
    }

}

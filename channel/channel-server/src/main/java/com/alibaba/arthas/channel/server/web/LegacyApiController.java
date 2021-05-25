package com.alibaba.arthas.channel.server.web;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.ExecuteParams;
import com.alibaba.arthas.channel.proto.ExecuteResult;
import com.alibaba.arthas.channel.proto.RequestAction;
import com.alibaba.arthas.channel.proto.ResponseStatus;
import com.alibaba.arthas.channel.proto.ResultFormat;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.shell.term.impl.http.api.ApiAction;
import com.taobao.arthas.core.shell.term.impl.http.api.ApiRequest;
import com.taobao.arthas.core.shell.term.impl.http.api.ApiResponse;
import com.taobao.arthas.core.shell.term.impl.http.api.ApiState;
import com.taobao.arthas.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Compatible with Arthas Http API (https://arthas.aliyun.com/doc/http-api.html)
 *
 * @author gongdewei 2021/5/24
 */
@RestController
public class LegacyApiController {


    private static final Logger logger = LoggerFactory.getLogger(LegacyApiController.class);

    @Autowired
    private AgentManageService agentManageService;

    @Autowired
    private ApiActionDelegateService apiActionDelegateService;

    @RequestMapping("/legacy_api/{agentId}")
    public Mono<ApiResponse> process(@PathVariable String agentId, @RequestBody ApiRequest request) {

        try {
            checkAgentExists(agentId);

            // set default exec timeout
            if (request.getExecTimeout() == null) {
                request.setExecTimeout(30000);
            }

            Mono<ActionResponse> actionResponseMono = null;
            ApiAction action = parseApiAction(request.getAction());
            // process pull results
            switch (action) {
                case PULL_RESULTS:
                    checkRequestId(request);
                    actionResponseMono = apiActionDelegateService.pullResults(agentId, request.getRequestId(), request.getExecTimeout());
                    return convertToApiResponse(actionResponseMono);
            }

            // process other actions
            ActionRequest actionRequest = createActionRequest(agentId, request);
            switch (actionRequest.getAction()) {
                case EXECUTE:
                    actionResponseMono = apiActionDelegateService.execCommand(agentId, actionRequest);
                    return convertToApiResponse(actionResponseMono);
                case ASYNC_EXECUTE:
                    actionResponseMono = apiActionDelegateService.asyncExecCommand(agentId, actionRequest);
                    return convertToApiResponse(actionResponseMono);
                case INIT_SESSION:
                    actionResponseMono = apiActionDelegateService.initSession(agentId);
                    return convertToApiResponse(actionResponseMono);
                case CLOSE_SESSION:
                    actionResponseMono = apiActionDelegateService.closeSession(agentId, request.getSessionId());
                    return convertToApiResponse(actionResponseMono);
                default:
                    throw new UnsupportedOperationException("unsupported action: " + actionRequest.getAction());
            }
        } catch (Throwable e) {
            logger.error("process request failed, agentId: {}, request: {}", agentId, request, e);
            ApiResponse response = new ApiResponse();
            response.setState(ApiState.FAILED)
                    .setMessage("process request failed: " + e.getMessage())
                    .setAgentId(agentId)
                    .setRequestId(request.getRequestId())
                    .setSessionId(request.getSessionId());
            return Mono.just(response);
        }
    }

    private void checkRequestId(ApiRequest request) {
        if (StringUtils.isBlank(request.getRequestId())) {
            throw new IllegalArgumentException("Invalid request, the 'requestId' is required");
        }
    }

    private Mono<ApiResponse> convertToApiResponse(Mono<ActionResponse> actionResponseMono) {
        return actionResponseMono.flatMap((actionResponse) -> Mono.just(convertToApiResponse(actionResponse)));
    }

    private ApiResponse convertToApiResponse(ActionResponse actionResponse) {
        ApiResponse response = new ApiResponse();
        response.setAgentId(actionResponse.getAgentId());
        response.setState(convertToApiState(actionResponse.getStatus()));
        if (StringUtils.hasText(actionResponse.getRequestId())) {
            response.setRequestId(actionResponse.getRequestId());
        }
        if (StringUtils.hasText(actionResponse.getMessage())) {
            response.setMessage(actionResponse.getMessage());
        }
        if (StringUtils.hasText(actionResponse.getSessionId())) {
            response.setSessionId(actionResponse.getSessionId());
        }
        if (actionResponse.hasExecuteResult()) {
            String resultsJson = null;
            ExecuteResult executeResult = actionResponse.getExecuteResult();
            if (executeResult.hasResultsJson()) {
                resultsJson = executeResult.getResultsJson().getValue();
            }
            Map<String, Object> body = new TreeMap<String, Object>();

            // attributes that are not available
//            body.put("command", commandLine);
            //packing results
//            body.put("jobId", job.id());
//            body.put("jobStatus", job.status());
//            body.put("timeExpired", timeExpired);
//            if (timeExpired) {
//                body.put("timeout", timeout);
//            }

            // set result as list
            body.put("results", parseJsonToList(resultsJson));
            // set results is a json string
            //body.put("resultsJson", resultsJson);

            response.setBody(body);
        }
        return response;
    }

    private List<Map> parseJsonToList(String resultsJson) {
        return JSON.parseArray(resultsJson, Map.class);
    }

    private ActionRequest createActionRequest(String agentId, ApiRequest request) {
        ActionRequest.Builder actionRequest = ActionRequest.newBuilder()
                .setAgentId(agentId)
                .setAction(convertToRequestAction(request.getAction()))
                .setExecuteParams(createExecuteParams(request));

        if (StringUtils.hasText(request.getRequestId())) {
            actionRequest.setRequestId(request.getRequestId());
        }
        if (StringUtils.hasText(request.getSessionId())) {
            actionRequest.setSessionId(request.getSessionId());
        }

        return actionRequest.build();
    }

    private ExecuteParams createExecuteParams(ApiRequest request) {
        return ExecuteParams.newBuilder()
                .setCommandLine(request.getCommand() != null ? request.getCommand() : "")
                .setResultFormat(ResultFormat.JSON)
                .setExecTimeout(request.getExecTimeout())
                .build();
    }

    private AgentVO checkAgentExists(String agentId) {
        Optional<AgentVO> optionalAgentVO = agentManageService.findAgentById(agentId).block();
        if (!optionalAgentVO.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
        }
        return optionalAgentVO.get();
    }

    private ApiState convertToApiState(ResponseStatus status) {
        switch (status) {
            case SUCCEEDED:
            case CONTINUOUS:
                return ApiState.SUCCEEDED;
            case REFUSED:
                return ApiState.REFUSED;
            case INTERRUPTED:
                return ApiState.INTERRUPTED;
            case FAILED:
            case UNRECOGNIZED:
                return ApiState.FAILED;
        }
        return ApiState.FAILED;
    }

    private RequestAction convertToRequestAction(String actionStr) {
        ApiAction action = parseApiAction(actionStr);

        switch (action) {
            case INIT_SESSION:
                return RequestAction.INIT_SESSION;
            case CLOSE_SESSION:
                return RequestAction.CLOSE_SESSION;
            case EXEC:
                return RequestAction.EXECUTE;
            case ASYNC_EXEC:
                return RequestAction.ASYNC_EXECUTE;
            case INTERRUPT_JOB:
                return RequestAction.INTERRUPT_JOB;
            case PULL_RESULTS:
            case JOIN_SESSION:
            case SESSION_INFO:
            default:
                throw new IllegalArgumentException("unsupported action: " + actionStr);
        }
    }

    private ApiAction parseApiAction(String actionStr) {
        ApiAction action;
        try {
            action = ApiAction.valueOf(actionStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown action: " + actionStr);
        }
        return action;
    }
}

package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.logger.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Http Restful Api Handler
 * @author gongdewei 2020-03-18
 */
public class HttpApiHandler {

    private static final Logger logger = LogUtil.getArthasLogger();
    private final SessionManager sessionManager;
    private final AtomicInteger requestIdGenerator = new AtomicInteger(0);
    private static HttpApiHandler instance;

    public static HttpApiHandler getInstance() {
        if (instance == null){
            synchronized (HttpApiHandler.class){
                instance = new HttpApiHandler();
            }
        }
        return instance;
    }

    private HttpApiHandler() {
        sessionManager = ArthasBootstrap.getInstance().getSessionManager();
    }

    public HttpResponse handle(FullHttpRequest request) throws Exception {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                HttpResponseStatus.OK);

        ApiResponse result;
        String requestBody = null;
        String requestId = "req_" + requestIdGenerator.addAndGet(1);
        try {
            HttpMethod method = request.method();
            if (HttpMethod.POST.equals(method)){
                requestBody = getBody(request);
                ApiRequest apiRequest = parseRequest(requestBody);
                apiRequest.setRequestId(requestId);
                result = processRequest(apiRequest);
            } else {
                result = createResponse(ApiState.REFUSED, "Unsupported http method: "+method.name());
            }
        } catch (Throwable e) {
            result = createResponse(ApiState.FAILED, "Process request error: "+e.getMessage());
            logger.error("arthas", "arthas process http api request error: " + request.uri()+", request body: "+requestBody, e);
        }
        if (result == null) {
            result = createResponse(ApiState.FAILED, "The request was not processed");
        }

        result.setRequestId(requestId);
        String jsonResult = JSON.toJSONString(result);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
        response.content().writeBytes(jsonResult.getBytes("UTF-8"));
        return response;
    }

    private ApiRequest parseRequest(String requestBody) throws ApiException {
        if (StringUtils.isBlank(requestBody)){
            throw new ApiException("parse request failed: request body is empty");
        }
        try {
            //Object jsonRequest = JSON.parse(requestBody);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(requestBody, ApiRequest.class);
        } catch (Exception e) {
            throw new ApiException("parse request failed: "+e.getMessage(), e);
        }
    }

    private ApiResponse processRequest(ApiRequest apiRequest) {

        String actionStr = apiRequest.getAction();
        try {
            if (StringUtils.isBlank(actionStr)){
                throw new ApiException("'action' is required");
            }
            ApiAction action;
            try {
                action = ApiAction.valueOf(actionStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("unknown action: "+actionStr);
            }

            //no session required
            if(ApiAction.INIT_SESSION.equals(action)){
                return processInitSessionRequest(apiRequest);
            }

            //required session
            String sessionId = apiRequest.getSessionId();
            if (StringUtils.isBlank(sessionId)) {
                throw new ApiException("'sessionId' is required");
            }
            Session session = sessionManager.getSession(sessionId);
            if (session == null){
                throw new ApiException("session not found");
            }
            sessionManager.updateAccessTime(session);

            switch (action) {
                case EXEC:
                    return processExecRequest(apiRequest, session);
                case SESSION_INFO:
                    return processSessionInfoRequest(apiRequest, session);
                case CLOSE_SESSION:
                    return processCloseSessionRequest(apiRequest, session);
            }

        } catch (ApiException e) {
            logger.info("arthas", e.getMessage(), e);
            return createResponse(ApiState.FAILED, e.getMessage());
        } catch (Throwable e) {
            logger.error("arthas", "process http api request failed", e);
            return createResponse(ApiState.FAILED, "process http api request failed");
        }

        return createResponse(ApiState.REFUSED, "Unsupported action: "+actionStr);
    }

    private ApiResponse processInitSessionRequest(ApiRequest apiRequest) throws ApiException {
        ApiResponse response = new ApiResponse();
        Session session = sessionManager.createSession();
        if (session != null) {
            response.setSessionId(session.getSessionId())
                    .setState(ApiState.SUCCEEDED);
        } else {
            throw new ApiException("create api session failed");
        }
        return response;
    }

    private ApiResponse processSessionInfoRequest(ApiRequest apiRequest, Session session) {
        ApiResponse response = new ApiResponse();
        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("pid", session.getPid());
        body.put("sessionId", session.getSessionId());
        body.put("createTime", session.getCreateTime());
        body.put("lastAccessTime", session.getLastAccessTime());

        response.setState(ApiState.SUCCEEDED).setBody(body);
        return response;
    }

    private ApiResponse processCloseSessionRequest(ApiRequest apiRequest, Session session) {
        sessionManager.removeSession(session.getSessionId());

        return null;
    }

    private ApiResponse processExecRequest(ApiRequest apiRequest, Session session) {
        String command = apiRequest.getCommand();
        //TODO
        return null;
    }

    private ApiResponse createResponse(ApiState apiState, String message) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setState(apiState);
        apiResponse.setMessage(message);
        return apiResponse;
    }

    private String getBody(FullHttpRequest request){
        ByteBuf buf = request.content();
        return buf.toString(CharsetUtil.UTF_8);
    }

}

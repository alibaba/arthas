package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.logger.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;


/**
 * Http Restful Api Handler
 * @author gongdewei 2020-03-18
 */
public class HttpApiHandler {

    private static final Logger logger = LogUtil.getArthasLogger();

    public HttpResponse handle(FullHttpRequest request) throws Exception {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                HttpResponseStatus.OK);

        ApiResponse result = null;
        String requestBody = null;
        try {
            HttpMethod method = request.method();
            if (HttpMethod.POST.equals(method)){
                requestBody = getBody(request);
                ApiRequest apiRequest = parseRequest(requestBody);
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

        String action = apiRequest.getAction();
        if ("exec".equalsIgnoreCase(action)){
            return processExecRequest(apiRequest);
        } else if("init_session".equalsIgnoreCase(action)){
            return processInitSessionRequest(apiRequest);
        } else if("close_session".equalsIgnoreCase(action)){
            return processCloseSessionRequest(apiRequest);
        }

        return createResponse(ApiState.REFUSED, "Unsupported action: "+action);
    }

    private ApiResponse processInitSessionRequest(ApiRequest apiRequest) {
//        ShellServer shellServer = getShellServer();
//        shellServer.createShell()

        return null;
    }

    private ApiResponse processCloseSessionRequest(ApiRequest apiRequest) {
        return null;
    }

    private ApiResponse processExecRequest(ApiRequest apiRequest) {
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

//    private ShellServer getShellServer() {
//        return ArthasBootstrap.getInstance().getShellServer();
//    }
}

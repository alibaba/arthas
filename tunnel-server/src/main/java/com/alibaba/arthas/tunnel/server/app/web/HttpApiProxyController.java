package com.alibaba.arthas.tunnel.server.app.web;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.alibaba.arthas.tunnel.common.URIConstans;
import com.alibaba.arthas.tunnel.server.AgentInfo;
import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author haidao
 * @date 2021/11/3
 */
@Controller
public class HttpApiProxyController {
    private final static Logger logger = LoggerFactory.getLogger(HttpApiProxyController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private TunnelServer tunnelServer;

    @RequestMapping(value = "/apiProxy/{agentId}/api", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> execute(@PathVariable(name = "agentId") String agentId,
                                     @RequestBody ApiRequest apiRequest) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        Optional<AgentInfo> findAgent = tunnelServer.findAgent(agentId);
        if (!findAgent.isPresent()) {
            logger.error("can not find agent by agentId: {}", agentId);
            return ResponseEntity.notFound().build();
        }

        String requestId = apiRequest.getRequestId();
        // if requestId from frontend is blank, then generate it
        if (StringUtils.isBlank(requestId)) {
            requestId = RandomStringUtils.random(20, true, true).toUpperCase();
            apiRequest.setRequestId(requestId);
        }

        ChannelHandlerContext agentCtx = findAgent.get().getChannelHandlerContext();

        Promise<SimpleHttpResponse> httpResponsePromise = GlobalEventExecutor.INSTANCE.newPromise();

        tunnelServer.addProxyRequestPromise(requestId, httpResponsePromise);

        URI uri = UriComponentsBuilder.newInstance().scheme(URIConstans.RESPONSE).path("/")
                .queryParam(URIConstans.METHOD, MethodConstants.HTTP_API_PROXY)
                .queryParam(URIConstans.API_REQUEST_BODY, MAPPER.writeValueAsString(apiRequest))
                .queryParam(URIConstans.PROXY_REQUEST_ID, requestId)
                .build().toUri();

        agentCtx.channel().writeAndFlush(new TextWebSocketFrame(uri.toString()));
        logger.info("waiting for arthas agent http proxy, agentId: {}, apiRequest: {}", agentId, apiRequest);

        SimpleHttpResponse simpleHttpResponse = httpResponsePromise.get(15, TimeUnit.SECONDS);

        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.status(simpleHttpResponse.getStatus());
        for (Map.Entry<String, String> entry : simpleHttpResponse.getHeaders().entrySet()) {
            bodyBuilder.header(entry.getKey(), entry.getValue());
        }
        return bodyBuilder.body(simpleHttpResponse.getContent());
    }
}

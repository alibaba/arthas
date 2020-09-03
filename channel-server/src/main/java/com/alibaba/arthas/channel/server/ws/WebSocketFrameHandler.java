package com.alibaba.arthas.channel.server.ws;

import com.alibaba.arthas.channel.proto.ActionResponse;
import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.proto.ConsoleData;
import com.alibaba.arthas.channel.proto.ConsoleResult;
import com.alibaba.arthas.channel.proto.ResponseStatus;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
import com.google.protobuf.ByteString;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

/**
 * @author gongdewei 2020/9/2
 */
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameHandler.class);

    private WebSocketServer wsServer;
    private final AgentManageService agentManageService;
    private final ApiActionDelegateService apiActionDelegateService;

    public WebSocketFrameHandler(WebSocketServer wsServer) {
        this.wsServer = wsServer;
        agentManageService = wsServer.getAgentManageService();
        apiActionDelegateService = wsServer.getApiActionDelegateService();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        //do nothing
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshake = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            // http request uri
            String uri = handshake.requestUri();
            logger.info("websocket handshake complete, uri: {}", uri);

            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(uri).build().getQueryParams();
            String method = parameters.getFirst("method");

            if ("connectArthas".equals(method)) { // form browser
                connectArthas(ctx, parameters);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void connectArthas(ChannelHandlerContext ctx, MultiValueMap<String, String> parameters)
            throws Exception {

        List<String> agentIds = parameters.getOrDefault("id", Collections.emptyList());

        if (agentIds.isEmpty()) {
            logger.error("arthas agent id can not be null, parameters: ", parameters);
            throw new IllegalArgumentException("arthas agent id can not be null");
        }

        String agentId = agentIds.get(0);
        logger.info("try to connect to arthas agent, id: " + agentId);

        AgentVO agentVO = agentManageService.findAgentById(agentId);
        if (agentVO == null) {
            String error = "Can not find arthas agent by id: " + agentIds;
            sendErrorAndClose(ctx, error);
            logger.error("Can not find arthas agent by id: {}", agentIds);
            throw new IllegalArgumentException(error);
        }

        if (!AgentStatus.IN_SERVICE.name().equals(agentVO.getAgentStatus())) {
            String error = "Agent ["+agentId+"] is unavailable, please try again later.";
            sendErrorAndClose(ctx, error);
            logger.error(error);
            throw new IllegalArgumentException(error);
        }

        Promise<ActionResponse> responsePromise = apiActionDelegateService.openConsole(agentId, 15000);
        ActionResponse actionResponse = responsePromise.get();
        if (!actionResponse.getStatus().equals(ResponseStatus.SUCCEEDED)) {
            logger.error("open console failure, response: {}", actionResponse);
            throw new Exception("open console failure");

        }

        String consoleId = actionResponse.getConsoleResult().getConsoleId();
        logger.info("open console successfully, consoleId: {}", consoleId);

        // replace channel handler
        ctx.pipeline().remove(this);
        // proxy forward: frontend WebConsole => arthas agent
        ctx.pipeline().addLast(new ForwardHandler(apiActionDelegateService, agentId, consoleId));

        //proxy backward: arthas agent => frontend WebConsole
        apiActionDelegateService.subscribeResults(agentId, consoleId, 15 * 1000, new ApiActionDelegateService.ResponseListener() {
            @Override
            public boolean onMessage(ActionResponse response) {
                if (response.hasConsoleResult()) {
                    ConsoleResult consoleResult = response.getConsoleResult();
                    List<ConsoleData> resultsList = consoleResult.getResultsList();
                    for (ConsoleData consoleData : resultsList) {
                        String dataType = consoleData.getDataType();
                        ByteString dataBytes = consoleData.getDataBytes();
                        if ("tty".equals(dataType)) {
                            TextWebSocketFrame msg = new TextWebSocketFrame(Unpooled.wrappedBuffer(dataBytes.toByteArray()));
                            try {
                                ctx.channel().writeAndFlush(msg);
                            } catch (Exception e) {
                                logger.error("send console output error, consoleId: {}", consoleId, e);
                                return false;
                            }
                        } else {
                            logger.info("unsupported data type: {}, consoleId: {}", dataType, consoleId);
                        }
                    }
                    //ctx.channel().flush();
                }
                boolean continuous = response.getStatus().equals(ResponseStatus.CONTINUOUS);
                if (!continuous) {
                    logger.info("console is closed, agentId: {}, consoleId: {}, response: {}", agentId, consoleId, response);
                }
                return continuous;
            }

            @Override
            public boolean onTimeout() {
                if (!ctx.channel().isActive()) {
                    logger.info("console connection is broken, closing console. agentId: {}, consoleId: {}", agentId, consoleId);
                    //TODO close clonsole
                    return false;
                }
                //check agent status
                AgentVO agentVO = agentManageService.findAgentById(agentId);
                if (agentVO == null || !AgentStatus.IN_SERVICE.name().equals(agentVO.getAgentStatus())) {
                    logger.warn("Agent status is not ready, closing console. agentId: {}, consoleId: {}", agentId, consoleId);
                    sendErrorAndClose(ctx, "Agent is unavailable, please try again later.");
                    return false;
                }
                // continue subscribing
                return true;
            }
        });

        ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                //TODO stop subscribeResults
            }
        });
    }

    private void sendErrorAndClose(ChannelHandlerContext ctx, String error) {
        ctx.channel().writeAndFlush(new CloseWebSocketFrame(2000, error))
                .addListener(ChannelFutureListener.CLOSE);
    }
}

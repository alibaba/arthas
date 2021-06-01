package com.alibaba.arthas.channel.server.ws;

import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gongdewei 2020/9/2
 */
public final class ForwardHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final static Logger logger = LoggerFactory.getLogger(ForwardHandler.class);
    private final ApiActionDelegateService apiActionDelegateService;
    private final String agentId;
    private final String consoleId;

    public ForwardHandler(ApiActionDelegateService apiActionDelegateService, String agentId, String consoleId) {
        this.apiActionDelegateService = apiActionDelegateService;
        this.agentId = agentId;
        this.consoleId = consoleId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) msg;
            String inputData = textWebSocketFrame.text();
            apiActionDelegateService.consoleInput(agentId, consoleId, inputData);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        closeConsole();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("", cause);
        ctx.close();
    }

    private void closeConsole() {
        try {
            apiActionDelegateService.closeConsole(agentId, consoleId);
        } catch (Exception e) {
            logger.error("close console error, agentId: {}, consoleId: {}", agentId, consoleId, e);
        }
    }
}

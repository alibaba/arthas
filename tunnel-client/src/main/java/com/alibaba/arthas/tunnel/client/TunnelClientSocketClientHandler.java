
package com.alibaba.arthas.tunnel.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.URIConstans;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * 
 * @author hengyunabc 2019-08-28
 *
 */
public class TunnelClientSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final static Logger logger = LoggerFactory.getLogger(TunnelClientSocketClientHandler.class);

    private final TunnelClient tunnelClient;
    private ChannelPromise registerPromise;

    public TunnelClientSocketClientHandler(TunnelClient tunnelClient) {
        this.tunnelClient = tunnelClient;
    }

    public ChannelFuture registerFuture() {
        return registerPromise;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        registerPromise = ctx.newPromise();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String text = textFrame.text();

            logger.info("receive TextWebSocketFrame: {}", text);

            QueryStringDecoder queryDecoder = new QueryStringDecoder(text);
            Map<String, List<String>> parameters = queryDecoder.parameters();
            List<String> methodList = parameters.get(URIConstans.METHOD);
            String method = null;
            if (methodList != null && !methodList.isEmpty()) {
                method = methodList.get(0);
            }

            if (MethodConstants.AGENT_REGISTER.equals(method)) {
                List<String> idList = parameters.get(URIConstans.ID);
                if (idList != null && !idList.isEmpty()) {
                    this.tunnelClient.setId(idList.get(0));
                }
                registerPromise.setSuccess();
            }

            if (MethodConstants.START_TUNNEL.equals(method)) {
                QueryStringEncoder queryEncoder = new QueryStringEncoder(this.tunnelClient.getTunnelServerUrl());
                queryEncoder.addParam(URIConstans.METHOD, MethodConstants.OPEN_TUNNEL);
                queryEncoder.addParam(URIConstans.CLIENT_CONNECTION_ID, parameters.get(URIConstans.CLIENT_CONNECTION_ID).get(0));
                queryEncoder.addParam(URIConstans.ID, parameters.get(URIConstans.ID).get(0));

                final URI forwardUri = queryEncoder.toUri();

                logger.info("start ForwardClient, uri: {}", forwardUri);
                try {
                    ForwardClient forwardClient = new ForwardClient(forwardUri);
                    forwardClient.start();
                } catch (Throwable e) {
                    logger.error("start ForwardClient error, forwardUri: {}", forwardUri, e);
                }
            }

        }
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                logger.error("try to reconnect to tunnel server, uri: {}", tunnelClient.getTunnelServerUrl());
                try {
                    tunnelClient.connect(true);
                } catch (Throwable e) {
                    logger.error("reconnect error", e);
                }
            }
        }, tunnelClient.getReconnectDelay(), TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!registerPromise.isDone()) {
            registerPromise.setFailure(cause);
        }
        ctx.fireExceptionCaught(cause);
    }
}

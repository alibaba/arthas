
package com.alibaba.arthas.tunnel.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.alibaba.arthas.tunnel.common.URIConstans;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

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
                tunnelClient.setConnected(true);
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

            if (MethodConstants.HTTP_PROXY.equals(method)) {
                /**
                 * <pre>
                 * 1. 从proxy请求里读取到目标的 targetUrl，和 requestId
                 * 2. 然后通过 ProxyClient直接请求得到结果
                 * 3. 把response结果转为 byte[]，再转为base64，再统一组合的一个url，再用 TextWebSocketFrame 发回去
                 * </pre>
                 * 
                 */
                ProxyClient proxyClient = new ProxyClient();
                List<String> targetUrls = parameters.get(URIConstans.TARGET_URL);

                List<String> requestIDs = parameters.get(URIConstans.PROXY_REQUEST_ID);
                String id = null;
                if (requestIDs != null && !requestIDs.isEmpty()) {
                    id = requestIDs.get(0);
                }
                if (id == null) {
                    logger.error("error, http proxy need {}", URIConstans.PROXY_REQUEST_ID);
                    return;
                }

                if (targetUrls != null && !targetUrls.isEmpty()) {
                    String targetUrl = targetUrls.get(0);
                    SimpleHttpResponse simpleHttpResponse = proxyClient.query(targetUrl);

                    ByteBuf byteBuf = Base64
                            .encode(Unpooled.wrappedBuffer(SimpleHttpResponse.toBytes(simpleHttpResponse)));
                    String requestData = byteBuf.toString(CharsetUtil.UTF_8);

                    QueryStringEncoder queryEncoder = new QueryStringEncoder("");
                    queryEncoder.addParam(URIConstans.METHOD, MethodConstants.HTTP_PROXY);
                    queryEncoder.addParam(URIConstans.PROXY_REQUEST_ID, id);
                    queryEncoder.addParam(URIConstans.PROXY_RESPONSE_DATA, requestData);

                    String url = queryEncoder.toString();
                    ctx.writeAndFlush(new TextWebSocketFrame(url));
                }
            }

        }
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        tunnelClient.setConnected(false);
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
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(new PingWebSocketFrame());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("TunnelClient error, tunnel server url: " + tunnelClient.getTunnelServerUrl(), cause);
        if (!registerPromise.isDone()) {
            registerPromise.setFailure(cause);
        }
        ctx.close();
    }
}

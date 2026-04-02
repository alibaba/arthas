
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
 * 隧道客户端Socket处理器
 *
 * <p>处理与隧道服务器之间的WebSocket通信，负责：</p>
 * <ul>
 *   <li>接收并处理来自隧道服务器的消息</li>
 *   <li>处理代理注册响应</li>
 *   <li>处理启动隧道请求</li>
 *   <li>处理HTTP代理请求</li>
 *   <li>处理连接断开后的自动重连</li>
 *   <li>处理空闲事件，发送心跳帧</li>
 * </ul>
 *
 * @author hengyunabc 2019-08-28
 *
 */
public class TunnelClientSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(TunnelClientSocketClientHandler.class);

    /**
     * 隧道客户端实例
     */
    private final TunnelClient tunnelClient;

    /**
     * 注册Promise
     * <p>用于通知代理注册是否成功</p>
     */
    private ChannelPromise registerPromise;

    /**
     * 构造函数
     *
     * @param tunnelClient 隧道客户端实例
     */
    public TunnelClientSocketClientHandler(TunnelClient tunnelClient) {
        this.tunnelClient = tunnelClient;
    }

    /**
     * 获取注册Future
     *
     * <p>返回的Future可以用于监听代理注册是否成功</p>
     *
     * @return 注册Future对象
     */
    public ChannelFuture registerFuture() {
        return registerPromise;
    }

    /**
     * 当处理器被添加到通道时调用
     *
     * <p>初始化注册Promise</p>
     *
     * @param ctx 通道处理器上下文
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        registerPromise = ctx.newPromise();
    }

    /**
     * 读取通道消息
     *
     * <p>处理来自隧道服务器的WebSocket消息，根据不同的方法类型执行相应的操作：</p>
     * <ul>
     *   <li>agentRegister: 处理代理注册响应</li>
     *   <li>startTunnel: 启动转发客户端</li>
     *   <li>httpProxy: 处理HTTP代理请求</li>
     * </ul>
     *
     * @param ctx 通道处理器上下文
     * @param frame WebSocket帧对象
     * @throws Exception 处理过程中可能抛出异常
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 只处理文本WebSocket帧
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String text = textFrame.text();

            logger.info("receive TextWebSocketFrame: {}", text);

            // 解析消息参数
            QueryStringDecoder queryDecoder = new QueryStringDecoder(text);
            Map<String, List<String>> parameters = queryDecoder.parameters();
            List<String> methodList = parameters.get(URIConstans.METHOD);
            String method = null;
            if (methodList != null && !methodList.isEmpty()) {
                method = methodList.get(0);
            }

            // 处理代理注册响应
            if (MethodConstants.AGENT_REGISTER.equals(method)) {
                List<String> idList = parameters.get(URIConstans.ID);
                if (idList != null && !idList.isEmpty()) {
                    // 保存隧道服务器分配的代理ID
                    this.tunnelClient.setId(idList.get(0));
                }
                tunnelClient.setConnected(true);
                // 标记注册成功
                registerPromise.setSuccess();
            }

            // 处理启动隧道请求
            if (MethodConstants.START_TUNNEL.equals(method)) {
                // 构建转发URI
                QueryStringEncoder queryEncoder = new QueryStringEncoder(this.tunnelClient.getTunnelServerUrl());
                queryEncoder.addParam(URIConstans.METHOD, MethodConstants.OPEN_TUNNEL);
                queryEncoder.addParam(URIConstans.CLIENT_CONNECTION_ID, parameters.get(URIConstans.CLIENT_CONNECTION_ID).get(0));
                queryEncoder.addParam(URIConstans.ID, parameters.get(URIConstans.ID).get(0));

                final URI forwardUri = queryEncoder.toUri();

                logger.info("start ForwardClient, uri: {}", forwardUri);
                try {
                    // 创建并启动转发客户端
                    ForwardClient forwardClient = new ForwardClient(forwardUri);
                    forwardClient.start();
                } catch (Throwable e) {
                    logger.error("start ForwardClient error, forwardUri: {}", forwardUri, e);
                }
            }

            // 处理HTTP代理请求
            if (MethodConstants.HTTP_PROXY.equals(method)) {
                /**
                 * HTTP代理处理流程：
                 * <pre>
                 * 1. 从代理请求中读取目标URL和请求ID
                 * 2. 通过ProxyClient直接请求目标URL获取结果
                 * 3. 将响应结果转为byte数组，再进行Base64编码
                 * 4. 将编码后的数据组合成URL格式，通过TextWebSocketFrame发送回去
                 * </pre>
                 *
                 */
                ProxyClient proxyClient = new ProxyClient();
                List<String> targetUrls = parameters.get(URIConstans.TARGET_URL);

                // 获取请求ID
                List<String> requestIDs = parameters.get(URIConstans.PROXY_REQUEST_ID);
                String id = null;
                if (requestIDs != null && !requestIDs.isEmpty()) {
                    id = requestIDs.get(0);
                }
                if (id == null) {
                    logger.error("error, http proxy need {}", URIConstans.PROXY_REQUEST_ID);
                    return;
                }

                // 处理目标URL请求
                if (targetUrls != null && !targetUrls.isEmpty()) {
                    String targetUrl = targetUrls.get(0);
                    // 通过代理客户端请求目标URL
                    SimpleHttpResponse simpleHttpResponse = proxyClient.query(targetUrl);

                    ByteBuf byteBuf = null;
                    try{
                        // 将响应结果转为Base64编码
                        byteBuf = Base64
                                .encode(Unpooled.wrappedBuffer(SimpleHttpResponse.toBytes(simpleHttpResponse)));
                        String requestData = byteBuf.toString(CharsetUtil.UTF_8);

                        // 构建响应URL
                        QueryStringEncoder queryEncoder = new QueryStringEncoder("");
                        queryEncoder.addParam(URIConstans.METHOD, MethodConstants.HTTP_PROXY);
                        queryEncoder.addParam(URIConstans.PROXY_REQUEST_ID, id);
                        queryEncoder.addParam(URIConstans.PROXY_RESPONSE_DATA, requestData);

                        String url = queryEncoder.toString();
                        // 发送响应回隧道服务器
                        ctx.writeAndFlush(new TextWebSocketFrame(url));
                    }finally {
                        // 释放ByteBuf资源
                        if (byteBuf != null) {
                            byteBuf.release();
                        }
                    }
                }
            }

        }
    }

    /**
     * 当通道从事件循环注销时调用
     *
     * <p>连接断开时的处理逻辑：</p>
     * <ol>
     *   <li>将连接状态设置为false</li>
     *   <li>延迟指定时间后尝试重新连接</li>
     * </ol>
     *
     * @param ctx 通道处理器上下文
     * @throwsException 处理过程中可能抛出异常
     */
    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        // 更新连接状态
        tunnelClient.setConnected(false);
        // 延迟重连，避免频繁重连
        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                logger.error("try to reconnect to tunnel server, uri: {}", tunnelClient.getTunnelServerUrl());
                try {
                    // 尝试重新连接
                    tunnelClient.connect(true);
                } catch (Throwable e) {
                    logger.error("reconnect error", e);
                }
            }
        }, tunnelClient.getReconnectDelay(), TimeUnit.SECONDS);
    }

    /**
     * 当用户事件触发时调用
     *
     * <p>主要用于处理空闲事件，发送WebSocket Ping帧保持连接活跃</p>
     *
     * @param ctx 通道处理器上下文
     * @param evt 用户事件对象
     * @throws Exception 处理过程中可能抛出异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 发送Ping帧作为心跳
            ctx.writeAndFlush(new PingWebSocketFrame());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 当异常发生时调用
     *
     * <p>处理通道异常：</p>
     * <ol>
     *   <li>记录错误日志</li>
     *   <li>如果注册未完成，标记注册失败</li>
     *   <li>关闭通道</li>
     * </ol>
     *
     * @param ctx 通道处理器上下文
     * @param cause 异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("TunnelClient error, tunnel server url: " + tunnelClient.getTunnelServerUrl(), cause);
        // 如果注册还未完成，标记注册失败
        if (!registerPromise.isDone()) {
            registerPromise.setFailure(cause);
        }
        // 关闭通道
        ctx.close();
    }
}

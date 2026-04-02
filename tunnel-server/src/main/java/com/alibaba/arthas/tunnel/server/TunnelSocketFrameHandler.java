
package com.alibaba.arthas.tunnel.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.alibaba.arthas.tunnel.common.URIConstans;
import com.alibaba.arthas.tunnel.server.utils.HttpUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * Tunnel WebSocket帧处理器
 * <p>
 * 该类是Tunnel服务器的核心处理器，负责处理WebSocket连接和消息转发。
 * 主要功能包括：
 * 1. 处理来自浏览器的Arthas连接请求
 * 2. 处理来自Arthas Agent的注册请求
 * 3. 在浏览器和Arthas Agent之间建立双向通信隧道
 * 4. 处理HTTP代理请求和响应
 * 5. 管理连接的生命周期和超时处理
 * </p>
 *
 * @author hengyunabc 2019-08-27
 *
 */
public class TunnelSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    // 日志记录器
    private final static Logger logger = LoggerFactory.getLogger(TunnelSocketFrameHandler.class);

    // Tunnel服务器实例，用于管理Agent连接和客户端连接信息
    private TunnelServer tunnelServer;

    /**
     * 构造函数
     *
     * @param tunnelServer Tunnel服务器实例
     */
    public TunnelSocketFrameHandler(TunnelServer tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    /**
     * 处理用户事件触发
     * <p>
     * 该方法处理Netty管道中的用户事件，主要包括：
     * 1. WebSocket握手完成事件：解析请求参数，根据方法类型执行不同的处理逻辑
     * 2. 空闲状态事件：发送Ping帧保持连接活跃
     * </p>
     *
     * @param ctx 通道处理器上下文
     * @param evt 用户事件对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断事件是否为WebSocket握手完成事件
        if (evt instanceof HandshakeComplete) {
            HandshakeComplete handshake = (HandshakeComplete) evt;
            // 获取HTTP请求URI
            String uri = handshake.requestUri();
            logger.info("websocket handshake complete, uri: {}", uri);

            // 解析URI中的查询参数
            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(uri).build().getQueryParams();
            // 获取方法参数
            String method = parameters.getFirst(URIConstans.METHOD);

            // 如果方法为连接Arthas，说明请求来自浏览器
            if (MethodConstants.CONNECT_ARTHAS.equals(method)) { // form browser
                connectArthas(ctx, parameters);
            }
            // 如果方法为Agent注册，说明请求来自Arthas Agent
            else if (MethodConstants.AGENT_REGISTER.equals(method)) { // form arthas agent, register
                agentRegister(ctx, handshake, uri);
            }
            // 如果方法为打开隧道，说明请求来自Arthas Agent，要求打开到客户端的隧道
            if (MethodConstants.OPEN_TUNNEL.equals(method)) { // from arthas agent open tunnel
                String clientConnectionId = parameters.getFirst(URIConstans.CLIENT_CONNECTION_ID);
                openTunnel(ctx, clientConnectionId);
            }
        }
        // 如果是空闲状态事件，发送Ping帧保持连接活跃
        else if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(new PingWebSocketFrame());
        }
        // 其他事件传递给下一个处理器
        else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * 读取WebSocket帧消息
     * <p>
     * 该方法处理接收到的WebSocket消息帧，主要处理来自Arthas Agent的文本消息。
     * 只有Arthas Agent注册建立的连接才可能有数据传输到此处
     * </p>
     *
     * @param ctx  通道处理器上下文
     * @param frame WebSocket消息帧
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 只有 arthas agent register建立的 channel 才可能有数据到这里
        // 判断帧类型是否为文本帧
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            // 获取文本内容
            String text = textFrame.text();

            // 解析文本内容中的查询参数
            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(text).build()
                    .getQueryParams();

            // 获取方法参数
            String method = parameters.getFirst(URIConstans.METHOD);

            /**
             * 处理HTTP代理响应
             * <pre>
             * 1. 之前http proxy请求已发送到 tunnel cleint，这里接收到 tunnel client的结果，并解析出SimpleHttpResponse
             * 2. 需要据 URIConstans.PROXY_REQUEST_ID 取出当时的 Promise，再设置SimpleHttpResponse进去
             * </pre>
             */
            if (MethodConstants.HTTP_PROXY.equals(method)) {
                // 获取原始的请求ID（可能被URL编码）
                final String requestIdRaw = parameters.getFirst(URIConstans.PROXY_REQUEST_ID);
                final String requestId;
                if (requestIdRaw != null) {
                    // URL解码请求ID
                    requestId = URLDecoder.decode(requestIdRaw, "utf-8");
                } else {
                    requestId = null;
                }
                // 校验请求ID是否存在
                if (requestId == null) {
                    logger.error("error, need {}, text: {}", URIConstans.PROXY_REQUEST_ID, text);
                    return;
                }
                logger.info("received http proxy response, requestId: {}", requestId);

                // 从Tunnel服务器中查找对应的Promise对象
                Promise<SimpleHttpResponse> promise = tunnelServer.findProxyRequestPromise(requestId);

                // 获取原始的响应数据（可能被URL编码和Base64编码）
                final String dataRaw = parameters.getFirst(URIConstans.PROXY_RESPONSE_DATA);
                final String data;
                if (dataRaw != null) {
                    // URL解码响应数据
                    data = URLDecoder.decode(dataRaw, "utf-8");
                    // Base64解码响应数据
                    byte[] bytes = Base64.decodeBase64(data);

                    // 将字节数组转换为SimpleHttpResponse对象
                    SimpleHttpResponse simpleHttpResponse = SimpleHttpResponse.fromBytes(bytes);
                    // 设置Promise的成功结果
                    promise.setSuccess(simpleHttpResponse);
                } else {
                    data = null;
                    // 设置Promise的失败结果
                    promise.setFailure(new Exception(URIConstans.PROXY_RESPONSE_DATA + " is null! reuqestId: " + requestId));
                }
            }
        }
    }

    /**
     * 连接到Arthas Agent
     * <p>
     * 该方法处理来自浏览器的连接请求，建立浏览器到Arthas Agent的双向通信隧道。
     * 主要流程：
     * 1. 解析Agent ID
     * 2. 查找已注册的Agent
     * 3. 生成客户端连接ID
     * 4. 创建Promise用于等待Agent打开隧道
     * 5. 向Agent发送打开隧道请求
     * 6. 等待Agent响应（最多20秒）
     * 7. 建立双向转发处理器
     * </p>
     *
     * @param tunnelSocketCtx WebSocket通道上下文（浏览器端）
     * @param parameters URI查询参数
     * @throws URISyntaxException 如果URI语法错误
     */
    private void connectArthas(ChannelHandlerContext tunnelSocketCtx, MultiValueMap<String, String> parameters)
            throws URISyntaxException {

        // 从参数中获取Agent ID列表，如果不存在则返回空列表
        List<String> agentId = parameters.getOrDefault("id", Collections.emptyList());

        // 校验Agent ID是否为空
        if (agentId.isEmpty()) {
            logger.error("arthas agent id can not be null, parameters: {}", parameters);
            throw new IllegalArgumentException("arthas agent id can not be null");
        }

        logger.info("try to connect to arthas agent, id: " + agentId.get(0));

        // 在Tunnel服务器中查找指定的Agent
        Optional<AgentInfo> findAgent = tunnelServer.findAgent(agentId.get(0));

        // 如果找到了Agent
        if (findAgent.isPresent()) {
            // 获取Agent的通道上下文
            ChannelHandlerContext agentCtx = findAgent.get().getChannelHandlerContext();

            // 生成随机的客户端连接ID（20位字母数字组合，大写）
            String clientConnectionId = RandomStringUtils.random(20, true, true).toUpperCase();

            logger.info("random clientConnectionId: " + clientConnectionId);
            // 构建打开隧道的URI，包含方法类型、Agent ID和客户端连接ID
            // URI uri = new URI("response", null, "/",
            //        "method=" + MethodConstants.START_TUNNEL + "&id=" + agentId.get(0) + "&clientConnectionId=" + clientConnectionId, null);
            URI uri = UriComponentsBuilder.newInstance().scheme(URIConstans.RESPONSE).path("/")
                    .queryParam(URIConstans.METHOD, MethodConstants.START_TUNNEL).queryParam(URIConstans.ID, agentId)
                    .queryParam(URIConstans.CLIENT_CONNECTION_ID, clientConnectionId).build().toUri();

            logger.info("startTunnel response: " + uri);

            // 创建客户端连接信息对象
            ClientConnectionInfo clientConnectionInfo = new ClientConnectionInfo();
            // 获取远程地址（浏览器端的地址）
            SocketAddress remoteAddress = tunnelSocketCtx.channel().remoteAddress();
            if (remoteAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
                // 设置客户端主机地址
                clientConnectionInfo.setHost(inetSocketAddress.getHostString());
                // 设置客户端端口
                clientConnectionInfo.setPort(inetSocketAddress.getPort());
            }
            // 设置客户端通道上下文
            clientConnectionInfo.setChannelHandlerContext(tunnelSocketCtx);

            // 创建Promise对象，用于等待Agent打开隧道成功，之后会设置结果到Promise中
            Promise<Channel> promise = GlobalEventExecutor.INSTANCE.newPromise();
            // 添加Promise监听器，当Agent打开隧道成功时触发
            promise.addListener(new FutureListener<Channel>() {
                @Override
                public void operationComplete(final Future<Channel> future) throws Exception {
                    // 获取Agent侧的通道
                    final Channel outboundChannel = future.getNow();
                    if (future.isSuccess()) {
                        // 移除当前处理器
                        tunnelSocketCtx.pipeline().remove(TunnelSocketFrameHandler.this);

                        // outboundChannel is form arthas agent
                        // 移除Agent通道的最后一个处理器
                        outboundChannel.pipeline().removeLast();

                        // 在Agent通道上添加转发处理器，转发到浏览器
                        outboundChannel.pipeline().addLast(new RelayHandler(tunnelSocketCtx.channel()));
                        // 在浏览器通道上添加转发处理器，转发到Agent
                        tunnelSocketCtx.pipeline().addLast(new RelayHandler(outboundChannel));
                    } else {
                        // 等待Agent连接失败，记录错误日志并关闭Agent通道
                        logger.error("wait for agent connect error. agentId: {}, clientConnectionId: {}", agentId,
                                clientConnectionId);
                        ChannelUtils.closeOnFlush(agentCtx.channel());
                    }
                }
            });

            // 设置Promise到客户端连接信息中
            clientConnectionInfo.setPromise(promise);
            // 将客户端连接信息添加到Tunnel服务器中
            this.tunnelServer.addClientConnectionInfo(clientConnectionId, clientConnectionInfo);
            // 添加关闭监听器，当浏览器连接关闭时，从Tunnel服务器中移除客户端连接信息
            tunnelSocketCtx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    tunnelServer.removeClientConnectionInfo(clientConnectionId);
                }
            });

            // 向Agent发送打开隧道的请求
            agentCtx.channel().writeAndFlush(new TextWebSocketFrame(uri.toString()));

            logger.info("browser connect waitting for arthas agent open tunnel");
            // 等待Agent打开隧道，最多等待20秒
            boolean watiResult = promise.awaitUninterruptibly(20, TimeUnit.SECONDS);
            if (watiResult) {
                // 等待成功，记录日志
                logger.info(
                        "browser connect wait for arthas agent open tunnel success, agentId: {}, clientConnectionId: {}",
                        agentId, clientConnectionId);
            } else {
                // 等待超时，记录错误日志并关闭浏览器连接
                logger.error(
                        "browser connect wait for arthas agent open tunnel timeout, agentId: {}, clientConnectionId: {}",
                        agentId, clientConnectionId);
                tunnelSocketCtx.close();
            }
        } else {
            // 未找到Agent，发送关闭帧并抛出异常
            tunnelSocketCtx.channel().writeAndFlush(new CloseWebSocketFrame(2000, "Can not find arthas agent by id: "+ agentId));
            logger.error("Can not find arthas agent by id: {}", agentId);
            throw new IllegalArgumentException("Can not find arthas agent by id: " + agentId);
        }
    }

    /**
     * 处理Agent注册请求
     * <p>
     * 该方法处理来自Arthas Agent的注册请求，为Agent分配唯一ID并保存连接信息。
     * 主要流程：
     * 1. 解析请求参数（应用名称、Agent ID、Arthas版本等）
     * 2. 生成或使用传入的Agent ID
     * 3. 提取客户端真实IP地址（考虑nginx代理情况）
     * 4. 保存Agent信息到Tunnel服务器
     * 5. 返回注册成功响应
     * </p>
     *
     * @param ctx 通道处理器上下文
     * @param handshake WebSocket握手完成事件
     * @param requestUri 请求URI
     * @throws URISyntaxException 如果URI语法错误
     */
    private void agentRegister(ChannelHandlerContext ctx, HandshakeComplete handshake, String requestUri) throws URISyntaxException {
        // 解析查询字符串参数
        QueryStringDecoder queryDecoder = new QueryStringDecoder(requestUri);
        Map<String, List<String>> parameters = queryDecoder.parameters();

        // 解析应用名称
        String appName = null;
        List<String> appNameList = parameters.get(URIConstans.APP_NAME);
        if (appNameList != null && !appNameList.isEmpty()) {
            appName = appNameList.get(0);
        }

        // generate a random agent id
        // 生成随机的Agent ID
        String id = null;
        if (appName != null) {
            // 如果有传 app name，则生成带 app name前缀的id，方便管理
            id = appName + "_" + RandomStringUtils.random(20, true, true).toUpperCase();
        } else {
            id = RandomStringUtils.random(20, true, true).toUpperCase();
        }
        // agent传过来，则优先用 agent的
        List<String> idList = parameters.get(URIConstans.ID);
        if (idList != null && !idList.isEmpty()) {
            id = idList.get(0);
        }

        // 解析Arthas版本
        String arthasVersion = null;
        List<String> arthasVersionList = parameters.get(URIConstans.ARTHAS_VERSION);
        if (arthasVersionList != null && !arthasVersionList.isEmpty()) {
            arthasVersion = arthasVersionList.get(0);
        }

        final String finalId = id;

        // 构建注册成功的响应URI
        // URI responseUri = new URI("response", null, "/", "method=" + MethodConstants.AGENT_REGISTER + "&id=" + id, null);
        URI responseUri = UriComponentsBuilder.newInstance().scheme(URIConstans.RESPONSE).path("/")
                .queryParam(URIConstans.METHOD, MethodConstants.AGENT_REGISTER).queryParam(URIConstans.ID, id).build()
                .encode().toUri();

        // 创建Agent信息对象
        AgentInfo info = new AgentInfo();

        // 前面可能有nginx代理
        // 从请求头中获取客户端IP地址
        HttpHeaders headers = handshake.requestHeaders();
        String host = HttpUtils.findClientIP(headers);

        if (host == null) {
            // 如果请求头中没有找到IP地址，则从远程地址中获取
            SocketAddress remoteAddress = ctx.channel().remoteAddress();
            if (remoteAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
                // 设置Agent主机地址
                info.setHost(inetSocketAddress.getHostString());
                // 设置Agent端口
                info.setPort(inetSocketAddress.getPort());
            }
        } else {
            // 使用从请求头中提取的IP地址
            info.setHost(host);
            // 尝试从请求头中提取端口
            Integer port = HttpUtils.findClientPort(headers);
            if (port != null) {
                info.setPort(port);
            }
        }

        // 设置通道上下文
        info.setChannelHandlerContext(ctx);
        // 设置Arthas版本（如果存在）
        if (arthasVersion != null) {
            info.setArthasVersion(arthasVersion);
        }

        // 将Agent信息添加到Tunnel服务器中
        tunnelServer.addAgent(id, info);
        // 添加关闭监听器，当Agent连接关闭时，从Tunnel服务器中移除Agent信息
        ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                tunnelServer.removeAgent(finalId);
            }

        });

        // 向Agent发送注册成功的响应
        ctx.channel().writeAndFlush(new TextWebSocketFrame(responseUri.toString()));
    }

    /**
     * 打开到客户端的隧道
     * <p>
     * 该方法处理来自Arthas Agent的打开隧道请求，将Agent的通道设置到对应的Promise中，
     * 从而触发浏览器端的连接建立流程。
     * </p>
     *
     * @param ctx 通道处理器上下文（Agent端）
     * @param clientConnectionId 客户端连接ID
     */
    private void openTunnel(ChannelHandlerContext ctx, String clientConnectionId) {
        // 在Tunnel服务器中查找对应的客户端连接信息
        Optional<ClientConnectionInfo> infoOptional = this.tunnelServer.findClientConnection(clientConnectionId);

        // 如果找到了对应的客户端连接信息
        if (infoOptional.isPresent()) {
            ClientConnectionInfo info = infoOptional.get();
            logger.info("openTunnel clientConnectionId:" + clientConnectionId);

            // 获取Promise对象
            Promise<Channel> promise = info.getPromise();
            // 设置Promise的成功结果，传入Agent的通道
            promise.setSuccess(ctx.channel());
        } else {
            // 未找到对应的客户端连接信息，记录错误日志
            logger.error("Can not find client connection by id: {}", clientConnectionId);
        }

    }
}

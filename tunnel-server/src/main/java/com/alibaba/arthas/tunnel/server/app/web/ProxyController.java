package com.alibaba.arthas.tunnel.server.app.web;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.alibaba.arthas.tunnel.common.URIConstans;
import com.alibaba.arthas.tunnel.server.AgentInfo;
import com.alibaba.arthas.tunnel.server.TunnelServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * HTTP 代理控制器
 * <p>
 * 该控制器负责将接收到的 HTTP 请求代理转发到具体的 Arthas Agent。
 * 主要用于通过 Tunnel Server 访问已连接的 Arthas Agent 提供的 HTTP 接口。
 * </p>
 * <p>
 * 工作流程：
 * 1. 接收客户端的 HTTP 请求
 * 2. 根据 Agent ID 找到对应的 Agent 连接
 * 3. 将请求通过 WebSocket 连接转发给 Agent
 * 4. 等待 Agent 处理并返回响应
 * 5. 将 Agent 的响应返回给客户端
 * </p>
 *
 * @author hengyunabc 2020-10-22
 *
 */
@Controller
public class ProxyController {
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(ProxyController.class);

    /**
     * Tunnel 服务器实例
     * 管理所有 Agent 的 WebSocket 连接
     */
    @Autowired
    TunnelServer tunnelServer;

    /**
     * 执行 HTTP 代理请求
     * <p>
     * 该方法接收客户端的 HTTP 请求，将其转发给指定的 Arthas Agent，
     * 并将 Agent 的响应返回给客户端。
     * </p>
     * <p>
     * URL 格式：/proxy/{agentId}/{targetUrl}
     * 例如：/proxy/abc123/api/jad
     * </p>
     *
     * @param agentId 目标 Agent 的唯一标识（必填）
     * @param request HTTP 请求对象
     * @return HTTP 响应实体，包含响应状态码、响应头和响应体
     * @throws InterruptedException 如果等待响应时被中断
     * @throws ExecutionException 如果执行过程中发生错误
     * @throws TimeoutException 如果等待响应超时
     */
    @RequestMapping(value = "/proxy/{agentId}/**")
    @ResponseBody
    public ResponseEntity<?> execute(@PathVariable(name = "agentId", required = true) String agentId,
            HttpServletRequest request) throws InterruptedException, ExecutionException, TimeoutException {

        // 获取完整的请求路径
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        // 从完整路径中提取目标 URL
        // 去掉 "/proxy/" 前缀和 agentId 部分
        String targetUrl = fullPath.substring("/proxy/".length() + agentId.length());

        // 记录代理请求日志
        logger.info("http proxy, agentId: {}, targetUrl: {}", agentId, targetUrl);

        // 根据 Agent ID 查找对应的 Agent 连接信息
        Optional<AgentInfo> findAgent = tunnelServer.findAgent(agentId);

        // 如果找到了对应的 Agent
        if (findAgent.isPresent()) {
            // 生成一个随机的请求 ID，用于匹配请求和响应
            // 使用 20 位随机字母数字字符，并转为大写
            String requestId = RandomStringUtils.random(20, true, true).toUpperCase();

            // 获取 Agent 的 WebSocket 通道上下文
            ChannelHandlerContext agentCtx = findAgent.get().getChannelHandlerContext();

            // 创建一个 Promise 对象，用于异步获取 Agent 的响应
            // 使用 GlobalEventExecutor 作为线程执行器
            Promise<SimpleHttpResponse> httpResponsePromise = GlobalEventExecutor.INSTANCE.newPromise();

            // 将 Promise 注册到 TunnelServer 中，以便接收 Agent 的响应
            tunnelServer.addProxyRequestPromise(requestId, httpResponsePromise);

            // 构造 WebSocket 消息的 URI
            // URI 中包含请求方法、Agent ID、目标 URL 和请求 ID
            URI uri = UriComponentsBuilder.newInstance().scheme(URIConstans.RESPONSE).path("/")
                    .queryParam(URIConstans.METHOD, MethodConstants.HTTP_PROXY).queryParam(URIConstans.ID, agentId)
                    .queryParam(URIConstans.TARGET_URL, targetUrl).queryParam(URIConstans.PROXY_REQUEST_ID, requestId)
                    .build().toUri();

            // 通过 WebSocket 将请求发送给 Agent
            agentCtx.channel().writeAndFlush(new TextWebSocketFrame(uri.toString()));

            // 记录等待响应的日志
            logger.info("waitting for arthas agent http proxy, agentId: {}, targetUrl: {}", agentId, targetUrl);

            // 等待 Agent 返回响应，最多等待 15 秒
            SimpleHttpResponse simpleHttpResponse = httpResponsePromise.get(15, TimeUnit.SECONDS);

            // 构造响应实体，设置响应状态码
            BodyBuilder bodyBuilder = ResponseEntity.status(simpleHttpResponse.getStatus());

            // 复制 Agent 返回的所有响应头
            for (Entry<String, String> entry : simpleHttpResponse.getHeaders().entrySet()) {
                bodyBuilder.header(entry.getKey(), entry.getValue());
            }

            // 设置响应体并返回
            ResponseEntity<byte[]> responseEntity = bodyBuilder.body(simpleHttpResponse.getContent());
            return responseEntity;
        } else {
            // 如果找不到对应的 Agent，记录错误日志
            logger.error("can not find agent by agentId: {}", agentId);
        }

        // 如果找不到 Agent 或发生错误，返回 404 响应
        return ResponseEntity.notFound().build();
    }
}

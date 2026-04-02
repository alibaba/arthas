package com.taobao.arthas.core.mcp.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP（Model Context Protocol）认证信息提取工具
 * <p>
 * 该工具类用于从不同的上下文中提取认证相关的信息，包括：
 * <ul>
 * <li>从Netty的ChannelHandlerContext中提取认证主体(Subject)</li>
 * <li>从HTTP请求头中提取用户ID</li>
 * </ul>
 * </p>
 * <p>
 * 主要用于Arthas的MCP服务端实现，处理来自客户端的认证请求。
 * </p>
 *
 * @author Yeaury
 */
public class McpAuthExtractor {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(McpAuthExtractor.class);

    /**
     * MCP认证主体在配置中的键名
     * <p>用于在MCP传输上下文中存储和获取认证主体</p>
     */
    public static final String MCP_AUTH_SUBJECT_KEY = "mcp.auth.subject";

    /**
     * User ID 在 McpTransportContext 中的 key
     * <p>用于在MCP传输上下文中存储和获取用户ID</p>
     */
    public static final String MCP_USER_ID_KEY = "mcp.user.id";

    /**
     * 从 HTTP Header 中提取 User ID 的 header 名称
     * <p>客户端应使用此header名称传递用户ID</p>
     */
    public static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Netty Channel属性键，用于存储认证主体
     * <p>将认证主体存储在Channel的属性中，便于在整个连接生命周期中访问</p>
     */
    public static final AttributeKey<Object> SUBJECT_ATTRIBUTE_KEY =
            AttributeKey.valueOf("arthas.auth.subject");

    /**
     * 从ChannelHandlerContext中提取认证主体
     * <p>
     * 该方法从Netty的Channel上下文中提取已认证的主体对象。
     * 认证主体通常在用户通过身份验证后被存储在Channel的属性中。
     * </p>
     *
     * @param ctx Netty的ChannelHandlerContext对象，包含Channel和其属性
     * @return 认证主体对象，如果未认证或提取失败则返回null
     */
    public static Object extractAuthSubjectFromContext(ChannelHandlerContext ctx) {
        // 参数校验：确保context和channel不为空
        if (ctx == null || ctx.channel() == null) {
            return null;
        }

        try {
            // 从Channel的属性中获取认证主体
            Object subject = ctx.channel().attr(SUBJECT_ATTRIBUTE_KEY).get();
            if (subject != null) {
                // 记录调试信息：成功提取认证主体
                logger.debug("Extracted auth subject from channel context: {}", subject.getClass().getSimpleName());
                return subject;
            }
        } catch (Exception e) {
            // 记录调试信息：提取失败
            logger.debug("Failed to extract auth subject from context: {}", e.getMessage());
        }

        // 未找到认证主体，返回null
        return null;
    }

    /**
     * 从 HTTP 请求中提取 User ID
     * <p>
     * 该方法从HTTP请求的头部中提取用户ID。
     * 客户端应该在请求头中包含"X-User-Id"字段来传递用户ID。
     * </p>
     *
     * @param request HTTP请求对象，包含请求头等信息
     * @return 用户ID字符串，如果不存在或为空则返回null
     */
    public static String extractUserIdFromRequest(FullHttpRequest request) {
        // 参数校验：确保request不为空
        if (request == null) {
            return null;
        }

        // 从请求头中获取用户ID
        String userId = request.headers().get(USER_ID_HEADER);
        // 验证用户ID不为空
        if (userId != null && !userId.trim().isEmpty()) {
            // 记录调试信息：成功提取用户ID
            logger.debug("Extracted userId from HTTP header {}: {}", USER_ID_HEADER, userId);
            // 返回去除首尾空格的用户ID
            return userId.trim();
        }

        // 未找到用户ID，返回null
        return null;
    }

}

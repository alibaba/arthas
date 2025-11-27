package com.taobao.arthas.mcp.server.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP认证信息提取工具
 *
 * @author Yeaury
 */
public class McpAuthExtractor {

    private static final Logger logger = LoggerFactory.getLogger(McpAuthExtractor.class);

    public static final String MCP_AUTH_SUBJECT_KEY = "mcp.auth.subject";

    /**
     * User ID 在 McpTransportContext 中的 key
     */
    public static final String MCP_USER_ID_KEY = "mcp.user.id";

    /**
     * 从 HTTP Header 中提取 User ID 的 header 名称
     */
    public static final String USER_ID_HEADER = "X-User-Id";

    public static final AttributeKey<Object> SUBJECT_ATTRIBUTE_KEY =
            AttributeKey.valueOf("arthas.auth.subject");

    /**
     * 从ChannelHandlerContext中提取认证主体
     *
     * @param ctx Netty ChannelHandlerContext
     * @return 认证主体对象，如果未认证则返回null
     */
    public static Object extractAuthSubjectFromContext(ChannelHandlerContext ctx) {
        if (ctx == null || ctx.channel() == null) {
            return null;
        }

        try {
            Object subject = ctx.channel().attr(SUBJECT_ATTRIBUTE_KEY).get();
            if (subject != null) {
                logger.debug("Extracted auth subject from channel context: {}", subject.getClass().getSimpleName());
                return subject;
            }
        } catch (Exception e) {
            logger.debug("Failed to extract auth subject from context: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从 HTTP 请求中提取 User ID
     *
     * @param request HTTP 请求
     * @return User ID，如果不存在则返回 null
     */
    public static String extractUserIdFromRequest(FullHttpRequest request) {
        if (request == null) {
            return null;
        }

        String userId = request.headers().get(USER_ID_HEADER);
        if (userId != null && !userId.trim().isEmpty()) {
            logger.debug("Extracted userId from HTTP header {}: {}", USER_ID_HEADER, userId);
            return userId.trim();
        }

        return null;
    }

}

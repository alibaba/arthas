package com.taobao.arthas.mcp.server.util;

import io.netty.channel.ChannelHandlerContext;
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

}

package com.taobao.arthas.mcp.server.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting authentication information from Netty context and HTTP headers.
 */
public class McpAuthExtractor {
    private static final Logger logger = LoggerFactory.getLogger(McpAuthExtractor.class);

    /**
     * String key for MCP transport context
     */
    public static final String MCP_AUTH_SUBJECT_KEY = "mcp.auth.subject";
    public static final String MCP_USER_ID_KEY = "mcp.user.id";
    public static final String USER_ID_HEADER = "X-User-Id";
    
    /**
     * AttributeKey for Netty channel
     */
    public static final AttributeKey<Object> CHANNEL_AUTH_SUBJECT_KEY = AttributeKey.valueOf("mcp.auth.subject");
    public static final AttributeKey<String> CHANNEL_USER_ID_KEY = AttributeKey.valueOf("mcp.user.id");
    public static final AttributeKey<Object> SUBJECT_ATTRIBUTE_KEY =
            AttributeKey.valueOf("arthas.auth.subject");

    /**
     * Extract auth subject from ChannelHandlerContext
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
     * Extract user ID from HTTP request headers
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

    /**
     * Extract user ID from channel attributes
     */
    public static String extractUserId(Channel channel) {
        if (channel == null) {
            return null;
        }
        return channel.attr(CHANNEL_USER_ID_KEY).get();
    }

    /**
     * Set user ID to channel attributes
     */
    public static void setUserId(Channel channel, String userId) {
        if (channel != null && userId != null) {
            channel.attr(CHANNEL_USER_ID_KEY).set(userId);
        }
    }

    /**
     * Extract auth subject from channel attributes
     */
    public static Object extractAuthSubject(Channel channel) {
        if (channel == null) {
            return null;
        }
        return channel.attr(CHANNEL_AUTH_SUBJECT_KEY).get();
    }

    /**
     * Set auth subject to channel attributes
     */
    public static void setAuthSubject(Channel channel, Object subject) {
        if (channel != null && subject != null) {
            channel.attr(CHANNEL_AUTH_SUBJECT_KEY).set(subject);
        }
    }
}

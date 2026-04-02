package com.taobao.arthas.mcp.server.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 从 Netty Channel 上下文和 HTTP 请求头中提取认证信息的工具类。
 * <p>
 * MCP Server 基于 Netty 实现，认证信息（如认证主体、用户 ID）需要在不同的处理阶段传递：
 * <ul>
 *   <li>认证过滤器在 HTTP 请求到达时提取认证主体，写入 Channel 属性（Attribute）</li>
 *   <li>后续的业务处理器从 Channel 属性中读取认证主体，用于鉴权或统计上报</li>
 * </ul>
 * <p>
 * 本类提供统一的存取接口，屏蔽了 Netty {@link AttributeKey} 的操作细节，
 * 同时集中定义了所有认证相关的键名常量，避免散落在各处导致不一致。
 * <p>
 * 本类为工具类，不可实例化，所有方法和常量均为静态。
 */
public class McpAuthExtractor {
    private static final Logger logger = LoggerFactory.getLogger(McpAuthExtractor.class);

    /**
     * MCP 传输上下文中认证主体的字符串键名。
     * <p>
     * 用于在 MCP 传输层上下文（非 Netty Channel）中以字符串 key 存取认证主体，
     * 例如在 Map 形式的上下文对象中传递。
     */
    public static final String MCP_AUTH_SUBJECT_KEY = "mcp.auth.subject";

    /**
     * MCP 传输上下文中用户 ID 的字符串键名。
     * <p>
     * 用于在 MCP 传输层上下文（非 Netty Channel）中以字符串 key 存取用户 ID。
     */
    public static final String MCP_USER_ID_KEY = "mcp.user.id";

    /**
     * HTTP 请求头中用户 ID 的头部字段名。
     * <p>
     * 客户端在发起 MCP 请求时，可通过此 HTTP 请求头携带用户 ID，
     * 供服务端提取后用于统计上报或审计日志。
     */
    public static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Netty Channel 属性中认证主体的 {@link AttributeKey}。
     * <p>
     * 认证过滤器在认证成功后，通过此 Key 将认证主体对象写入 Netty Channel 属性，
     * 后续所有处理器均可通过此 Key 从 Channel 中读取认证主体。
     * Key 字符串值与 {@link #MCP_AUTH_SUBJECT_KEY} 保持一致。
     */
    public static final AttributeKey<Object> CHANNEL_AUTH_SUBJECT_KEY = AttributeKey.valueOf("mcp.auth.subject");

    /**
     * Netty Channel 属性中用户 ID 的 {@link AttributeKey}。
     * <p>
     * 用于在 Netty Channel 属性中存取用户 ID 字符串。
     * Key 字符串值与 {@link #MCP_USER_ID_KEY} 保持一致。
     */
    public static final AttributeKey<String> CHANNEL_USER_ID_KEY = AttributeKey.valueOf("mcp.user.id");

    /**
     * Netty Channel 属性中 Arthas 认证主体的 {@link AttributeKey}。
     * <p>
     * 专用于存取 Arthas 侧的认证主体（可能与 MCP 层的认证主体不同），
     * 在将认证信息传递给 Arthas Agent 执行鉴权时使用。
     */
    public static final AttributeKey<Object> SUBJECT_ATTRIBUTE_KEY =
            AttributeKey.valueOf("arthas.auth.subject");

    /**
     * 从 Netty {@link ChannelHandlerContext} 中提取 Arthas 认证主体。
     * <p>
     * 通过 {@link #SUBJECT_ATTRIBUTE_KEY} 从当前 Channel 属性中读取认证主体对象。
     * 若 {@code ctx} 或其关联的 Channel 为 {@code null}，或读取过程中发生异常，则返回 {@code null}。
     * <p>
     * 认证主体通常在 HTTP 请求的认证阶段由认证过滤器写入 Channel，
     * 后续命令执行时通过本方法读取，用于向 Arthas Agent 传递鉴权信息。
     *
     * @param ctx Netty 通道处理器上下文，可以为 {@code null}
     * @return 认证主体对象，若不存在或获取失败则返回 {@code null}
     */
    public static Object extractAuthSubjectFromContext(ChannelHandlerContext ctx) {
        if (ctx == null || ctx.channel() == null) {
            return null;
        }

        try {
            // 从 Netty Channel 属性中读取 Arthas 认证主体
            Object subject = ctx.channel().attr(SUBJECT_ATTRIBUTE_KEY).get();
            if (subject != null) {
                logger.debug("Extracted auth subject from channel context: {}", subject.getClass().getSimpleName());
                return subject;
            }
        } catch (Exception e) {
            // 读取失败时仅记录 debug 日志，不抛出异常，避免影响正常请求处理
            logger.debug("Failed to extract auth subject from context: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从 HTTP 请求头中提取用户 ID。
     * <p>
     * 读取请求头中 {@link #USER_ID_HEADER}（{@code X-User-Id}）字段的值，
     * 对值进行首尾空白字符裁剪后返回。
     * 若请求为 {@code null}、请求头中不存在该字段、或字段值为空白字符串，则返回 {@code null}。
     * <p>
     * 提取到的用户 ID 通常用于统计上报，关联诊断操作与发起用户。
     *
     * @param request Netty HTTP 完整请求对象，可以为 {@code null}
     * @return 用户 ID 字符串（已去除首尾空白），若不存在则返回 {@code null}
     */
    public static String extractUserIdFromRequest(FullHttpRequest request) {
        if (request == null) {
            return null;
        }

        // 从 HTTP 请求头中读取 X-User-Id 字段
        String userId = request.headers().get(USER_ID_HEADER);
        if (userId != null && !userId.trim().isEmpty()) {
            logger.debug("Extracted userId from HTTP header {}: {}", USER_ID_HEADER, userId);
            // 裁剪首尾空白后返回，避免空格导致后续比较或查询失败
            return userId.trim();
        }

        return null;
    }

    /**
     * 从 Netty Channel 属性中读取用户 ID。
     * <p>
     * 通过 {@link #CHANNEL_USER_ID_KEY} 读取预先存储在 Channel 属性中的用户 ID。
     * 用于在同一个 Channel 的不同处理阶段共享用户 ID，避免重复解析 HTTP 头。
     *
     * @param channel Netty Channel，可以为 {@code null}
     * @return 存储在 Channel 属性中的用户 ID，若 Channel 为 {@code null} 或未设置则返回 {@code null}
     */
    public static String extractUserId(Channel channel) {
        if (channel == null) {
            return null;
        }
        return channel.attr(CHANNEL_USER_ID_KEY).get();
    }

    /**
     * 将用户 ID 写入 Netty Channel 属性。
     * <p>
     * 通过 {@link #CHANNEL_USER_ID_KEY} 将用户 ID 存储到 Channel 属性中，
     * 使得同一 Channel 生命周期内的后续处理器可以通过 {@link #extractUserId(Channel)} 读取。
     * 若 {@code channel} 或 {@code userId} 为 {@code null}，则跳过写入（no-op）。
     *
     * @param channel Netty Channel，为 {@code null} 时跳过
     * @param userId  用户 ID，为 {@code null} 时跳过
     */
    public static void setUserId(Channel channel, String userId) {
        if (channel != null && userId != null) {
            channel.attr(CHANNEL_USER_ID_KEY).set(userId);
        }
    }

    /**
     * 从 Netty Channel 属性中读取认证主体。
     * <p>
     * 通过 {@link #CHANNEL_AUTH_SUBJECT_KEY} 读取预先由认证过滤器写入的认证主体对象。
     * 认证主体通常在 HTTP 请求经过认证阶段后由 {@link #setAuthSubject(Channel, Object)} 写入，
     * 后续命令执行时通过本方法读取，用于鉴权和权限校验。
     *
     * @param channel Netty Channel，可以为 {@code null}
     * @return 存储在 Channel 属性中的认证主体对象，若 Channel 为 {@code null} 或未设置则返回 {@code null}
     */
    public static Object extractAuthSubject(Channel channel) {
        if (channel == null) {
            return null;
        }
        return channel.attr(CHANNEL_AUTH_SUBJECT_KEY).get();
    }

    /**
     * 将认证主体写入 Netty Channel 属性。
     * <p>
     * 通过 {@link #CHANNEL_AUTH_SUBJECT_KEY} 将认证主体对象存储到 Channel 属性中，
     * 使得同一 Channel 生命周期内的后续处理器可以通过 {@link #extractAuthSubject(Channel)} 读取。
     * 通常由认证过滤器在认证成功后调用，将认证上下文与 Channel 绑定。
     * 若 {@code channel} 或 {@code subject} 为 {@code null}，则跳过写入（no-op）。
     *
     * @param channel Netty Channel，为 {@code null} 时跳过
     * @param subject 认证主体对象，为 {@code null} 时跳过
     */
    public static void setAuthSubject(Channel channel, Object subject) {
        if (channel != null && subject != null) {
            channel.attr(CHANNEL_AUTH_SUBJECT_KEY).set(subject);
        }
    }
}

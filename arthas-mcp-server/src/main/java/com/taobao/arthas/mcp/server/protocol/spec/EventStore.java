package com.taobao.arthas.mcp.server.protocol.spec;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 事件存储接口，用于存储和重放JSON-RPC事件
 * 支持事件持久化和流恢复功能，用于MCP可流式HTTP协议
 *
 * <p>该接口定义了事件存储的核心操作，包括：
 * <ul>
 *   <li>存储JSON-RPC消息事件</li>
 *   <li>按会话和事件ID检索事件流</li>
 *   <li>清理过期事件</li>
 *   <li>移除特定会话的所有事件</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface EventStore {

    /**
     * 已存储事件的内部表示类
     * 封装了事件的所有相关信息，用于持久化和检索
     */
    class StoredEvent {
        // 事件唯一标识符，用于区分不同的存储事件
        private final String eventId;

        // 会话标识符，标识事件所属的MCP会话
        private final String sessionId;

        // JSON-RPC消息对象，包含实际的协议消息内容
        private final McpSchema.JSONRPCMessage message;

        // 事件时间戳，记录事件创建或存储的时间
        private final Instant timestamp;

        /**
         * 构造一个新的已存储事件对象
         *
         * @param eventId   事件唯一标识符
         * @param sessionId 会话标识符
         * @param message   JSON-RPC消息对象
         * @param timestamp 事件时间戳
         */
        public StoredEvent(String eventId, String sessionId, McpSchema.JSONRPCMessage message, Instant timestamp) {
            this.eventId = eventId;
            this.sessionId = sessionId;
            this.message = message;
            this.timestamp = timestamp;
        }

        /**
         * 获取事件唯一标识符
         *
         * @return 事件ID字符串
         */
        public String getEventId() {
            return eventId;
        }

        /**
         * 获取会话标识符
         *
         * @return 会话ID字符串
         */
        public String getSessionId() {
            return sessionId;
        }

        /**
         * 获取JSON-RPC消息对象
         *
         * @return JSON-RPC消息
         */
        public McpSchema.JSONRPCMessage getMessage() {
            return message;
        }

        /**
         * 获取事件时间戳
         *
         * @return 事件创建时间
         */
        public Instant getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 将JSON-RPC消息事件存储到事件存储中
     *
     * @param sessionId 会话标识符，用于将事件关联到特定会话
     * @param message   要存储的JSON-RPC消息对象
     * @return 生成的事件ID，可用于后续的事件检索和重放
     */
    String storeEvent(String sessionId, McpSchema.JSONRPCMessage message);

    /**
     * 获取指定会话的事件流，从指定事件ID之后开始
     * 用于实现断点续传和事件重放功能
     *
     * @param sessionId  会话标识符
     * @param fromEventId 起始事件ID，从此事件之后开始返回（不包含此事件），
     *                    如果为null则从会话的第一个事件开始
     * @return 事件流，按时间顺序排列的已存储事件
     */
    Stream<StoredEvent> getEventsForSession(String sessionId, String fromEventId);

    /**
     * 清理指定会话的过期事件
     * 用于防止事件存储无限增长，释放存储空间
     *
     * @param sessionId 会话标识符
     * @param maxAge    事件最大保留时长（毫秒），超过此时长的事件将被删除
     */
    void cleanupOldEvents(String sessionId, long maxAge);

    /**
     * 移除指定会话的所有事件
     * 通常在会话结束时调用，清理与该会话相关的所有数据
     *
     * @param sessionId 会话标识符
     */
    void removeSessionEvents(String sessionId);

}

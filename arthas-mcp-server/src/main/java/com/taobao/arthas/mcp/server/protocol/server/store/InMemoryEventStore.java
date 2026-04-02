package com.taobao.arthas.mcp.server.protocol.server.store;

import com.taobao.arthas.mcp.server.protocol.spec.EventStore;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 内存事件存储实现类
 * <p>
 * 该类实现了EventStore接口，提供了基于内存的事件存储功能。
 * 主要用于存储和管理MCP协议通信过程中的事件，支持按会话ID存储和检索事件。
 * </p>
 * <p>
 * 主要功能：
 * 1. 事件存储：将事件存储到内存中，每个会话有独立的事件列表
 * 2. 事件检索：根据会话ID和起始事件ID获取事件流
 * 3. 事件清理：支持清理过期事件和超出数量限制的事件
 * 4. 会话管理：移除指定会话的所有事件
 * </p>
 * <p>
 * 注意：这是内存实现，重启后数据会丢失。适用于临时存储和测试场景。
 * </p>
 *
 * @author Yeaury
 */
public class InMemoryEventStore implements EventStore {

	/**
	 * 日志记录器，用于记录事件存储操作的关键信息
	 */
	private static final Logger logger = LoggerFactory.getLogger(InMemoryEventStore.class);

	/**
	 * 全局事件ID计数器
	 * 使用AtomicLong确保在多线程环境下的线程安全性
	 * 每次存储新事件时自动递增，用于生成全局唯一的事件ID
	 */
	private final AtomicLong globalEventIdCounter = new AtomicLong(0);

	/**
	 * 会话事件存储映射表
	 * key: 会话ID（String）
	 * value: 该会话的事件列表（List<StoredEvent>）
	 * 使用ConcurrentHashMap确保多线程环境下的线程安全
	 */
	private final Map<String, List<StoredEvent>> sessionEvents = new ConcurrentHashMap<>();

	/**
	 * 事件ID到会话ID的映射表
	 * key: 事件ID（String）
	 * value: 会话ID（String）
	 * 用于快速查找某个事件属于哪个会话，提高查询效率
	 */
	private final Map<String, String> eventIdToSession = new ConcurrentHashMap<>();

	/**
	 * 每个会话最多保存的事件数量
	 * 超过此数量时，最老的事件将被移除，防止内存泄漏
	 */
	private final int maxEventsPerSession;

	/**
	 * 默认的事件保留时间（毫秒）
	 * 超过此时间的事件将被视为过期事件，可以被清理
	 * 默认值为24小时（24 * 60 * 60 * 1000毫秒）
	 */
	private final long defaultRetentionMs;

	/**
	 * 默认构造函数
	 * 使用默认参数初始化事件存储：
	 * - 每个会话最多保存1000个事件
	 * - 事件保留时间为24小时
	 */
	public InMemoryEventStore() {
		// 调用带参数的构造函数，设置默认值
		// 1000个事件，24小时保留时间
		this(1000, 24 * 60 * 60 * 1000L); // 1000 events, 24 hours
	}

	/**
	 * 带参数的构造函数
	 *
	 * @param maxEventsPerSession 每个会话最多保存的事件数量
	 * @param defaultRetentionMs 默认的事件保留时间（毫秒）
	 */
	public InMemoryEventStore(int maxEventsPerSession, long defaultRetentionMs) {
		// 保存每个会话的最大事件数量限制
		this.maxEventsPerSession = maxEventsPerSession;
		// 保存默认的事件保留时间
		this.defaultRetentionMs = defaultRetentionMs;
	}

	/**
	 * 存储事件到内存中
	 * <p>
	 * 该方法执行以下操作：
	 * 1. 生成全局唯一的事件ID
	 * 2. 创建StoredEvent对象并存储
	 * 3. 维护会话事件列表和事件ID映射
	 * 4. 检查并清理超出数量限制的事件
	 * </p>
	 *
	 * @param sessionId 会话ID，用于标识事件所属的会话
	 * @param message 要存储的MCP JSON-RPC消息对象
	 * @return 生成的事件ID，用于后续检索
	 */
	@Override
	public String storeEvent(String sessionId, McpSchema.JSONRPCMessage message) {
		// 生成全局唯一的事件ID
		// incrementAndGet是原子操作，确保ID的唯一性和线程安全
		String eventId = String.valueOf(globalEventIdCounter.incrementAndGet());
		// 记录当前时间戳，用于后续的过期判断
		Instant timestamp = Instant.now();

		// 创建存储事件对象，包含事件ID、会话ID、消息和时间戳
		StoredEvent event = new StoredEvent(eventId, sessionId, message, timestamp);

		// 将事件添加到对应会话的事件列表中
		// computeIfAbsent：如果会话不存在则创建新列表
		sessionEvents.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(event);
		// 维护事件ID到会话ID的映射，便于快速查找
		eventIdToSession.put(eventId, sessionId);

		// 检查是否需要清理旧事件
		// 如果事件数量超过最大限制，移除最老的事件
		List<StoredEvent> events = sessionEvents.get(sessionId);
		if (events.size() > maxEventsPerSession) {
			// 计算需要移除的事件数量
			int toRemove = events.size() - maxEventsPerSession;
			// 循环移除最老的事件（从列表开头移除）
			for (int i = 0; i < toRemove; i++) {
				// 移除列表中的第一个元素（最老的事件）
				StoredEvent removedEvent = events.remove(0);
				// 同时从事件ID映射中移除该事件
				eventIdToSession.remove(removedEvent.getEventId());
			}
			// 记录清理操作的调试信息
			logger.debug("Cleaned up {} old events for session {}", toRemove, sessionId);
		}

		// 记录事件存储的跟踪信息
		logger.trace("Stored event {} for session {}", eventId, sessionId);
		// 返回生成的事件ID
		return eventId;
	}

	/**
	 * 获取指定会话从指定事件ID开始的事件流
	 * <p>
	 * 该方法用于事件重放功能，从指定的事件ID开始获取后续所有事件。
	 * 注意：返回的事件会从存储中移除（实现at-least-once语义）。
	 * </p>
	 *
	 * @param sessionId 会话ID
	 * @param fromEventId 起始事件ID，从此事件开始返回（包含此事件）
	 * @return 事件流，包含从起始事件开始的所有事件
	 */
	@Override
	public Stream<StoredEvent> getEventsForSession(String sessionId, String fromEventId) {
		// 获取指定会话的事件列表
		List<StoredEvent> events = sessionEvents.get(sessionId);
		// 如果会话不存在或没有事件，返回空流
		if (events == null || events.isEmpty()) {
			return Stream.empty();
		}

		// 如果起始事件ID为空，返回空流
		// fromEventId是必需的参数，不能为null
		if (fromEventId == null) {
			return Stream.empty();
		}

		// 标记是否找到起始事件
		boolean foundStartEvent = false;
		// 创建结果列表存储要返回的事件
		List<StoredEvent> result = new ArrayList<>();

		// 遍历会话的所有事件
		for (StoredEvent event : events) {
			// 如果还没找到起始事件
			if (!foundStartEvent) {
				// 检查当前事件是否是起始事件
				if (event.getEventId().equals(fromEventId)) {
					// 找到起始事件，设置标志位
					foundStartEvent = true;
					// 将起始事件添加到结果列表
					result.add(event);
					// 从存储中移除已重放的事件（实现at-least-once语义）
					// 这样可以防止重复消费
					events.remove(event);
					eventIdToSession.remove(event.getEventId());
				}
				// 继续查找，直到找到起始事件
				continue;
			}
			// 已找到起始事件，将后续所有事件添加到结果列表
			result.add(event);
		}

		// 返回结果列表的流
		return result.stream();
	}

	/**
	 * 清理指定会话的过期事件
	 * <p>
	 * 该方法会移除超过指定时间限制的所有事件。
	 * 用于定期清理，防止内存占用过大。
	 * </p>
	 *
	 * @param sessionId 会话ID
	 * @param maxAge 最大保留时间（毫秒），超过此时间的事件将被删除
	 */
	@Override
	public void cleanupOldEvents(String sessionId, long maxAge) {
		// 获取指定会话的事件列表
		List<StoredEvent> events = sessionEvents.get(sessionId);
		// 如果会话不存在或没有事件，直接返回
		if (events == null || events.isEmpty()) {
			return;
		}

		// 计算截止时间点
		// 当前时间减去最大保留时间，得到过期的时间界限
		Instant cutoff = Instant.now().minusMillis(maxAge);

		// 找出所有需要移除的过期事件
		// filter：筛选出时间戳早于截止时间的事件
		// collect：将筛选结果收集到列表中
		List<StoredEvent> toRemove = events.stream()
			.filter(event -> event.getTimestamp().isBefore(cutoff))
			.collect(Collectors.toList());

		// 从事件列表和映射中移除过期事件
		for (StoredEvent event : toRemove) {
			// 从会话事件列表中移除
			events.remove(event);
			// 从事件ID映射中移除
			eventIdToSession.remove(event.getEventId());
		}

		// 如果有事件被清理，记录调试信息
		if (!toRemove.isEmpty()) {
			logger.debug("Cleaned up {} old events for session {}", toRemove.size(), sessionId);
		}
	}

	/**
	 * 移除指定会话的所有事件
	 * <p>
	 * 该方法会清理指定会话的所有事件和相关的映射关系。
	 * 通常在会话结束时调用。
	 * </p>
	 *
	 * @param sessionId 要清理的会话ID
	 */
	@Override
	public void removeSessionEvents(String sessionId) {
		// 从会话事件映射中移除整个会话
		// remove方法返回被移除的值（事件列表）
		List<StoredEvent> events = sessionEvents.remove(sessionId);
		// 如果会话存在且有事件
		if (events != null) {
			// 遍历所有事件，从事件ID映射中移除
			for (StoredEvent event : events) {
				eventIdToSession.remove(event.getEventId());
			}
			// 记录清理操作的调试信息
			logger.debug("Removed {} events for session {}", events.size(), sessionId);
		}
	}

	/**
	 * 清理所有会话的过期事件
	 * <p>
	 * 该方法会遍历所有会话，清理超过默认保留时间的事件。
	 * 通常由定时任务调用，实现自动清理功能。
	 * </p>
	 */
	public void cleanupExpiredEvents() {
		// 遍历所有会话的keySet（会话ID集合）
		for (String sessionId : sessionEvents.keySet()) {
			// 对每个会话调用清理方法，使用默认的保留时间
			cleanupOldEvents(sessionId, defaultRetentionMs);
		}
	}
}

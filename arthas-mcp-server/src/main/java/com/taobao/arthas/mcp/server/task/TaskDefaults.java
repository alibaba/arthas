/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.time.Duration;

/**
 * Default configuration constants for the task system.
 *
 * @author Yeaury
 */
public final class TaskDefaults {

    public static final long DEFAULT_TTL_MS = 10 * 60 * 1000L;

    public static final long DEFAULT_POLL_INTERVAL_MS = 1000L;

    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * task 记录存储容量上限（内存兜底）。
     * 统计的是 tasks Map 中所有状态的 entry 总数（包含已完成但未被 TTL 清理的），
     */
    public static final int DEFAULT_MAX_TASKS = 10_000;

    /**
     * 并发 task session 上限（资源保护）。
     * 统计的是正在执行中的 Arthas session 数量，保护目标 JVM 不被过多 session 拖垮。
     *
     * <p>取值 5 的依据：
     * <ul>
     *   <li>每个 task 会触发 {@code Instrumentation.retransformClasses}，引发一次 JVM STW 暂停，
     *       并使目标方法的 JIT 编译结果失效（去优化）。N 个并发 task 意味着 N 次串行 STW。</li>
     *   <li>AdviceListenerManager 按 (ClassLoader, 类, 方法签名) 存储 {@code List<AdviceListener>}，
     *       没有 adviceId 维度。同一热点方法被 N 个 task 监听时，每次方法调用要顺序回调 N 次，
     *       开销线性叠加。以 1000 QPS 为例：5 个 listener × 约 50μs/次 ≈ 单核 25% 开销，
     *       是业务延迟开始可感知的经验阈值；10 个时翻倍至 50%。</li>
     *   <li>当前支持 task 模式的流式工具恰好有 5 个（watch / trace / stack / tt / monitor），
     *       上限设为 5 对应"每种工具最多同时运行一个 task"，语义直观。</li>
     *   <li>与 Arthas 社区实践对齐：官方建议生产环境同时活跃的 trace/watch 不超过 5 个
     *       （参见 alibaba/arthas#44）。</li>
     * </ul>
     */
    public static final int DEFAULT_MAX_CONCURRENT_TASK_SESSIONS = 5;

    public static final long DEFAULT_AUTOMATIC_POLLING_TIMEOUT_MS = 600000L;

    public static final int DEFAULT_SIDE_CHANNEL_TIMEOUT_MINUTES = 5;

    public static final long MAX_TTL_MS = 24 * 60 * 60 * 1000L;

    public static final long MIN_POLL_INTERVAL_MS = 100L;

    public static final long MAX_POLL_INTERVAL_MS = 60 * 60 * 1000L;

    public static final long CLEANUP_INTERVAL_MINUTES = 1L;

    public static final long MESSAGE_QUEUE_CLEANUP_TIMEOUT_MS = 1_000L;

    public static final long RESPONSE_POLL_INTERVAL_MS = 50L;

    public static final long TASK_STORE_SHUTDOWN_TIMEOUT_SECONDS = 5L;

    public static final int DEFAULT_MAX_POLL_ATTEMPTS = 60;

    public static final long MAX_TIMEOUT_MS = 3_600_000L;

    public static final int MAX_WATCH_UPDATES = 100;

    public static final McpSchema.JsonSchema EMPTY_INPUT_SCHEMA =
            new McpSchema.JsonSchema("object", null, null, null);

    /**
     * Calculates a polling timeout scaled to the given poll interval, capped at {@link #MAX_TIMEOUT_MS}.
     */
    public static Duration calculateTimeout(Long pollInterval) {
        long interval = pollInterval != null ? pollInterval : DEFAULT_POLL_INTERVAL_MS;
        long calculatedMs = interval * DEFAULT_MAX_POLL_ATTEMPTS;
        return Duration.ofMillis(Math.min(calculatedMs, MAX_TIMEOUT_MS));
    }

    /**
     * Validates that task-aware tools are not registered without a TaskStore.
     */
    public static void validateTaskConfiguration(boolean hasTaskTools, boolean hasTaskStore) {
        if (hasTaskTools && !hasTaskStore) {
            throw new IllegalStateException(
                    "Task-aware tools registered but no TaskStore configured. " +
                    "Add a TaskStore via .taskStore(store) or remove task tools.");
        }
    }

    private TaskDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }
}

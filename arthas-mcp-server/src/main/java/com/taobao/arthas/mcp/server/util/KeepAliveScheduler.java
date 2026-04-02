/**
 * Copyright 2025 - 2025 the original author or authors.
 */

package com.taobao.arthas.mcp.server.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 定期向所有活跃的 MCP 客户端发送心跳（Ping）请求以维持连接的调度器。
 * <p>
 * 某些网络环境（如负载均衡、反向代理、防火墙）会对长时间无数据传输的连接执行空闲超时断开。
 * 本调度器通过以固定时间间隔向所有已连接的 {@link McpSession} 发送 MCP Ping 请求，
 * 使连接保持活跃，防止被中间层断开。
 * <p>
 * 主要特性：
 * <ul>
 *   <li>支持自定义初始延迟（{@link Builder#initialDelay}）和心跳间隔（{@link Builder#interval}）</li>
 *   <li>支持外部注入自定义 {@link ScheduledExecutorService}（{@link Builder#scheduler}）；
 *       若未注入则自动创建一个守护线程池</li>
 *   <li>线程安全：运行状态通过 {@link AtomicBoolean} 保证原子更新</li>
 *   <li>每次心跳失败仅打印警告日志，不影响其他 Session 的心跳发送</li>
 * </ul>
 * <p>
 * 推荐通过 {@link Builder} 创建实例：
 * <pre>{@code
 * KeepAliveScheduler scheduler = KeepAliveScheduler.builder(() -> sessionManager.getActiveSessions())
 *     .initialDelay(Duration.ofSeconds(5))
 *     .interval(Duration.ofSeconds(30))
 *     .build()
 *     .start();
 * }</pre>
 */
public class KeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);

    /**
     * 用于接收 MCP Ping 请求响应结果的泛型类型引用。
     * Ping 响应结果为通用 Object 类型，不需要特定的反序列化目标类。
     */
    private static final TypeReference<Object> OBJECT_TYPE_REF = new TypeReference<Object>() {
    };

    /**
     * 第一次心跳发送前的等待时间。
     * 可在 {@link Builder#initialDelay(Duration)} 中配置，默认为 0（立即开始）。
     */
    private final Duration initialDelay;

    /**
     * 相邻两次心跳发送之间的时间间隔。
     * 可在 {@link Builder#interval(Duration)} 中配置，默认为 30 秒。
     */
    private final Duration interval;

    /**
     * 用于调度心跳任务的线程池。
     * 若调用方通过 {@link Builder#scheduler(ScheduledExecutorService)} 注入，则使用外部线程池；
     * 否则由 {@link Builder#build()} 自动创建一个单线程守护线程池。
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 标记当前调度器是否持有 {@link #scheduler} 的所有权。
     * <p>
     * {@code true}：线程池由本调度器创建，{@link #shutdown()} 时需要负责关闭它；
     * {@code false}：线程池由外部注入，{@link #shutdown()} 时不关闭线程池（生命周期由外部管理）。
     */
    private final boolean ownsExecutor;

    /**
     * 调度器的运行状态标志。
     * <p>
     * 使用 {@link AtomicBoolean} 保证多线程并发调用 {@link #start()} / {@link #stop()} 时的原子性，
     * 防止重复启动或重复停止。
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 当前正在执行的定时任务句柄。
     * <p>
     * 使用 {@code volatile} 保证多线程可见性。
     * 在 {@link #start()} 时赋值，在 {@link #stop()} 时取消。
     */
    private volatile ScheduledFuture<?> currentTask;

    /**
     * 提供当前所有活跃 MCP Session 集合的供给者（Supplier）。
     * <p>
     * 每次心跳触发时调用 {@code get()} 获取最新的 Session 列表，
     * 确保动态加入或离开的 Session 都能被正确处理。
     */
    private final Supplier<? extends Collection<? extends McpSession>> mcpSessions;

    /**
     * 私有构造方法，通过 {@link Builder} 创建实例。
     *
     * @param scheduler    用于调度心跳任务的线程池
     * @param ownsExecutor 是否持有线程池所有权（关闭时是否负责销毁线程池）
     * @param initialDelay 首次心跳前的等待时间
     * @param interval     相邻心跳的时间间隔
     * @param mcpSessions  提供活跃 MCP Session 集合的 Supplier
     */
    private KeepAliveScheduler(ScheduledExecutorService scheduler, boolean ownsExecutor, Duration initialDelay,
                            Duration interval, Supplier<? extends Collection<? extends McpSession>> mcpSessions) {
        this.scheduler = scheduler;
        this.ownsExecutor = ownsExecutor;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.mcpSessions = mcpSessions;
    }

    /**
     * 创建 {@link Builder} 实例，用于配置并构建 {@link KeepAliveScheduler}。
     *
     * @param mcpSessions 提供活跃 MCP Session 集合的 Supplier，不能为 {@code null}
     * @return 新的 {@link Builder} 实例
     */
    public static Builder builder(Supplier<? extends Collection<? extends McpSession>> mcpSessions) {
        return new Builder(mcpSessions);
    }

    /**
     * 启动心跳调度，以固定时间间隔向所有活跃 Session 发送 Ping 请求。
     * <p>
     * 使用 {@link AtomicBoolean#compareAndSet} 保证仅能启动一次：
     * 若当前已处于运行状态（{@link #isRunning} 为 {@code true}），则抛出 {@link IllegalStateException}。
     * <p>
     * 内部使用 {@link ScheduledExecutorService#scheduleAtFixedRate} 以固定频率调度心跳任务，
     * 心跳逻辑封装在 {@link #sendKeepAlivePings()} 中。
     *
     * @return 当前 {@link KeepAliveScheduler} 实例，支持链式调用
     * @throws IllegalStateException 若调度器已处于运行状态
     */
    public KeepAliveScheduler start() {
        if (this.isRunning.compareAndSet(false, true)) {
            logger.debug("Starting KeepAlive scheduler with initial delay: {}ms, interval: {}ms",
                        initialDelay.toMillis(), interval.toMillis());

            // 以固定频率调度心跳任务，初始延迟和间隔均以毫秒为单位
            this.currentTask = this.scheduler.scheduleAtFixedRate(
                this::sendKeepAlivePings,
                this.initialDelay.toMillis(),
                this.interval.toMillis(),
                TimeUnit.MILLISECONDS
            );

            return this;
        } else {
            throw new IllegalStateException("KeepAlive scheduler is already running. Stop it first.");
        }
    }

    /**
     * 向所有活跃的 MCP Session 发送 Ping 心跳请求。
     * <p>
     * 该方法由 {@link ScheduledExecutorService} 定期回调，执行逻辑如下：
     * <ol>
     *   <li>通过 {@link #mcpSessions} Supplier 获取当前活跃的 Session 集合。</li>
     *   <li>若集合为空，记录 trace 日志并直接返回。</li>
     *   <li>遍历每个 Session，调用 {@link McpSession#sendRequest} 发送 MCP Ping 请求。</li>
     *   <li>Ping 请求以异步方式（{@code CompletableFuture}）执行，通过 {@code whenComplete} 回调记录成功/失败日志。</li>
     *   <li>单个 Session 发送失败时仅打印警告日志，不影响其他 Session 的心跳发送。</li>
     *   <li>整个心跳周期若发生未预期异常，通过最外层 try-catch 捕获并记录 error 日志，
     *       不向调度器抛出异常（否则会导致调度任务停止）。</li>
     * </ol>
     */
    private void sendKeepAlivePings() {
        try {
            Collection<? extends McpSession> sessions = this.mcpSessions.get();
            if (sessions == null || sessions.isEmpty()) {
                logger.trace("No active sessions to ping");
                return;
            }

            logger.trace("Sending keep-alive pings to {} sessions", sessions.size());

            for (McpSession session : sessions) {
                try {
                    // 异步发送 MCP Ping 请求，方法名由 McpSchema.METHOD_PING 定义，无请求体参数（null）
                    session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                // Ping 失败：记录警告日志，但不中断对其他 Session 的心跳
                                logger.warn("Failed to send keep-alive ping to session {}: {}",
                                           session, error.getMessage());
                            } else {
                                logger.trace("Keep-alive ping sent successfully to session {}", session);
                            }
                        });
                } catch (Exception e) {
                    // 单个 Session 发送时抛出同步异常（如 Session 已关闭），仅记录警告
                    logger.warn("Exception while sending keep-alive ping to session {}: {}",
                               session, e.getMessage());
                }
            }
        } catch (Exception e) {
            // 捕获整个心跳周期中的未预期异常，避免异常向上传播导致调度任务被取消
            logger.error("Error during keep-alive ping cycle", e);
        }
    }

    /**
     * 停止心跳调度。
     * <p>
     * 取消当前正在运行的定时任务（不中断正在执行的心跳），并将 {@link #isRunning} 设为 {@code false}。
     * 若任务已被取消或从未启动，则此方法为空操作（no-op）。
     * <p>
     * 注意：此方法不关闭底层 {@link ScheduledExecutorService}，
     * 如需彻底释放线程资源，请调用 {@link #shutdown()}。
     */
    public void stop() {
        if (this.currentTask != null && !this.currentTask.isCancelled()) {
            // 取消定时任务，参数 false 表示不中断正在执行的心跳轮次
            this.currentTask.cancel(false);
            logger.debug("KeepAlive scheduler stopped");
        }
        this.isRunning.set(false);
    }

    /**
     * 判断心跳调度器当前是否处于运行状态。
     *
     * @return 若调度器正在运行则返回 {@code true}，否则返回 {@code false}
     */
    public boolean isRunning() {
        return this.isRunning.get();
    }

    /**
     * 停止心跳调度并关闭底层线程池（仅当调度器持有线程池所有权时）。
     * <p>
     * 执行步骤：
     * <ol>
     *   <li>调用 {@link #stop()} 停止心跳任务。</li>
     *   <li>若 {@link #ownsExecutor} 为 {@code true} 且线程池尚未关闭，则优雅关闭线程池：
     *     <ul>
     *       <li>调用 {@link ScheduledExecutorService#shutdown()} 发起优雅关闭请求，
     *           不再接受新任务，等待已提交的任务完成。</li>
     *       <li>等待最多 5 秒让任务自然结束；若 5 秒内未完成，调用 {@code shutdownNow()} 强制中断。</li>
     *       <li>若等待期间线程被中断，同样调用 {@code shutdownNow()} 并恢复中断标志。</li>
     *     </ul>
     *   </li>
     *   <li>若 {@link #ownsExecutor} 为 {@code false}（线程池由外部注入），则不操作线程池。</li>
     * </ol>
     */
    public void shutdown() {
        stop();
        if (this.ownsExecutor && !this.scheduler.isShutdown()) {
            // 发起优雅关闭，不再接受新任务
            this.scheduler.shutdown();
            try {
                // 等待最多 5 秒让已提交的任务完成
                if (!this.scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    // 超时后强制中断所有正在执行的任务
                    this.scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                // 等待期间被中断：强制关闭并恢复线程中断标志
                this.scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.debug("KeepAlive scheduler executor shut down");
        }
    }

    /**
     * {@link KeepAliveScheduler} 的构造器，支持链式调用配置各项参数。
     */
    public static class Builder {

        /**
         * 外部注入的调度线程池，为 {@code null} 时由 {@link #build()} 自动创建。
         */
        private ScheduledExecutorService scheduler;

        /**
         * 是否持有线程池所有权。
         * 由外部注入时为 {@code false}；由 {@link #build()} 自动创建时为 {@code true}。
         */
        private boolean ownsExecutor = false;

        /**
         * 首次心跳前的等待时间，默认为 0（立即开始）。
         */
        private Duration initialDelay = Duration.ofSeconds(0);

        /**
         * 相邻心跳的时间间隔，默认为 30 秒。
         */
        private Duration interval = Duration.ofSeconds(30);

        /**
         * 提供活跃 MCP Session 集合的 Supplier，必须在构造时提供。
         */
        private Supplier<? extends Collection<? extends McpSession>> mcpSessions;

        /**
         * 构造 Builder，注入提供 MCP Session 集合的 Supplier。
         *
         * @param mcpSessions 提供活跃 MCP Session 集合的 Supplier，不能为 {@code null}
         * @throws IllegalArgumentException 若 {@code mcpSessions} 为 {@code null}
         */
        Builder(Supplier<? extends Collection<? extends McpSession>> mcpSessions) {
            Assert.notNull(mcpSessions, "McpSessions supplier must not be null");
            this.mcpSessions = mcpSessions;
        }

        /**
         * 注入自定义的 {@link ScheduledExecutorService}。
         * <p>
         * 注入后，{@link #ownsExecutor} 设为 {@code false}，
         * 即调度器不持有线程池所有权，{@link KeepAliveScheduler#shutdown()} 时不会关闭该线程池，
         * 线程池的生命周期由调用方自行管理。
         *
         * @param scheduler 外部线程池，不能为 {@code null}
         * @return 当前 Builder 实例，支持链式调用
         * @throws IllegalArgumentException 若 {@code scheduler} 为 {@code null}
         */
        public Builder scheduler(ScheduledExecutorService scheduler) {
            Assert.notNull(scheduler, "Scheduler must not be null");
            this.scheduler = scheduler;
            this.ownsExecutor = false;
            return this;
        }

        /**
         * 设置首次心跳前的等待时间。
         *
         * @param initialDelay 初始延迟，不能为 {@code null}
         * @return 当前 Builder 实例，支持链式调用
         * @throws IllegalArgumentException 若 {@code initialDelay} 为 {@code null}
         */
        public Builder initialDelay(Duration initialDelay) {
            Assert.notNull(initialDelay, "Initial delay must not be null");
            this.initialDelay = initialDelay;
            return this;
        }

        /**
         * 设置相邻两次心跳发送之间的时间间隔。
         *
         * @param interval 心跳间隔，不能为 {@code null}
         * @return 当前 Builder 实例，支持链式调用
         * @throws IllegalArgumentException 若 {@code interval} 为 {@code null}
         */
        public Builder interval(Duration interval) {
            Assert.notNull(interval, "Interval must not be null");
            this.interval = interval;
            return this;
        }

        /**
         * 根据当前 Builder 配置构造 {@link KeepAliveScheduler} 实例。
         * <p>
         * 若未通过 {@link #scheduler(ScheduledExecutorService)} 注入外部线程池，
         * 则自动创建一个以 {@code "mcp-keep-alive-scheduler"} 命名的单线程守护线程池：
         * <ul>
         *   <li>单线程保证心跳任务串行执行，避免并发问题</li>
         *   <li>设置为守护线程（{@code t.setDaemon(true)}），JVM 退出时不会被此线程阻塞</li>
         *   <li>{@link #ownsExecutor} 设为 {@code true}，由调度器负责关闭线程池</li>
         * </ul>
         *
         * @return 配置完成的 {@link KeepAliveScheduler} 实例（尚未启动，需调用 {@link #start()} 启动）
         */
        public KeepAliveScheduler build() {
            if (this.scheduler == null) {
                // 未注入外部线程池，创建内部单线程守护线程池
                this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "mcp-keep-alive-scheduler");
                    // 设置为守护线程，不阻止 JVM 正常退出
                    t.setDaemon(true);
                    return t;
                });
                // 标记由本调度器持有线程池所有权，shutdown() 时负责关闭
                this.ownsExecutor = true;
            }

            return new KeepAliveScheduler(scheduler, ownsExecutor, initialDelay, interval, mcpSessions);
        }
    }
}

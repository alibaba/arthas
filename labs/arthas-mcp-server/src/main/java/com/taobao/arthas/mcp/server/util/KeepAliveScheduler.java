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
 * A utility class for scheduling regular keep-alive calls to maintain connections. It
 * sends periodic keep-alive, ping, messages to connected mcp clients to prevent idle
 * timeouts.
 *
 * The pings are sent to all active mcp sessions at regular intervals.
 *
 */
public class KeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private static final TypeReference<Object> OBJECT_TYPE_REF = new TypeReference<Object>() {
    };

    /** Initial delay before the first keepAlive call */
    private final Duration initialDelay;

    /** Interval between subsequent keepAlive calls */
    private final Duration interval;

    /** The scheduler used for executing keepAlive calls */
    private final ScheduledExecutorService scheduler;

    /** Whether this scheduler owns the executor and should shut it down */
    private final boolean ownsExecutor;

    /** The current state of the scheduler */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /** The current scheduled task */
    private volatile ScheduledFuture<?> currentTask;

    /** Supplier for McpSession instances */
    private final Supplier<? extends Collection<? extends McpSession>> mcpSessions;

    /**
     * Creates a KeepAliveScheduler with a custom scheduler, initial delay, interval and a
     * supplier for McpSession instances.
     * @param scheduler The scheduler to use for executing keepAlive calls
     * @param ownsExecutor Whether this scheduler owns the executor and should shut it down
     * @param initialDelay Initial delay before the first keepAlive call
     * @param interval Interval between subsequent keepAlive calls
     * @param mcpSessions Supplier for McpSession instances
     */
    private KeepAliveScheduler(ScheduledExecutorService scheduler, boolean ownsExecutor, Duration initialDelay,
                            Duration interval, Supplier<? extends Collection<? extends McpSession>> mcpSessions) {
        this.scheduler = scheduler;
        this.ownsExecutor = ownsExecutor;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.mcpSessions = mcpSessions;
    }

    public static Builder builder(Supplier<? extends Collection<? extends McpSession>> mcpSessions) {
        return new Builder(mcpSessions);
    }

    /**
     * Starts regular keepAlive calls with sessions supplier.
     * @return This scheduler instance for method chaining
     */
    public KeepAliveScheduler start() {
        if (this.isRunning.compareAndSet(false, true)) {
            logger.debug("Starting KeepAlive scheduler with initial delay: {}ms, interval: {}ms", 
                        initialDelay.toMillis(), interval.toMillis());

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
     * Sends keep-alive pings to all active sessions.
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
                    session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                logger.warn("Failed to send keep-alive ping to session {}: {}", 
                                           session, error.getMessage());
                            } else {
                                logger.trace("Keep-alive ping sent successfully to session {}", session);
                            }
                        });
                } catch (Exception e) {
                    logger.warn("Exception while sending keep-alive ping to session {}: {}", 
                               session, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error during keep-alive ping cycle", e);
        }
    }

    public void stop() {
        if (this.currentTask != null && !this.currentTask.isCancelled()) {
            this.currentTask.cancel(false);
            logger.debug("KeepAlive scheduler stopped");
        }
        this.isRunning.set(false);
    }

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public void shutdown() {
        stop();
        if (this.ownsExecutor && !this.scheduler.isShutdown()) {
            this.scheduler.shutdown();
            try {
                if (!this.scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.debug("KeepAlive scheduler executor shut down");
        }
    }

    public static class Builder {

        private ScheduledExecutorService scheduler;
        private boolean ownsExecutor = false;
        private Duration initialDelay = Duration.ofSeconds(0);
        private Duration interval = Duration.ofSeconds(30);
        private Supplier<? extends Collection<? extends McpSession>> mcpSessions;

        Builder(Supplier<? extends Collection<? extends McpSession>> mcpSessions) {
            Assert.notNull(mcpSessions, "McpSessions supplier must not be null");
            this.mcpSessions = mcpSessions;
        }

        public Builder scheduler(ScheduledExecutorService scheduler) {
            Assert.notNull(scheduler, "Scheduler must not be null");
            this.scheduler = scheduler;
            this.ownsExecutor = false;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            Assert.notNull(initialDelay, "Initial delay must not be null");
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder interval(Duration interval) {
            Assert.notNull(interval, "Interval must not be null");
            this.interval = interval;
            return this;
        }

        public KeepAliveScheduler build() {
            if (this.scheduler == null) {
                this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "mcp-keep-alive-scheduler");
                    t.setDaemon(true);
                    return t;
                });
                this.ownsExecutor = true;
            }
            
            return new KeepAliveScheduler(scheduler, ownsExecutor, initialDelay, interval, mcpSessions);
        }
    }
}

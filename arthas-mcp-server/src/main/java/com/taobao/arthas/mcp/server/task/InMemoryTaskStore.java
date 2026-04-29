/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory {@link TaskStore} implementation with TTL-based cleanup.
 *
 * <p>Uses {@link ConcurrentSkipListMap} for O(log n) sorted access and efficient
 * cursor-based pagination via {@code tailMap()}.
 *
 * @param <R> result type
 * @author Yeaury
 */
public class InMemoryTaskStore<R extends McpSchema.Result> implements TaskStore<R> {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryTaskStore.class);

    private static final long DEFAULT_TTL_MS = TaskDefaults.DEFAULT_TTL_MS;

    private static final long DEFAULT_POLL_INTERVAL_MS = TaskDefaults.DEFAULT_POLL_INTERVAL_MS;

    private static final int DEFAULT_PAGE_SIZE = TaskDefaults.DEFAULT_PAGE_SIZE;

    private static final int DEFAULT_MAX_TASKS = TaskDefaults.DEFAULT_MAX_TASKS;

    private final NavigableMap<String, TaskEntry> tasks = new ConcurrentSkipListMap<>();

    private final Map<String, R> results = new ConcurrentHashMap<>();

    private final Set<String> cancellationRequests = ConcurrentHashMap.newKeySet();

    private final Map<String, Long> expiredCancellationDeadlines = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor;

    private final long defaultTtl;

    private final long defaultPollInterval;

    private final long expiredCancellationRetentionMs;

    private static final AtomicLong INSTANCE_COUNTER = new AtomicLong(0);

    private final long instanceId;

    private final TaskMessageQueue messageQueue;

    private final int maxTasks;

    private static final long DEFAULT_EXPIRED_CANCELLATION_RETENTION_MS = 30 * 60 * 1000L;

    public InMemoryTaskStore() {
        this(DEFAULT_TTL_MS, DEFAULT_POLL_INTERVAL_MS, null, DEFAULT_MAX_TASKS);
    }

    public InMemoryTaskStore(long defaultTtl, long defaultPollInterval) {
        this(defaultTtl, defaultPollInterval, null, DEFAULT_MAX_TASKS);
    }

    public InMemoryTaskStore(long defaultTtl, long defaultPollInterval, TaskMessageQueue messageQueue) {
        this(defaultTtl, defaultPollInterval, messageQueue, DEFAULT_MAX_TASKS);
    }

    public InMemoryTaskStore(long defaultTtl, long defaultPollInterval,
                             TaskMessageQueue messageQueue, int maxTasks) {
        this(defaultTtl, defaultPollInterval, messageQueue, maxTasks,
                DEFAULT_EXPIRED_CANCELLATION_RETENTION_MS);
    }

    InMemoryTaskStore(long defaultTtl, long defaultPollInterval,
                      TaskMessageQueue messageQueue, int maxTasks,
                      long expiredCancellationRetentionMs) {
        if (defaultTtl <= 0) throw new IllegalArgumentException("defaultTtl must be positive");
        if (defaultPollInterval <= 0) throw new IllegalArgumentException("defaultPollInterval must be positive");
        if (maxTasks <= 0) throw new IllegalArgumentException("maxTasks must be positive");
        if (expiredCancellationRetentionMs <= 0) {
            throw new IllegalArgumentException("expiredCancellationRetentionMs must be positive");
        }

        this.instanceId = INSTANCE_COUNTER.incrementAndGet();
        this.defaultTtl = defaultTtl;
        this.defaultPollInterval = defaultPollInterval;
        this.messageQueue = messageQueue;
        this.maxTasks = maxTasks;
        this.expiredCancellationRetentionMs = expiredCancellationRetentionMs;

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mcp-task-cleanup-" + instanceId);
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredTasks, 1, 1, TimeUnit.MINUTES);
    }

    public static <R extends McpSchema.Result> Builder<R> builder() {
        return new Builder<>();
    }

    public static class Builder<R extends McpSchema.Result> {

        private long defaultTtl = DEFAULT_TTL_MS;
        private long defaultPollInterval = DEFAULT_POLL_INTERVAL_MS;
        private TaskMessageQueue messageQueue = null;
        private int maxTasks = DEFAULT_MAX_TASKS;

        public Builder<R> defaultTtl(Duration ttl) {
            this.defaultTtl = ttl.toMillis();
            return this;
        }

        public Builder<R> defaultTtlMs(long ttlMs) {
            this.defaultTtl = ttlMs;
            return this;
        }

        public Builder<R> defaultPollInterval(Duration interval) {
            this.defaultPollInterval = interval.toMillis();
            return this;
        }

        public Builder<R> defaultPollIntervalMs(long intervalMs) {
            this.defaultPollInterval = intervalMs;
            return this;
        }

        public Builder<R> messageQueue(TaskMessageQueue queue) {
            this.messageQueue = queue;
            return this;
        }

        public Builder<R> maxTasks(int max) {
            this.maxTasks = max;
            return this;
        }

        public InMemoryTaskStore<R> build() {
            return new InMemoryTaskStore<>(defaultTtl, defaultPollInterval, messageQueue, maxTasks);
        }
    }

    private final Object createTaskLock = new Object();

    private boolean isSessionValid(TaskEntry entry, String requestSessionId) {
        if (requestSessionId == null) return true;
        String taskSessionId = entry.sessionId();
        if (taskSessionId == null || taskSessionId.isEmpty()) return true;
        return requestSessionId.equals(taskSessionId);
    }

    @Override
    public CompletableFuture<McpSchema.Task> createTask(CreateTaskOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (createTaskLock) {
                if (tasks.size() >= maxTasks) {
                    throw new CompletionException(
                        McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                            .message("Maximum task limit reached (" + maxTasks + ")")
                            .build()
                    );
                }

                String taskId = options.taskId() != null ? options.taskId() : UUID.randomUUID().toString();
                String now = Instant.now().toString();
                Long ttl = options.requestedTtl() != null ? options.requestedTtl() : defaultTtl;
                Long pollInterval = options.pollInterval() != null ? options.pollInterval() : defaultPollInterval;
                String sessionId = options.sessionId();

                McpSchema.Task task = McpSchema.Task.builder()
                    .taskId(taskId)
                    .status(McpSchema.TaskStatus.WORKING)
                    .createdAt(now)
                    .lastUpdatedAt(now)
                    .ttl(ttl)
                    .pollInterval(pollInterval)
                    .build();

                tasks.put(taskId, new TaskEntry(task, options.originatingRequest(),
                                                options.context(), sessionId));
                logger.info("createTask: Created task - taskId: {}, sessionId: {}", taskId, sessionId);
                return task;
            }
        });
    }

    @Override
    public CompletableFuture<GetTaskFromStoreResult> getTask(String taskId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            TaskEntry entry = tasks.get(taskId);
            if (entry == null) {
                logger.debug("getTask: Task not found - taskId: {}", taskId);
                return null;
            }
            if (!isSessionValid(entry, sessionId)) {
                logger.warn("getTask: Session validation failed - taskId: {}", taskId);
                return null;
            }
            return new GetTaskFromStoreResult(entry.task(), entry.originatingRequest());
        });
    }

    @Override
    public CompletableFuture<Void> updateTaskStatus(String taskId, String sessionId,
                                                     McpSchema.TaskStatus status, String statusMessage) {
        return CompletableFuture.runAsync(() -> {
            tasks.computeIfPresent(taskId, (id, entry) -> {
                if (!isSessionValid(entry, sessionId)) return entry;
                McpSchema.Task oldTask = entry.task();
                if (TaskHelper.isTerminal(oldTask.getStatus())) return entry;
                String now = Instant.now().toString();
                McpSchema.Task newTask = McpSchema.Task.builder()
                    .taskId(oldTask.getTaskId())
                    .status(status)
                    .statusMessage(statusMessage)
                    .createdAt(oldTask.getCreatedAt())
                    .lastUpdatedAt(now)
                    .ttl(oldTask.getTtl())
                    .pollInterval(oldTask.getPollInterval())
                    .build();
                logger.debug("Updated task {} status: {} -> {}", taskId, oldTask.getStatus(), status);
                return new TaskEntry(newTask, entry.originatingRequest(), entry.context(), entry.sessionId());
            });
        });
    }

    @Override
    public CompletableFuture<Void> storeTaskResult(String taskId, String sessionId,
                                                    McpSchema.TaskStatus status, R result) {
        return CompletableFuture.runAsync(() -> {
            AtomicBoolean taskFound = new AtomicBoolean(false);
            AtomicBoolean sessionValid = new AtomicBoolean(true);
            AtomicBoolean wasTerminal = new AtomicBoolean(false);

            tasks.computeIfPresent(taskId, (id, entry) -> {
                taskFound.set(true);
                if (!isSessionValid(entry, sessionId)) {
                    sessionValid.set(false);
                    return entry;
                }
                McpSchema.Task oldTask = entry.task();
                if (TaskHelper.isTerminal(oldTask.getStatus())) {
                    wasTerminal.set(true);
                    return entry;
                }
                results.put(taskId, result);
                String now = Instant.now().toString();
                McpSchema.Task newTask = McpSchema.Task.builder()
                    .taskId(oldTask.getTaskId())
                    .status(status)
                    .createdAt(oldTask.getCreatedAt())
                    .lastUpdatedAt(now)
                    .ttl(oldTask.getTtl())
                    .pollInterval(oldTask.getPollInterval())
                    .build();
                logger.debug("Stored result for task: {}", taskId);
                return new TaskEntry(newTask, entry.originatingRequest(), entry.context(), entry.sessionId());
            });

            if (!taskFound.get()) {
                throw new CompletionException(
                    McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                        .message("Task not found (may have expired after TTL): " + taskId)
                        .build()
                );
            }
            if (!sessionValid.get()) {
                throw new CompletionException(
                    McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                        .message("Task not found (may have expired after TTL): " + taskId)
                        .build()
                );
            }
            if (wasTerminal.get()) {
                logger.debug("Skipped storing result for task {} - already in terminal state", taskId);
            }
        });
    }

    @Override
    public CompletableFuture<R> getTaskResult(String taskId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            TaskEntry entry = tasks.get(taskId);
            if (entry == null || !isSessionValid(entry, sessionId)) return null;
            return results.get(taskId);
        });
    }

    @Override
    public CompletableFuture<McpSchema.ListTasksResult> listTasks(String cursor, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            List<McpSchema.Task> taskList = new ArrayList<>();
            String nextCursor = null;

            // Use tailMap for O(log n) cursor lookup; handles missing cursors gracefully
            NavigableMap<String, TaskEntry> view = cursor != null
                    ? tasks.tailMap(cursor, false)
                    : tasks;

            Iterator<Map.Entry<String, TaskEntry>> iterator = view.entrySet().iterator();
            int count = 0;
            String lastKey = null;

            while (iterator.hasNext() && count < DEFAULT_PAGE_SIZE) {
                Map.Entry<String, TaskEntry> entry = iterator.next();
                TaskEntry taskEntry = entry.getValue();
                if (sessionId != null && !sessionId.equals(taskEntry.sessionId())) continue;
                taskList.add(taskEntry.task());
                lastKey = entry.getKey();
                count++;
            }

            while (iterator.hasNext()) {
                Map.Entry<String, TaskEntry> entry = iterator.next();
                if (sessionId == null || sessionId.equals(entry.getValue().sessionId())) {
                    nextCursor = lastKey;
                    break;
                }
            }

            return new McpSchema.ListTasksResult(taskList, nextCursor);
        });
    }

    @Override
    public CompletableFuture<McpSchema.Task> requestCancellation(String taskId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<McpSchema.Task> resultRef = new AtomicReference<>();
            AtomicReference<McpSchema.TaskStatus> terminalStatusRef = new AtomicReference<>();
            AtomicBoolean sessionValid = new AtomicBoolean(true);

            tasks.computeIfPresent(taskId, (id, entry) -> {
                if (!isSessionValid(entry, sessionId)) {
                    sessionValid.set(false);
                    return entry;
                }
                McpSchema.Task oldTask = entry.task();
                if (TaskHelper.isTerminal(oldTask.getStatus())) {
                    terminalStatusRef.set(oldTask.getStatus());
                    resultRef.set(oldTask);
                    return entry;
                }
                cancellationRequests.add(taskId);
                String now = Instant.now().toString();
                McpSchema.Task newTask = McpSchema.Task.builder()
                    .taskId(oldTask.getTaskId())
                    .status(McpSchema.TaskStatus.CANCELLED)
                    .statusMessage("Cancellation requested")
                    .createdAt(oldTask.getCreatedAt())
                    .lastUpdatedAt(now)
                    .ttl(oldTask.getTtl())
                    .pollInterval(oldTask.getPollInterval())
                    .build();
                resultRef.set(newTask);
                logger.info("Cancelled task: {}", taskId);
                return new TaskEntry(newTask, entry.originatingRequest(), entry.context(), entry.sessionId());
            });

            if (!sessionValid.get()) return null;

            McpSchema.TaskStatus terminalStatus = terminalStatusRef.get();
            if (terminalStatus != null) {
                throw new CompletionException(
                    McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                        .message("Cannot cancel task: already in terminal status '" + terminalStatus + "'")
                        .data("taskId: " + taskId)
                        .build()
                );
            }

            return resultRef.get();
        });
    }

    @Override
    public CompletableFuture<Boolean> isCancellationRequested(String taskId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            TaskEntry entry = tasks.get(taskId);
            // Bug fix: entry == null means TTL cleanup already removed the task.
            // We must NOT suppress the cancellation signal — if cancellationRequests still
            // contains the taskId, the background thread must see it and exit cleanly.
            if (entry != null && !isSessionValid(entry, sessionId)) return false;
            return cancellationRequests.contains(taskId);
        });
    }

    @Override
    public CompletableFuture<List<McpSchema.Task>> watchTaskUntilTerminal(
            String taskId, String sessionId, long timeoutMs) {

        CompletableFuture<List<McpSchema.Task>> future = new CompletableFuture<>();
        List<McpSchema.Task> updates = new CopyOnWriteArrayList<>();

        return getTask(taskId, sessionId).thenCompose(initialResult -> {
            if (initialResult == null) {
                CompletableFuture<List<McpSchema.Task>> failed = new CompletableFuture<>();
                failed.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                        .message("Task not found (may have expired after TTL): " + taskId)
                        .build());
                return failed;
            }

            McpSchema.Task initialTask = initialResult.task();
            long pollInterval = initialTask.getPollInterval() != null
                ? initialTask.getPollInterval()
                : DEFAULT_POLL_INTERVAL_MS;

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "task-watch-" + taskId);
                t.setDaemon(true);
                return t;
            });

            long startTime = System.currentTimeMillis();

            ScheduledFuture<?> pollingTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (System.currentTimeMillis() - startTime > timeoutMs) {
                        future.completeExceptionally(new TimeoutException("Task watch timeout"));
                        scheduler.shutdown();
                        return;
                    }
                    getTask(taskId, sessionId).whenComplete((result, ex) -> {
                        if (ex != null) {
                            future.completeExceptionally(ex);
                            scheduler.shutdown();
                            return;
                        }
                        if (result != null) {
                            McpSchema.Task task = result.task();
                            if (updates.size() < TaskDefaults.MAX_WATCH_UPDATES) {
                                updates.add(task);
                            } else {
                                updates.remove(0);
                                updates.add(task);
                            }
                            if (TaskHelper.isTerminal(task.getStatus())) {
                                future.complete(updates);
                                scheduler.shutdown();
                            }
                        }
                    });
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    scheduler.shutdown();
                }
            }, 0, pollInterval, TimeUnit.MILLISECONDS);

            future.whenComplete((result, ex) -> {
                if (!pollingTask.isDone()) pollingTask.cancel(false);
                scheduler.shutdown();
            });

            return future;
        });
    }

    /** Package-visible for testing. */
    void cleanupExpiredTasks() {
        Instant now = Instant.now();
        long nowMillis = now.toEpochMilli();
        List<String> expiredTaskIds = new ArrayList<>();

        cleanupExpiredCancellationRequests(nowMillis);

        tasks.entrySet().removeIf(entry -> {
            McpSchema.Task task = entry.getValue().task();
            if (task.getTtl() == null) return false;
            Instant expiresAt = Instant.parse(task.getCreatedAt()).plusMillis(task.getTtl());
            if (now.isAfter(expiresAt)) {
                String taskId = entry.getKey();
                results.remove(taskId);
                if (!TaskHelper.isTerminal(task.getStatus())
                        || task.getStatus() == McpSchema.TaskStatus.CANCELLED) {
                    // 对仍可能有后台执行线程的任务，移除 task 记录前先保留协作取消信号。
                    // CANCELLED 对客户端是终态，但不代表后台 worker 已退出。
                    // 保留取消信号一段时间，避免后台线程尚未观察到信号时被提前清理。
                    markExpiredTaskCancellation(taskId, nowMillis);
                    logger.debug("Retained cancellation signal for expired active task: {}", taskId);
                } else {
                    // COMPLETED/FAILED 已不需要协作取消信号，可以完整清理。
                    clearCancellationRequest(taskId);
                }
                expiredTaskIds.add(taskId);
                logger.debug("Removed expired task: {}", taskId);
                return true;
            }
            return false;
        });

        if (messageQueue != null && !expiredTaskIds.isEmpty()) {
            for (String taskId : expiredTaskIds) {
                messageQueue.clearTask(taskId).exceptionally(ex -> {
                    logger.warn("Failed to clear task queue for {}", taskId, ex);
                    return null;
                });
            }
            logger.debug("Completed cleanup of {} expired tasks", expiredTaskIds.size());
        }
    }

    private void markExpiredTaskCancellation(String taskId, long nowMillis) {
        cancellationRequests.add(taskId);
        expiredCancellationDeadlines.put(taskId, nowMillis + expiredCancellationRetentionMs);
    }

    private void clearCancellationRequest(String taskId) {
        cancellationRequests.remove(taskId);
        expiredCancellationDeadlines.remove(taskId);
    }

    private void cleanupExpiredCancellationRequests(long nowMillis) {
        expiredCancellationDeadlines.entrySet().removeIf(entry -> {
            if (nowMillis < entry.getValue()) {
                return false;
            }
            cancellationRequests.remove(entry.getKey());
            logger.debug("Removed expired cancellation signal: {}", entry.getKey());
            return true;
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
                logger.info("TaskStore shut down");
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    private static class TaskEntry {
        private final McpSchema.Task task;
        private final McpSchema.Request originatingRequest;
        private final Object context;
        private final String sessionId;

        TaskEntry(McpSchema.Task task, McpSchema.Request originatingRequest,
                  Object context, String sessionId) {
            this.task = task;
            this.originatingRequest = originatingRequest;
            this.context = context;
            this.sessionId = sessionId;
        }

        McpSchema.Task task() { return task; }
        McpSchema.Request originatingRequest() { return originatingRequest; }
        Object context() { return context; }
        String sessionId() { return sessionId; }
    }
}

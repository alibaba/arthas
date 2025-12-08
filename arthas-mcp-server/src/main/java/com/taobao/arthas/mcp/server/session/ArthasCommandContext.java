package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Command execution context for MCP server.
 * Manages command execution lifecycle and result collection.
 */
public class ArthasCommandContext {

    private static final Logger logger = LoggerFactory.getLogger(ArthasCommandContext.class);

    private static final long DEFAULT_SYNC_TIMEOUT = 30000L;

    private final CommandExecutor commandExecutor;
    private final ArthasCommandSessionManager.CommandSessionBinding binding;
    private volatile boolean executionComplete = false;
    private final List<Object> results = new CopyOnWriteArrayList<>();
    private final Lock resultLock = new ReentrantLock();

    public ArthasCommandContext(CommandExecutor commandExecutor) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor cannot be null");
        this.binding = null;
    }

    public ArthasCommandContext(CommandExecutor commandExecutor, ArthasCommandSessionManager.CommandSessionBinding binding) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor cannot be null");
        this.binding = binding;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public String getSessionId() {
        return binding != null ? binding.getArthasSessionId() : null;
    }

    /**
     * Alias for getSessionId() for compatibility
     */
    public String getArthasSessionId() {
        requireSessionSupport();
        return binding.getArthasSessionId();
    }

    private void requireSessionSupport() {
        if (binding == null) {
            throw new IllegalStateException("Session-based operations are not supported in temporary mode. " +
                    "Use ArthasCommandContext(CommandExecutor, CommandSessionBinding) constructor to enable session support.");
        }
    }
    
    public String getConsumerId() {
        return binding != null ? binding.getConsumerId() : null;
    }

    public ArthasCommandSessionManager.CommandSessionBinding getBinding() {
        return binding;
    }

    public boolean isExecutionComplete() {
        return executionComplete;
    }

    public void setExecutionComplete(boolean executionComplete) {
        this.executionComplete = executionComplete;
    }

    public void addResult(Object result) {
        results.add(result);
    }

    public List<Object> getResults() {
        return results;
    }

    public void clearResults() {
        results.clear();
    }

    public Lock getResultLock() {
        return resultLock;
    }

    /**
     * Execute command synchronously with default timeout
     */
    public Map<String, Object> executeSync(String commandLine) {
        return executeSync(commandLine, DEFAULT_SYNC_TIMEOUT);
    }

    /**
     * Execute command synchronously with specified timeout
     */
    public Map<String, Object> executeSync(String commandLine, long timeout) {
        return commandExecutor.executeSync(commandLine, timeout);
    }

    /**
     * Execute command synchronously with auth subject
     */
    public Map<String, Object> executeSync(String commandStr, Object authSubject) {
        return commandExecutor.executeSync(commandStr, DEFAULT_SYNC_TIMEOUT, null, authSubject, null);
    }

    /**
     * Execute command synchronously with auth subject and userId
     *
     * @param commandStr 命令行
     * @param authSubject 认证主体
     * @param userId 用户 ID，用于统计上报
     * @return 执行结果
     */
    public Map<String, Object> executeSync(String commandStr, Object authSubject, String userId) {
        return commandExecutor.executeSync(commandStr, DEFAULT_SYNC_TIMEOUT, null, authSubject, userId);
    }

    /**
     * Execute command asynchronously
     */
    public Map<String, Object> executeAsync(String commandLine) {
        requireSessionSupport();
        return commandExecutor.executeAsync(commandLine, binding.getArthasSessionId());
    }

    /**
     * Pull command execution results
     */
    public Map<String, Object> pullResults() {
        requireSessionSupport();
        return commandExecutor.pullResults(binding.getArthasSessionId(), binding.getConsumerId());
    }

    /**
     * Interrupt the current job
     */
    public Map<String, Object> interruptJob() {
        requireSessionSupport();
        return commandExecutor.interruptJob(binding.getArthasSessionId());
    }

    /**
     * Set session userId for statistics reporting
     *
     * @param userId 用户 ID
     */
    public void setSessionUserId(String userId) {
        if (binding != null && userId != null) {
            commandExecutor.setSessionUserId(binding.getArthasSessionId(), userId);
            logger.debug("Set userId for session {}: {}", binding.getArthasSessionId(), userId);
        }
    }
}

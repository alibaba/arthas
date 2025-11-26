package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * 命令执行上下文
 *
 * @author Yeaury
 */
public class ArthasCommandContext {

    private static final Logger logger = LoggerFactory.getLogger(ArthasCommandContext.class);

    private final CommandExecutor commandExecutor;

    private final ArthasCommandSessionManager.CommandSessionBinding sessionBinding;

    private static final long DEFAULT_SYNC_TIMEOUT = 30000L;

    public ArthasCommandContext(CommandExecutor commandExecutor) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor cannot be null");
        this.sessionBinding = null;
    }

    public ArthasCommandContext(CommandExecutor commandExecutor, ArthasCommandSessionManager.CommandSessionBinding sessionBinding) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor cannot be null");
        this.sessionBinding = Objects.requireNonNull(sessionBinding, "sessionBinding cannot be null");
    }

    private void requireSessionSupport() {
        if (sessionBinding == null) {
            throw new IllegalStateException("Session-based operations are not supported in temporary mode. " +
                    "Use ArthasCommandContext(CommandExecutor, CommandSessionBinding) constructor to enable session support.");
        }
    }

    public boolean isSessionSupported() {
        return sessionBinding != null;
    }

    public Map<String, Object> executeSync(String commandLine) {
        return executeSync(commandLine, DEFAULT_SYNC_TIMEOUT);
    }

    public Map<String, Object> executeSync(String commandLine, long timeout) {
        return commandExecutor.executeSync(commandLine, timeout);
    }

    public Map<String, Object> executeSync(String commandStr, Object authSubject) {
        return commandExecutor.executeSync(commandStr, DEFAULT_SYNC_TIMEOUT, null, authSubject, null);
    }

    /**
     * 同步执行命令，支持指定 authSubject 和 userId
     *
     * @param commandStr 命令行
     * @param authSubject 认证主体
     * @param userId 用户 ID，用于统计上报
     * @return 执行结果
     */
    public Map<String, Object> executeSync(String commandStr, Object authSubject, String userId) {
        return commandExecutor.executeSync(commandStr, DEFAULT_SYNC_TIMEOUT, null, authSubject, userId);
    }

    public Map<String, Object> executeAsync(String commandLine) {
        requireSessionSupport();
        return commandExecutor.executeAsync(commandLine, sessionBinding.getArthasSessionId());
    }

    public Map<String, Object> pullResults() {
        requireSessionSupport();
        return commandExecutor.pullResults(sessionBinding.getArthasSessionId(), sessionBinding.getConsumerId());
    }

    public Map<String, Object> interruptJob() {
        requireSessionSupport();
        return commandExecutor.interruptJob(sessionBinding.getArthasSessionId());
    }

    public String getArthasSessionId() {
        requireSessionSupport();
        return sessionBinding.getArthasSessionId();
    }

    /**
     * 设置 session 的 userId
     *
     * @param userId 用户 ID
     */
    public void setSessionUserId(String userId) {
        if (sessionBinding != null && userId != null) {
            commandExecutor.setSessionUserId(sessionBinding.getArthasSessionId(), userId);
            logger.debug("Set userId for session {}: {}", sessionBinding.getArthasSessionId(), userId);
        }
    }
}

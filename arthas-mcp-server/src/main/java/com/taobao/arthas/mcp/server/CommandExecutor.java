package com.taobao.arthas.mcp.server;

import java.util.Map;

/**
 * 命令执行器接口
 *
 * @author Yeaury 2025/5/26
 */
public interface CommandExecutor {

    default Map<String, Object> executeSync(String commandLine, long timeout) {
        return executeSync(commandLine, timeout, null, null, null);
    }

    default Map<String, Object> executeSync(String commandLine, Object authSubject) {
        return executeSync(commandLine, 30000L, null, authSubject, null);
    }

    /**
     * 同步执行命令，支持指定 userId
     *
     * @param commandLine 命令行
     * @param authSubject 认证主体
     * @param userId 用户 ID，用于统计上报
     * @return 执行结果
     */
    default Map<String, Object> executeSync(String commandLine, Object authSubject, String userId) {
        return executeSync(commandLine, 30000L, null, authSubject, userId);
    }

    /**
     * 同步执行命令
     *
     * @param commandLine 命令行
     * @param timeout 超时时间
     * @param sessionId session ID，如果为null则创建临时session
     * @param authSubject 认证主体，如果不为null则应用到session
     * @param userId 用户 ID，用于统计上报
     * @return 执行结果
     */
    Map<String, Object> executeSync(String commandLine, long timeout, String sessionId, Object authSubject, String userId);

    Map<String, Object> executeAsync(String commandLine, String sessionId);

    Map<String, Object> pullResults(String sessionId, String consumerId);

    Map<String, Object> interruptJob(String sessionId);

    Map<String, Object> createSession();

    Map<String, Object> closeSession(String sessionId);

    void setSessionAuth(String sessionId, Object authSubject);

    /**
     * 设置 session 的 userId
     *
     * @param sessionId session ID
     * @param userId 用户 ID
     */
    void setSessionUserId(String sessionId, String userId);
}


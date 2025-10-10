package com.taobao.arthas.mcp.server;

import java.util.Map;

/**
 * 命令执行器接口
 *
 * @author Yeaury 2025/5/26
 */
public interface CommandExecutor {

    default Map<String, Object> executeSync(String commandLine, long timeout) {
        return executeSync(commandLine, timeout, null, null);
    }

    default Map<String, Object> executeSync(String commandLine, Object authSubject) {
        return executeSync(commandLine, 30000L, null, authSubject);
    }

    Map<String, Object> executeSync(String commandLine, long timeout, String sessionId, Object authSubject);

    Map<String, Object> executeAsync(String commandLine, String sessionId);

    Map<String, Object> pullResults(String sessionId, String consumerId);

    Map<String, Object> interruptJob(String sessionId);

    Map<String, Object> createSession();

    Map<String, Object> closeSession(String sessionId);

    void setSessionAuth(String sessionId, Object authSubject);
}


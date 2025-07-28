package com.taobao.arthas.mcp.server.tool.function;

import com.taobao.arthas.mcp.server.ArthasMcpBootstrap;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ArthasCommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ArthasCommandExecutor.class);
    private static final long DEFAULT_TIMEOUT = 15000; // 默认超时时间15秒

    /**
     * Execute Arthas commands
     * @param command command string
     * @return Execution result as JSON string
     */
    public static String executeCommand(String command) {
        return executeCommand(command, DEFAULT_TIMEOUT);
    }

    /**
     * Execute Arthas commands
     * @param command command string
     * @param timeout timeout (ms)
     * @return Execution result as JSON string
     */
    public static String executeCommand(String command, long timeout) {
        try {
            ArthasMcpBootstrap bootstrap = ArthasMcpBootstrap.getInstance();
            if (bootstrap == null) {
                throw new IllegalStateException("ArthasMcpBootstrap not initialized");
            }

            CommandExecutor executor = bootstrap.getCommandExecutor();
            if (executor == null) {
                throw new IllegalStateException("CommandExecutor not initialized");
            }

            Map<String, Object> result = executor.execute(command, timeout);
            return JsonParser.toJson(result);
        } catch (Exception e) {
            logger.error("Failed to execute command: {}", command, e);
            return JsonParser.toJson("Error executing command '" + command + "': " + e.getMessage());
        }
    }

}

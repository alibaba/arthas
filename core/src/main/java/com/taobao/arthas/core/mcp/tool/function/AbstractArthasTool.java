package com.taobao.arthas.core.mcp.tool.function;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.core.mcp.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.taobao.arthas.core.mcp.tool.util.McpToolUtils.*;
import static com.taobao.arthas.core.mcp.tool.function.StreamableToolUtils.*;

/**
 * Arthas工具的抽象基类
 */
public abstract class AbstractArthasTool {
    
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int DEFAULT_TIMEOUT_SECONDS = (int) (StreamableToolUtils.DEFAULT_TIMEOUT_MS / 1000);

    private static final long DEFAULT_ASYNC_START_RETRY_INTERVAL_MS = 100L;
    private static final long DEFAULT_ASYNC_START_MAX_WAIT_MS = 3000L;
    
    /**
     * 工具执行上下文，包含所有必要的上下文信息
     */
    protected static class ToolExecutionContext {
        private final ArthasCommandContext commandContext;
        private final McpTransportContext mcpTransportContext;
        private final Object authSubject;
        private final String userId;
        private final McpNettyServerExchange exchange;
        private final String progressToken;
        private final boolean isStreamable;
        
        public ToolExecutionContext(ToolContext toolContext, boolean isStreamable) {
            this.commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
            this.isStreamable = isStreamable;
            
            // 尝试获取 Exchange (在 Stateless 模式下为 null)
            this.exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
            
            // 尝试获取 Progress Token
            Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
            this.progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;
            
            // 尝试获取 Transport Context (在 Stateless 模式下可能为 null)
            this.mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
            
            // 从 Transport Context 中提取认证信息
            if (this.mcpTransportContext != null) {
                this.authSubject = mcpTransportContext.get(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY);
                this.userId = (String) mcpTransportContext.get(McpAuthExtractor.MCP_USER_ID_KEY);
            } else {
                this.authSubject = null;
                this.userId = null;
            }
        }
        
        public ArthasCommandContext getCommandContext() {
            return commandContext;
        }
        
        public McpTransportContext getMcpTransportContext() {
            return mcpTransportContext;
        }
        
        public Object getAuthSubject() {
            return authSubject;
        }
        
        /**
         * 获取用户 ID
         * @return 用户 ID，如果未设置则返回 null
         */
        public String getUserId() {
            return userId;
        }
        
        public McpNettyServerExchange getExchange() {
            return exchange;
        }
        
        public String getProgressToken() {
            return progressToken;
        }
        
        public boolean isStreamable() {
            return isStreamable;
        }
    }

    protected String executeSync(ToolContext toolContext, String commandStr) {
        try {
            ToolExecutionContext execContext = new ToolExecutionContext(toolContext, false);
            // 使用带 userId 参数的 executeSync 方法
            Object result = execContext.getCommandContext().executeSync(
                    commandStr, 
                    execContext.getAuthSubject(),
                    execContext.getUserId()
            );
            return JsonParser.toJson(result);
        } catch (Exception e) {
            logger.error("Error executing sync command: {}", commandStr, e);
            return JsonParser.toJson(createErrorResponse("Error executing command: " + e.getMessage()));
        }
    }

    protected String executeStreamable(ToolContext toolContext, String commandStr, 
                                     Integer expectedResultCount, Integer pollIntervalMs, 
                                     Integer timeoutMs,
                                     String successMessage) {
        ToolExecutionContext execContext = null;
        try {
            execContext = new ToolExecutionContext(toolContext, true);
            
            logger.info("Starting streamable execution: {}", commandStr);

            // Set userId to session before async execution for stat reporting
            if (execContext.getUserId() != null) {
                execContext.getCommandContext().setSessionUserId(execContext.getUserId());
            }

            Map<String, Object> asyncResult = executeAsyncWithRetry(execContext, commandStr, timeoutMs);
            if (!isAsyncExecutionStarted(asyncResult)) {
                String errorMessage = asyncResult != null ? String.valueOf(asyncResult.get("error")) : "unknown error";
                return JsonParser.toJson(createErrorResponse("Failed to start command: " + errorMessage));
            }
            logger.debug("Async execution started: {}", asyncResult);

            Map<String, Object> results = executeAndCollectResults(
                execContext.getExchange(), 
                execContext.getCommandContext(), 
                expectedResultCount, 
                pollIntervalMs, 
                timeoutMs,
                execContext.getProgressToken()
            );
            
            if (results != null) {
                String message = successMessage != null ? successMessage : "Command execution completed successfully";

                if (Boolean.TRUE.equals(results.get("timedOut"))) {
                    Integer count = (Integer) results.get("resultCount");
                    if (count != null && count > 0) {
                        message = "Command execution ended (Timed out). Captured " + count + " results.";
                    } else {
                        message = "Command execution ended (Timed out). No results captured within the time limit.";
                    }
                }
                
                return JsonParser.toJson(createCompletedResponse(message, results));
            } else {
                return JsonParser.toJson(createErrorResponse("Command execution failed due to timeout or error limits exceeded"));
            }
            
        } catch (Exception e) {
            logger.error("Error executing streamable command: {}", commandStr, e);
            return JsonParser.toJson(createErrorResponse("Error executing command: " + e.getMessage()));
        } finally {
            if (execContext != null) {
                try {
                    // 确保前台任务被及时释放，避免占用 session 影响后续 streamable 工具执行
                    execContext.getCommandContext().interruptJob();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean isAsyncExecutionStarted(Map<String, Object> asyncResult) {
        if (asyncResult == null) {
            return false;
        }
        Object success = asyncResult.get("success");
        return Boolean.TRUE.equals(success);
    }

    private static boolean isRetryableAsyncStartError(Map<String, Object> asyncResult) {
        if (asyncResult == null) {
            return false;
        }
        Object success = asyncResult.get("success");
        if (Boolean.TRUE.equals(success)) {
            return false;
        }
        Object error = asyncResult.get("error");
        if (error == null) {
            return false;
        }
        String message = String.valueOf(error);
        return message.contains("Another job is running") || message.contains("Another command is executing");
    }

    private static Map<String, Object> executeAsyncWithRetry(ToolExecutionContext execContext, String commandStr, Integer timeoutMs) {
        long maxWaitMs = DEFAULT_ASYNC_START_MAX_WAIT_MS;
        if (timeoutMs != null && timeoutMs > 0) {
            maxWaitMs = Math.min(maxWaitMs, timeoutMs);
        }

        long deadline = System.currentTimeMillis() + maxWaitMs;
        Map<String, Object> asyncResult = null;

        while (System.currentTimeMillis() < deadline) {
            asyncResult = execContext.getCommandContext().executeAsync(commandStr);
            if (isAsyncExecutionStarted(asyncResult)) {
                return asyncResult;
            }

            if (isRetryableAsyncStartError(asyncResult)) {
                try {
                    execContext.getCommandContext().interruptJob();
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(DEFAULT_ASYNC_START_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return asyncResult;
                }
                continue;
            }

            return asyncResult;
        }

        return asyncResult;
    }

    protected StringBuilder buildCommand(String baseCommand) {
        return new StringBuilder(baseCommand);
    }

    protected void addParameter(StringBuilder cmd, String flag, String value) {
        if (value != null && !value.trim().isEmpty()) {
            cmd.append(" ").append(flag).append(" ").append(value.trim());
        }
    }

    protected void addParameter(StringBuilder cmd, String value) {
        if (value != null && !value.trim().isEmpty()) {
            // Safely quote the value to prevent command injection
            cmd.append(" '").append(value.trim().replace("'", "'\\''")).append("'");
        }
    }

    protected void addFlag(StringBuilder cmd, String flag, Boolean condition) {
        if (Boolean.TRUE.equals(condition)) {
            cmd.append(" ").append(flag);
        }
    }
    
    /**
     * 添加引用参数到命令中（用于包含空格的参数）
     */
    protected void addQuotedParameter(StringBuilder cmd, String value) {
        if (value != null && !value.trim().isEmpty()) {
            cmd.append(" '").append(value.trim()).append("'");
        }
    }

    protected int getDefaultValue(Integer value, int defaultValue) {
        return (value != null && value > 0) ? value : defaultValue;
    }

    protected String getDefaultValue(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }
}

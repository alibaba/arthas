package com.taobao.arthas.mcp.server.tool.function;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.util.JsonParser;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.*;
import static com.taobao.arthas.mcp.server.tool.function.StreamableToolUtils.*;

/**
 * Arthas工具的抽象基类
 */
public abstract class AbstractArthasTool {
    
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
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
            
            if (isStreamable) {
                this.exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
                Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
                this.progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;
                this.mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
                this.authSubject = mcpTransportContext != null ? mcpTransportContext.get(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY) : null;
                this.userId = mcpTransportContext != null ? (String) mcpTransportContext.get(McpAuthExtractor.MCP_USER_ID_KEY) : null;
            } else {
                this.mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
                this.authSubject = mcpTransportContext != null ? mcpTransportContext.get(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY) : null;
                // 从 McpTransportContext 中获取 userId
                this.userId = mcpTransportContext != null ? (String) mcpTransportContext.get(McpAuthExtractor.MCP_USER_ID_KEY) : null;
                this.exchange = null;
                this.progressToken = null;
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
                                     String successMessage) {
        try {
            ToolExecutionContext execContext = new ToolExecutionContext(toolContext, true);
            
            logger.info("Starting streamable execution: {}", commandStr);

            // Set userId to session before async execution for stat reporting
            if (execContext.getUserId() != null) {
                execContext.getCommandContext().setSessionUserId(execContext.getUserId());
            }

            Map<String, Object> asyncResult = execContext.getCommandContext().executeAsync(commandStr);
            logger.debug("Async execution started: {}", asyncResult);

            Map<String, Object> results = executeAndCollectResults(
                execContext.getExchange(), 
                execContext.getCommandContext(), 
                expectedResultCount, 
                pollIntervalMs, 
                execContext.getProgressToken()
            );
            
            if (results != null) {
                String message = successMessage != null ? successMessage : "Command execution completed successfully";
                return JsonParser.toJson(createCompletedResponse(message, results));
            } else {
                return JsonParser.toJson(createErrorResponse("Command execution failed due to timeout or error limits exceeded"));
            }
            
        } catch (Exception e) {
            logger.error("Error executing streamable command: {}", commandStr, e);
            return JsonParser.toJson(createErrorResponse("Error executing command: " + e.getMessage()));
        }
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

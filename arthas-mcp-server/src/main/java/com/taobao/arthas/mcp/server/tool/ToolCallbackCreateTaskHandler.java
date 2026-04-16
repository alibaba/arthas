package com.taobao.arthas.mcp.server.tool;

import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.task.CreateTaskContext;
import com.taobao.arthas.mcp.server.task.CreateTaskHandler;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.taobao.arthas.mcp.server.tool.ToolContextKeys.*;

/**
 * 将 ToolCallback 适配为 CreateTaskHandler 的通用适配器。
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/tasks">MCP Tasks Specification</a>
 */
public class ToolCallbackCreateTaskHandler implements CreateTaskHandler {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallbackCreateTaskHandler.class);
    
    private final ToolCallback toolCallback;

    public ToolCallbackCreateTaskHandler(ToolCallback toolCallback) {
        this.toolCallback = toolCallback;
    }

    @Override
    public CompletableFuture<McpSchema.CreateTaskResult> createTask(
            Map<String, Object> args,
            CreateTaskContext context) {
        
        logger.debug("Creating task for tool: {}", toolCallback.getToolDefinition().getName());

        return context.createTask(opts -> {
            // 使用默认配置，工具可以通过注解自定义 pollInterval 等
        }).thenCompose(task -> {
            String taskId = task.getTaskId();
            
            logger.info("Task created: {}, starting async tool execution", taskId);

            CompletableFuture.runAsync(() -> {
                executeToolAndUpdateTaskStatus(taskId, args, context);
            });

            return CompletableFuture.completedFuture(
                new McpSchema.CreateTaskResult(task, null)
            );
        });
    }

    /**
     * 在后台执行工具并更新任务状态。
     * 
     * @param taskId 任务 ID
     * @param args 工具参数
     * @param context 任务上下文
     */
    private void executeToolAndUpdateTaskStatus(String taskId, Map<String, Object> args, CreateTaskContext context) {
        ArthasCommandContext isolatedContext = null;
        try {
            // 执行前检查任务是否已被取消
            Boolean alreadyCancelled = context.isCancellationRequested(taskId).join();
            if (Boolean.TRUE.equals(alreadyCancelled)) {
                logger.info("Task {} was cancelled before execution started, skipping", taskId);
                return;
            }

            logger.debug("Executing tool: {} for task: {}",
                toolCallback.getToolDefinition().getName(), taskId);

            // 为 task 创建独立的 session
            isolatedContext = context.createIsolatedTaskSession(taskId);
            logger.debug("Created isolated session for task: {}, arthasSessionId: {}",
                       taskId, isolatedContext.getArthasSessionId());

            // 使用独立的 context 构建工具上下文
            ToolContext enhancedContext = buildEnhancedToolContext(taskId, context, isolatedContext);

            // 调用工具方法（工具内部的轮询循环会检查取消状态）
            String toolInput = JsonParser.toJson(args);
            String resultJson = toolCallback.call(toolInput, enhancedContext);

            // 执行完成后再次检查取消状态
            Boolean cancelledAfter = context.isCancellationRequested(taskId).join();
            if (Boolean.TRUE.equals(cancelledAfter)) {
                logger.info("Task {} was cancelled during execution, interrupting job", taskId);
                interruptJob(isolatedContext);
                return;
            }

            // 解析结果为 CallToolResult
            McpSchema.CallToolResult result = parseToolResult(resultJson);

            // 检查工具返回的结果是否标记为 cancelled（由 StreamableToolUtils 设置）
            if (isResultCancelled(resultJson)) {
                logger.info("Task {} execution detected cancellation, interrupting job", taskId);
                interruptJob(isolatedContext);
                return;
            }

            // 根据结果类型更新任务状态
            if (Boolean.TRUE.equals(result.getIsError())) {
                // 工具返回错误结果，标记任务为失败
                String errorMessage = extractErrorMessage(result);
                context.failTask(taskId, new McpSchema.CallToolResult(errorMessage, true, null))
                    .exceptionally(ex -> {
                        logger.error("Failed to mark task as failed: {}", taskId, ex);
                        return null;
                    });
                logger.warn("Tool execution returned error for task: {}", taskId);
            } else {
                // 工具执行成功，完成任务
                context.completeTask(taskId, result)
                    .thenRun(() -> {
                        logger.info("Task completed successfully: {}", taskId);
                    })
                    .exceptionally(ex -> {
                        logger.error("Failed to update task completion: {}", taskId, ex);
                        return null;
                    });
            }

        } catch (Exception e) {
            logger.error("Tool execution failed for task: {}", taskId, e);

            // 标记任务失败（如果任务已被取消，updateTaskStatus 会静默忽略终态任务）
            context.failTask(taskId, new McpSchema.CallToolResult("Tool execution failed: " + e.getMessage(), true, null))
                .exceptionally(ex -> {
                    logger.error("Failed to update task failure: {}", taskId, ex);
                    return null;
                });
        } finally {
            // 清理独立的 session
            cleanupTaskSession(taskId, context);
        }
    }

    private void interruptJob(ArthasCommandContext commandContext) {
        try {
            if (commandContext != null) {
                commandContext.interruptJob();
            }
        } catch (Exception e) {
            logger.warn("Failed to interrupt job: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isResultCancelled(String resultJson) {
        try {
            Map<String, Object> resultMap = JsonParser.fromJson(resultJson, Map.class);
            return Boolean.TRUE.equals(resultMap.get("cancelled"));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 清理 task 的独立 session。
     */
    private void cleanupTaskSession(String taskId, CreateTaskContext context) {
        try {
            context.cleanupTaskSession(taskId);
            logger.debug("Cleaned up task session: {}", taskId);
        } catch (Exception e) {
            logger.warn("Failed to cleanup task session: {}, error={}", taskId, e.getMessage());
        }
    }

    /**
     * 构建增强的 ToolContext，用于 task 执行。
     */
    private ToolContext buildEnhancedToolContext(
            String taskId, 
            CreateTaskContext context, 
            ArthasCommandContext isolatedContext) {
        
        Map<String, Object> contextMap = new HashMap<>();

        contextMap.put(CREATE_TASK_CONTEXT, context);
        contextMap.put(TASK_ID, taskId);

        contextMap.put(COMMAND_CONTEXT, isolatedContext);

        if (context.exchange() != null) {
            contextMap.put(EXCHANGE, context.exchange());

            McpTransportContext transportContext = context.exchange().getTransportContext();
            if (transportContext != null) {
                contextMap.put(MCP_TRANSPORT_CONTEXT, transportContext);
            }
        }
        
        return new ToolContext(contextMap);
    }

    private McpSchema.CallToolResult parseToolResult(String resultJson) {
        try {
            Map<String, Object> resultMap = JsonParser.fromJson(resultJson, Map.class);

            if (resultMap.containsKey("content")) {
                return JsonParser.fromJson(resultJson, McpSchema.CallToolResult.class);
            }

            McpSchema.TextContent textContent = new McpSchema.TextContent(resultJson);
            return new McpSchema.CallToolResult(
                java.util.Collections.singletonList(textContent),
                false,
                null
            );
            
        } catch (Exception e) {
            logger.debug("Failed to parse tool result as JSON, treating as plain text", e);
            
            McpSchema.TextContent textContent = new McpSchema.TextContent(resultJson);
            return new McpSchema.CallToolResult(
                java.util.Collections.singletonList(textContent),
                false,
                null
            );
        }
    }

    private String extractErrorMessage(McpSchema.CallToolResult result) {
        if (result.getContent() != null && !result.getContent().isEmpty()) {
            McpSchema.Content firstContent = result.getContent().get(0);
            if (firstContent instanceof McpSchema.TextContent) {
                return ((McpSchema.TextContent) firstContent).getText();
            }
        }
        return "Tool execution failed";
    }
}

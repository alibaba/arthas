package com.taobao.arthas.mcp.server.tool.function.monitor200;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.*;
import static com.taobao.arthas.mcp.server.tool.function.StreamableToolUtils.*;

public class StackTool {

    private static final Logger logger = LoggerFactory.getLogger(StackTool.class);

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 5;
    public static final int DEFAULT_POLL_INTERVAL_MS = 50;

    /**
     * stack 调用堆栈跟踪工具
     * 输出当前方法被调用的调用路径
     * 支持:
     * - classPattern: 类名表达式匹配，支持通配符
     * - methodPattern: 方法名表达式匹配，支持通配符 
     * - condition: OGNL条件表达式，满足条件的调用才会被跟踪
     * - numberOfExecutions: 捕获次数限制，达到指定次数后自动停止
     * - regex: 是否开启正则表达式匹配，默认false
     * - excludeClassPattern: 排除的类名模式
     */
    @Tool(
        name = "stack",
        description = "Stack 调用堆栈跟踪工具: 输出当前方法被调用的调用路径，帮助分析方法的调用链路。对应 Arthas 的 stack 命令。",
        streamable = true
    )
    public String stack(
            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame")
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors", required = false)
            String methodPattern,

            @ToolParam(description = "OGNL条件表达式，满足条件的调用才会被跟踪，如params[0]<0", required = false)
            String condition,

            @ToolParam(description = "捕获次数限制，默认值为5。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "排除的类名模式，支持通配符", required = false)
            String excludeClassPattern,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Integer progressToken = (Integer) toolContext.getContext().get(PROGRESS_TOKEN);

        int execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;

        try {
            StringBuilder cmd = new StringBuilder("stack");

            cmd.append(" -n ").append(execCount);

            if (Boolean.TRUE.equals(regex)) {
                cmd.append(" -E");
            }

            if (excludeClassPattern != null && !excludeClassPattern.trim().isEmpty()) {
                cmd.append(" --exclude-class-pattern '").append(excludeClassPattern.trim()).append("'");
            }

            if (classPattern != null && !classPattern.trim().isEmpty()) {
                cmd.append(" ").append(classPattern.trim());
            } else {
                throw new IllegalArgumentException("classPattern is required");
            }

            if (methodPattern != null && !methodPattern.trim().isEmpty()) {
                cmd.append(" ").append(methodPattern.trim());
            }

            if (condition != null && !condition.trim().isEmpty()) {
                cmd.append(" '").append(condition.trim()).append("'");
            }

            String commandStr = cmd.toString();
            logger.info("Starting stack execution: {}", commandStr);

            Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
            logger.debug("Async execution started: {}", asyncResult);

            // Stack命令是事件驱动的，使用较快的轮询间隔来及时获取堆栈信息
            boolean success = pullResultsSync(exchange, commandContext, execCount, DEFAULT_POLL_INTERVAL_MS, progressToken);
            if (success) {
                return JsonParser.toJson(createCompletedResponse("Stack execution completed successfully"));
            } else {
                return JsonParser.toJson(createErrorResponse("Stack execution failed due to timeout or error limits exceeded"));
            }

        } catch (Exception e) {
            logger.error("Error executing stack command", e);
            return JsonParser.toJson(createErrorResponse("Error executing stack: " + e.getMessage()));
        }
    }
}
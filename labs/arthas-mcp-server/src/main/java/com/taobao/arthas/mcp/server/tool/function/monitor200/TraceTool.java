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

public class TraceTool {

    private static final Logger logger = LoggerFactory.getLogger(TraceTool.class);

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_POLL_INTERVAL_MS = 100;
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;

    /**
     * trace 方法内部调用路径跟踪工具
     * 追踪方法内部调用路径，输出每个节点的耗时信息
     */
    @Tool(
        name = "trace",
        description = "Trace 方法内部调用路径跟踪工具: 追踪方法内部调用路径，输出每个节点的耗时信息，对应 Arthas 的 trace 命令。",
        streamable = true
    )
    public String trace(
            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame")
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors", required = false)
            String methodPattern,

            @ToolParam(description = "OGNL条件表达式，包括#cost耗时过滤，如'#cost>100'表示耗时超过100ms", required = false)
            String condition,

            @ToolParam(description = "执行次数限制，默认值为3。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "指定Class最大匹配数量，默认50", required = false)
            Integer maxMatchCount,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
        String progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;

        int execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;
        int maxMatch = (maxMatchCount != null && maxMatchCount > 0) ? maxMatchCount : DEFAULT_MAX_MATCH_COUNT;

        try {
            StringBuilder cmd = new StringBuilder("trace");

            cmd.append(" -n ").append(execCount);
            cmd.append(" -m ").append(maxMatch);

            if (Boolean.TRUE.equals(regex)) {
                cmd.append(" -E");
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
            logger.info("Starting trace execution: {}", commandStr);

            Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
            logger.debug("Async execution started: {}", asyncResult);

            Map<String, Object> results = executeAndCollectResults(exchange, commandContext, execCount, DEFAULT_POLL_INTERVAL_MS, progressToken);
            if (results != null) {
                return JsonParser.toJson(createCompletedResponse("Trace execution completed successfully", results));
            } else {
                return JsonParser.toJson(createErrorResponse("Trace execution failed due to timeout or error limits exceeded"));
            }

        } catch (Exception e) {
            logger.error("Error executing trace command", e);
            return JsonParser.toJson(createErrorResponse("Error executing trace: " + e.getMessage()));
        }
    }
}

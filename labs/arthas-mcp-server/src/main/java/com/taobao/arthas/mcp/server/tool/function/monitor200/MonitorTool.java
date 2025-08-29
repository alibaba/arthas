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

public class MonitorTool {

    private static final Logger logger = LoggerFactory.getLogger(MonitorTool.class);

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;

    /**
     * monitor 方法调用监控工具
     * 实时监控指定类的指定方法的调用情况
     * 支持:
     * - classPattern: 类名表达式匹配，支持通配符
     * - methodPattern: 方法名表达式匹配，支持通配符
     * - condition: OGNL条件表达式，满足条件的调用才会被监控
     * - intervalMs: 监控统计输出间隔，默认3000ms
     * - numberOfExecutions: 监控执行次数，默认3次
     * - regex: 是否开启正则表达式匹配，默认false
     * - excludeClassPattern: 排除的类名模式
     * - maxMatch: 最大匹配类数量，防止匹配过多类影响性能，默认50
     */
    @Tool(
        name = "monitor",
        description = "Monitor 方法调用监控工具: 实时监控指定类的指定方法的调用情况，包括调用次数、成功次数、失败次数、平均RT、失败率等统计信息。对应 Arthas 的 monitor 命令。",
        streamable = true
    )
    public String monitor(
            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame")
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors", required = false)
            String methodPattern,

            @ToolParam(description = "OGNL条件表达式，满足条件的调用才会被监控，如params[0]<0", required = false)
            String condition,

            @ToolParam(description = "监控统计输出间隔，单位为毫秒，默认 3000ms。用于控制输出频率", required = false)
            Integer intervalMs,

            @ToolParam(description = "执行次数限制，默认值为3。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "最大匹配类数量，防止匹配过多类影响性能，默认50", required = false)
            Integer maxMatch,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
        String progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;

        int interval = (intervalMs != null && intervalMs > 0) ? intervalMs : DEFAULT_REFRESH_INTERVAL_MS;
        int execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;
        int maxMatchCount = (maxMatch != null && maxMatch > 0) ? maxMatch : DEFAULT_MAX_MATCH_COUNT;

        try {
            StringBuilder cmd = new StringBuilder("monitor");

            // -c 指定统计周期，默认值为 60 秒
            cmd.append(" -c ").append(interval / 1000);

            cmd.append(" -n ").append(execCount);

            cmd.append(" -m ").append(maxMatchCount);

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
            logger.info("Starting monitor execution: {}", commandStr);

            Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
            logger.debug("Async execution started: {}", asyncResult);

            Map<String, Object> results = executeAndCollectResults(exchange, commandContext, execCount, interval / 10, progressToken);
            if (results != null) {
                return JsonParser.toJson(createCompletedResponse("Monitor execution completed successfully", results));
            } else {
                return JsonParser.toJson(createErrorResponse("Monitor execution failed due to timeout or error limits exceeded"));
            }

        } catch (Exception e) {
            logger.error("Error executing monitor command", e);
            return JsonParser.toJson(createErrorResponse("Error executing monitor: " + e.getMessage()));
        }
    }
}

package com.taobao.arthas.mcp.server.tool.function.jvm300;

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

public class DashboardTool {

    private static final Logger logger = LoggerFactory.getLogger(DashboardTool.class);

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;

    /**
     * dashboard 实时面板命令
     * 支持:
     * - intervalMs: 刷新间隔，单位 ms，默认 3000ms
     * - count: 刷新次数限制，即 -n 参数；如果不指定则使用 DEFAULT_NUMBER_OF_EXECUTIONS (3次)
     */
    @Tool(
            name = "dashboard",
            description = "Dashboard 诊断工具: 实时展示 JVM/应用面板，可利用参数控制诊断次数与间隔。对应 Arthas 的 dashboard 命令。",
            streamable = true
    )
    public String dashboard(
            @ToolParam(description = "刷新间隔，单位为毫秒，默认 3000ms。用于控制输出频率", required = false)
            Integer intervalMs,

            @ToolParam(description = "执行次数限制，默认值为 3。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
        String progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;

        int interval = (intervalMs != null && intervalMs > 0) ? intervalMs : DEFAULT_REFRESH_INTERVAL_MS;
        int execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;

        try {
            StringBuilder cmd = new StringBuilder("dashboard");
            cmd.append(" -i ").append(interval);
            cmd.append(" -n ").append(execCount);
            String commandStr = cmd.toString();
            logger.info("Starting dashboard execution: {}", commandStr);

            // 如果只执行一次，使用同步执行
            if (execCount == 1) {
                logger.info("Executing sync dashboard command: {}", commandStr);
                Map<String, Object> result = commandContext.executeSync(commandStr);
                return JsonParser.toJson(result);
            } else {
                Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
                logger.debug("Async execution started: {}", asyncResult);
                
                Map<String, Object> results = executeAndCollectResults(exchange, commandContext, execCount, interval / 10, progressToken);
                if (results != null) {
                    return JsonParser.toJson(createCompletedResponse("Dashboard execution completed successfully", results));
                } else {
                    return JsonParser.toJson(createErrorResponse("Dashboard execution failed due to timeout or error limits exceeded"));
                }
            }

        } catch (Exception e) {
            logger.error("Error executing dashboard command", e);
            return JsonParser.toJson(createErrorResponse("Error executing dashboard: " + e.getMessage()));
        }
    }
}

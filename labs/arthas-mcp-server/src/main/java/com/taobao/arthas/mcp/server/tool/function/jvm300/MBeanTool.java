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

public class MBeanTool {

    private static final Logger logger = LoggerFactory.getLogger(MBeanTool.class);

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;

    /**
     * mbean 诊断工具: 查看或监控 MBean 属性
     * 支持:
     * - namePattern: MBean 名称表达式，支持通配符或正则（需开启 -E）
     * - attributePattern: 属性名称表达式，支持通配符或正则（需开启 -E）
     * - metadata: 是否查看元信息 (-m)
     * - intervalMs: 刷新属性值时间间隔 (ms) (-i)，required=false
     * - numberOfExecutions: 刷新次数 (-n)，若未指定或 <=0 则使用 DEFAULT_NUMBER_OF_EXECUTIONS
     * - regex: 是否启用正则匹配 (-E)，required=false
     */
    @Tool(
        name = "mbean",
        description = "MBean 诊断工具: 查看或监控 MBean 属性信息，对应 Arthas 的 mbean 命令。",
        streamable = true
    )
    public String mbean(
            @ToolParam(description = "MBean名称表达式匹配，如java.lang:type=GarbageCollector,name=*")
            String namePattern,

            @ToolParam(description = "属性名表达式匹配，支持通配符如CollectionCount", required = false)
            String attributePattern,

            @ToolParam(description = "是否查看元信息 (-m)", required = false)
            Boolean metadata,

            @ToolParam(description = "刷新间隔，单位为毫秒，默认 3000ms。用于控制输出频率", required = false)
            Integer intervalMs,

            @ToolParam(description = "执行次数限制，默认值为 3。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
        String progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;

        boolean needStreamOutput = (intervalMs != null && intervalMs > 0) || (numberOfExecutions != null && numberOfExecutions > 0);

        int interval = DEFAULT_REFRESH_INTERVAL_MS;
        int execCount = DEFAULT_NUMBER_OF_EXECUTIONS;
        
        if (needStreamOutput) {
            interval = (intervalMs != null && intervalMs > 0) ? intervalMs : DEFAULT_REFRESH_INTERVAL_MS;
            execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;
        }

        try {
            StringBuilder cmd = new StringBuilder("mbean");

            if (Boolean.TRUE.equals(metadata)) {
                cmd.append(" -m");
            }
            if (Boolean.TRUE.equals(regex)) {
                cmd.append(" -E");
            }
            
            // 只有在需要流式输出且不是查看元数据时才添加 -i 和 -n 参数
            if (needStreamOutput && !Boolean.TRUE.equals(metadata)) {
                cmd.append(" -i ").append(interval);
                cmd.append(" -n ").append(execCount);
            }
            
            if (namePattern != null && !namePattern.trim().isEmpty()) {
                cmd.append(" ").append(namePattern.trim());
            }
            if (attributePattern != null && !attributePattern.trim().isEmpty()) {
                cmd.append(" ").append(attributePattern.trim());
            }

            String commandStr = cmd.toString();
            logger.info("Starting mbean execution: {}", commandStr);

            // 使用同步执行的情况：查看元数据 或者 不需要流式输出
            if (Boolean.TRUE.equals(metadata) || !needStreamOutput) {
                logger.info("Executing sync mbean command: {}", commandStr);
                Map<String, Object> result = commandContext.executeSync(commandStr);
                return JsonParser.toJson(result);
            } else {
                Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
                logger.debug("Async execution started: {}", asyncResult);
                
                Map<String, Object> results = executeAndCollectResults(exchange, commandContext, execCount, interval / 10, progressToken);
                if (results != null) {
                    return JsonParser.toJson(createCompletedResponse("MBean execution completed successfully", results));
                } else {
                    return JsonParser.toJson(createErrorResponse("MBean execution failed due to timeout or error limits exceeded"));
                }
            }

        } catch (Exception e) {
            logger.error("Error executing mbean command", e);
            return JsonParser.toJson(createErrorResponse("Error executing mbean: " + e.getMessage()));
        }
    }
}

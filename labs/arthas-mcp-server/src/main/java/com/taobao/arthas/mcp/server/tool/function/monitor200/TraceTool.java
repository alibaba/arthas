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

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 5;
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

            @ToolParam(description = "执行次数限制，默认值为5。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "排除的类名模式，支持通配符", required = false)
            String excludeClassPattern,

            @ToolParam(description = "指定Class最大匹配数量，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "是否跳过JDK方法，默认true", required = false)
            Boolean skipJdkMethod,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHashcode,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "是否包含子类，默认为false", required = false)
            Boolean includeSubClass,

            @ToolParam(description = "监听耗时超过指定时间的调用，单位为毫秒", required = false)
            Integer costThreshold,

            @ToolParam(description = "输出结果属性遍历深度，默认为1", required = false)
            Integer expandLevel,

            @ToolParam(description = "只显示耗时大于等于指定时间的调用链路，单位为毫秒", required = false)
            Integer minCost,

            @ToolParam(description = "是否输出调用参数，默认为false", required = false)
            Boolean printParameters,

            @ToolParam(description = "是否输出返回值，默认为false", required = false)
            Boolean printReturnValue,

            @ToolParam(description = "是否输出异常信息，默认为true", required = false)
            Boolean printException,

            @ToolParam(description = "是否按调用耗时排序输出，默认为false", required = false)
            Boolean sortByTime,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Integer progressToken = (Integer) toolContext.getContext().get(PROGRESS_TOKEN);

        int execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;
        int maxMatch = (maxMatchCount != null && maxMatchCount > 0) ? maxMatchCount : DEFAULT_MAX_MATCH_COUNT;

        try {
            StringBuilder cmd = new StringBuilder("trace");

            cmd.append(" -n ").append(execCount);
            cmd.append(" -m ").append(maxMatch);

            if (Boolean.TRUE.equals(regex)) {
                cmd.append(" -E");
            }

            if (Boolean.FALSE.equals(skipJdkMethod)) {
                cmd.append(" --skipJDKMethod false");
            }

            if (classLoaderHashcode != null && !classLoaderHashcode.trim().isEmpty()) {
                cmd.append(" -c ").append(classLoaderHashcode.trim());
            } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
                cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
            }

            if (Boolean.TRUE.equals(includeSubClass)) {
                cmd.append(" --include-sub-class");
            }

            if (costThreshold != null && costThreshold > 0) {
                cmd.append(" --cost ").append(costThreshold);
            }

            if (expandLevel != null && expandLevel > 0) {
                cmd.append(" -x ").append(expandLevel);
            }

            if (minCost != null && minCost > 0) {
                cmd.append(" --min-cost ").append(minCost);
            }

            if (Boolean.TRUE.equals(printParameters)) {
                cmd.append(" -p");
            }

            if (Boolean.TRUE.equals(printReturnValue)) {
                cmd.append(" -r");
            }

            if (Boolean.FALSE.equals(printException)) {
                cmd.append(" --no-exception");
            }

            if (Boolean.TRUE.equals(sortByTime)) {
                cmd.append(" --sort-by-cost");
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
            logger.info("Starting trace execution: {}", commandStr);

            Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
            logger.debug("Async execution started: {}", asyncResult);

            // Trace命令是事件驱动的，使用较快的轮询间隔来及时获取调用路径信息
            boolean success = pullResultsSync(exchange, commandContext, execCount, DEFAULT_POLL_INTERVAL_MS, progressToken);
            if (success) {
                return JsonParser.toJson(createCompletedResponse("Trace execution completed successfully"));
            } else {
                return JsonParser.toJson(createErrorResponse("Trace execution failed due to timeout or error limits exceeded"));
            }

        } catch (Exception e) {
            logger.error("Error executing trace command", e);
            return JsonParser.toJson(createErrorResponse("Error executing trace: " + e.getMessage()));
        }
    }
}

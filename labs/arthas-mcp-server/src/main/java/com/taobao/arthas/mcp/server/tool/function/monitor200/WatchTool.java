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

public class WatchTool {

    private static final Logger logger = LoggerFactory.getLogger(WatchTool.class);

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_POLL_INTERVAL_MS = 50;
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;
    public static final int DEFAULT_EXPAND_LEVEL = 1;
    public static final String DEFAULT_EXPRESS = "{params, target, returnObj}";

    /**
     * watch 方法执行观察工具
     * 观察指定方法的调用情况，包括入参、返回值和抛出异常等信息
     * 整合了完整的参数支持和流式输出能力
     */
    @Tool(
        name = "watch",
        description = "Watch 方法执行观察工具: 观察指定方法的调用情况，包括入参、返回值和抛出异常等信息，支持实时流式输出。对应 Arthas 的 watch 命令。",
        streamable = true
    )
    public String watch(
            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame")
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors", required = false)
            String methodPattern,

            @ToolParam(description = "观察表达式，默认为{params, target, returnObj}，支持OGNL表达式", required = false)
            String express,

            @ToolParam(description = "OGNL条件表达式，满足条件的调用才会被观察，如params[0]<0", required = false)
            String condition,

            @ToolParam(description = "在方法调用之前观察(-b)，默认false", required = false)
            Boolean beforeMethod,

            @ToolParam(description = "在方法抛出异常后观察(-e)，默认false", required = false)
            Boolean exceptionOnly,

            @ToolParam(description = "在方法正常返回后观察(-s)，默认false", required = false)
            Boolean successOnly,

            @ToolParam(description = "执行次数限制，默认值为 3。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "排除的类名模式，支持通配符", required = false)
            String excludeClassPattern,

            @ToolParam(description = "指定Class最大匹配数量，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "指定输出结果的属性遍历深度，默认1，最大4", required = false)
            Integer expandLevel,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHashcode,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "是否包含子类，默认为false", required = false)
            Boolean includeSubClass,

            @ToolParam(description = "监听耗时超过指定时间的调用，单位为毫秒", required = false)
            Integer costThreshold,

            @ToolParam(description = "是否跳过JDK的方法，默认为true", required = false)
            Boolean skipJdkMethod,

            @ToolParam(description = "详细输出模式，显示调用的详细信息", required = false)
            Boolean verbose,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
        String progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;

        int execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;
        int maxMatch = (maxMatchCount != null && maxMatchCount > 0) ? maxMatchCount : DEFAULT_MAX_MATCH_COUNT;
        int expandDepth = (expandLevel != null && expandLevel >= 1 && expandLevel <= 4) ? expandLevel : DEFAULT_EXPAND_LEVEL;
        String watchExpress = (express != null && !express.trim().isEmpty()) ? express.trim() : DEFAULT_EXPRESS;

        try {
            StringBuilder cmd = new StringBuilder("watch");

            cmd.append(" -n ").append(execCount);
            cmd.append(" -m ").append(maxMatch);
            cmd.append(" -x ").append(expandDepth);

            if (Boolean.TRUE.equals(regex)) {
                cmd.append(" -E");
            }

            if (Boolean.TRUE.equals(beforeMethod)) {
                cmd.append(" -b");
            } else if (Boolean.TRUE.equals(exceptionOnly)) {
                cmd.append(" -e");
            } else if (Boolean.TRUE.equals(successOnly)) {
                cmd.append(" -s");
            } else {
                cmd.append(" -f");
            }

            if (classLoaderHashcode != null && !classLoaderHashcode.trim().isEmpty()) {
                cmd.append(" -c ").append(classLoaderHashcode.trim());
            } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
                cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
            }

            if (Boolean.TRUE.equals(includeSubClass)) {
                cmd.append(" --include-sub-class");
            }

            if (Boolean.FALSE.equals(skipJdkMethod)) {
                cmd.append(" --skipJDKMethod false");
            }

            if (costThreshold != null && costThreshold > 0) {
                cmd.append(" --cost ").append(costThreshold);
            }

            if (Boolean.TRUE.equals(verbose)) {
                cmd.append(" -v");
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

            cmd.append(" '").append(watchExpress).append("'");

            if (condition != null && !condition.trim().isEmpty()) {
                cmd.append(" '").append(condition.trim()).append("'");
            }

            String commandStr = cmd.toString();
            logger.info("Starting watch execution: {}", commandStr);

            Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
            logger.debug("Async execution started: {}", asyncResult);

            Map<String, Object> results = executeAndCollectResults(exchange, commandContext, execCount, DEFAULT_POLL_INTERVAL_MS, progressToken);
            if (results != null) {
                return JsonParser.toJson(createCompletedResponse("Watch execution completed successfully", results));
            } else {
                return JsonParser.toJson(createErrorResponse("Watch execution failed due to timeout or error limits exceeded"));
            }

        } catch (Exception e) {
            logger.error("Error executing watch command", e);
            return JsonParser.toJson(createErrorResponse("Error executing watch: " + e.getMessage()));
        }
    }
}

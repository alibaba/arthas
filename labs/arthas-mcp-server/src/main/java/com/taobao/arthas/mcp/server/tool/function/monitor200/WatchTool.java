package com.taobao.arthas.mcp.server.tool.function.monitor200;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class WatchTool extends AbstractArthasTool {

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

            @ToolParam(description = "指定Class最大匹配数量，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "指定输出结果的属性遍历深度，默认1，最大4", required = false)
            Integer expandLevel,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认200秒。超时后命令自动退出", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);
        int maxMatch = getDefaultValue(maxMatchCount, DEFAULT_MAX_MATCH_COUNT);
        int expandDepth = (expandLevel != null && expandLevel >= 1 && expandLevel <= 4) ? expandLevel : DEFAULT_EXPAND_LEVEL;
        String watchExpress = getDefaultValue(express, DEFAULT_EXPRESS);
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        StringBuilder cmd = buildCommand("watch");

        cmd.append(" --timeout ").append(timeoutSeconds);
        cmd.append(" -n ").append(execCount);
        cmd.append(" -m ").append(maxMatch);
        cmd.append(" -x ").append(expandDepth);

        addFlag(cmd, "-E", regex);

        if (Boolean.TRUE.equals(beforeMethod)) {
            cmd.append(" -b");
        } else if (Boolean.TRUE.equals(exceptionOnly)) {
            cmd.append(" -e");
        } else if (Boolean.TRUE.equals(successOnly)) {
            cmd.append(" -s");
        } else {
            cmd.append(" -f");
        }

        addParameter(cmd, classPattern);

        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            cmd.append(" ").append(methodPattern.trim());
        }

        addQuotedParameter(cmd, watchExpress);

        addQuotedParameter(cmd, condition);

        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS, timeoutSeconds * 1000,
                                "Watch execution completed successfully");
    }
}

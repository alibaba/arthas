package com.taobao.arthas.mcp.server.tool.function.monitor200;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class MonitorTool extends AbstractArthasTool {

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

            @ToolParam(description = "命令执行超时时间，单位为秒，默认200秒。超时后命令自动退出", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        int interval = getDefaultValue(intervalMs, DEFAULT_REFRESH_INTERVAL_MS);
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);
        int maxMatchCount = getDefaultValue(maxMatch, DEFAULT_MAX_MATCH_COUNT);
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        StringBuilder cmd = buildCommand("monitor");

        cmd.append(" --timeout ").append(timeoutSeconds);
        cmd.append(" -c ").append(interval / 1000);
        cmd.append(" -n ").append(execCount);
        cmd.append(" -m ").append(maxMatchCount);

        addFlag(cmd, "-E", regex);
        addParameter(cmd, classPattern);
        
        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            cmd.append(" ").append(methodPattern.trim());
        }
        
        addQuotedParameter(cmd, condition);

        return executeStreamable(toolContext, cmd.toString(), execCount, interval / 10, 
                                "Monitor execution completed successfully");
    }
}

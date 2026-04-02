package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * Monitor方法调用监控工具类
 *
 * 该类封装了Arthas的monitor命令，用于实时监控JVM中指定类的方法调用情况。
 通过此工具可以获取方法调用的统计信息，包括调用次数、成功次数、失败次数、平均响应时间、失败率等。
 *
 * 功能特性：
 * - 支持类名和方法名的通配符匹配
 * - 支持正则表达式匹配
 * - 支持OGNL条件表达式过滤
 * - 支持设置监控统计输出间隔
 * - 支持限制监控执行次数
 * - 支持设置最大匹配类数量，防止性能问题
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.command.monitor200.MonitorCommand
 */
public class MonitorTool extends AbstractArthasTool {

    /**
     * 默认的监控执行次数
     * 表示默认情况下只监控一次后自动停止
     */
    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;

    /**
     * 默认的监控统计输出间隔，单位为毫秒
     * 默认每3秒输出一次统计信息
     */
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;

    /**
     * 默认的最大匹配类数量
     * 防止匹配过多类导致性能问题，默认最多匹配50个类
     */
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;

    /**
     * monitor 方法调用监控工具
     * 实时监控指定类的指定方法的调用情况
     * 支持:
     * - classPattern: 类名表达式匹配，支持通配符
     * - methodPattern: 方法名表达式匹配，支持通配符
     * - condition: OGNL条件表达式，满足条件的调用才会被监控
     * - intervalMs: 监控统计输出间隔，默认3000ms
     * - numberOfExecutions: 监控执行次数，默认1次
     * - regex: 是否开启正则表达式匹配，默认false
     * - excludeClassPattern: 排除的类名模式
     * - maxMatch: 最大匹配类数量，防止匹配过多类影响性能，默认50
     */
    /**
     * 执行方法调用监控
     *
     * 该方法是Monitor工具的核心方法，用于构建并执行monitor命令。
     * 根据用户传入的参数，构建完整的命令字符串，并通过流式方式执行。
     *
     * 执行流程：
     * 1. 获取各个参数的默认值
     * 2. 构建命令基础部分："monitor"
     * 3. 添加超时、间隔、次数、最大匹配数等全局参数
     * 4. 添加正则表达式标志（如果启用）
     * 5. 添加类名模式
     * 6. 添加方法名模式（如果提供）
     * 7. 添加条件表达式（如果提供）
     * 8. 执行流式命令并返回结果
     *
     * @param classPattern 类名表达式，支持通配符匹配，如"demo.MathGame"或"demo.*"
     * @param methodPattern 方法名表达式，支持通配符匹配，如"primeFactors"，可选
     * @param condition OGNL条件表达式，满足条件的调用才会被监控，如"params[0]<0"，可选
     * @param intervalMs 监控统计输出间隔，单位为毫秒，默认3000ms
     * @param numberOfExecutions 执行次数限制，达到指定次数后自动停止，默认1次
     * @param regex 是否开启正则表达式匹配，默认false（使用通配符）
     * @param maxMatch 最大匹配类数量，防止匹配过多类影响性能，默认50
     * @param timeout 命令执行超时时间，单位为秒，默认60秒
     * @param toolContext 工具执行上下文，包含执行环境信息
     * @return 监控执行的输出结果
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

            @ToolParam(description = "执行次数限制，默认值为" + DEFAULT_NUMBER_OF_EXECUTIONS + "。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "最大匹配类数量，防止匹配过多类影响性能，默认50", required = false)
            Integer maxMatch,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认" + AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS +  "秒。超时后命令自动退出", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        // 获取各个参数的默认值，如果用户未指定则使用默认值
        int interval = getDefaultValue(intervalMs, DEFAULT_REFRESH_INTERVAL_MS);
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);
        int maxMatchCount = getDefaultValue(maxMatch, DEFAULT_MAX_MATCH_COUNT);
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        // 构建monitor命令的基础部分
        StringBuilder cmd = buildCommand("monitor");

        // 添加全局参数：超时时间（秒）
        cmd.append(" --timeout ").append(timeoutSeconds);
        // 添加监控统计输出间隔（秒），将毫秒转换为秒
        cmd.append(" -c ").append(interval / 1000);
        // 添加执行次数限制
        cmd.append(" -n ").append(execCount);
        // 添加最大匹配类数量
        cmd.append(" -m ").append(maxMatchCount);

        // 如果启用正则表达式，添加-E标志
        addFlag(cmd, "-E", regex);
        // 添加类名模式参数
        addParameter(cmd, classPattern);

        // 如果指定了方法名模式，添加到命令中
        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            cmd.append(" ").append(methodPattern.trim());
        }

        // 如果指定了条件表达式，添加到命令中（需要引号包围）
        addQuotedParameter(cmd, condition);

        // 执行流式命令并返回结果
        // 参数说明：
        // - execCount: 执行次数，用于判断是否完成
        // - interval / 10: 轮询间隔，设置为统计间隔的1/10，确保及时获取输出
        // - timeoutSeconds * 1000: 超时时间（毫秒）
        // - "Monitor execution completed successfully": 完成时的提示信息
        return executeStreamable(toolContext, cmd.toString(), execCount, interval / 10, timeoutSeconds * 1000,
                                "Monitor execution completed successfully");
    }
}

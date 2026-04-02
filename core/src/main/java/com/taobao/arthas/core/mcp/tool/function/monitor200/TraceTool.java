package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * Trace方法内部调用路径跟踪工具类
 * 该工具用于追踪方法内部调用路径，输出每个节点的耗时信息
 * 继承自AbstractArthasTool，提供trace命令的MCP工具接口
 *
 * @author Arthas
 * @see AbstractArthasTool
 */
public class TraceTool extends AbstractArthasTool {

    /**
     * 默认执行次数
     * 表示命令执行多少次后自动停止，默认值为1
     */
    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;

    /**
     * 默认轮询间隔时间（毫秒）
     * 用于流式输出时的轮询间隔，默认100毫秒
     */
    public static final int DEFAULT_POLL_INTERVAL_MS = 100;

    /**
     * 默认最大匹配数量
     * 指定Class的最大匹配数量，默认值为50
     */
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;

    /**
     * 执行trace方法调用路径追踪
     * 该方法会构建trace命令并执行，返回方法内部调用路径和每个节点的耗时信息
     * 支持流式输出，可以实时查看追踪结果
     *
     * @param classPattern 类名表达式匹配，支持通配符，如demo.MathGame
     * @param methodPattern 方法名表达式匹配，支持通配符，如primeFactors，可为空
     * @param condition OGNL条件表达式，包括#cost耗时过滤，如'#cost>100'表示耗时超过100ms，可为空
     * @param numberOfExecutions 执行次数限制，默认值为1。达到指定次数后自动停止
     * @param regex 开启正则表达式匹配，默认为通配符匹配，默认false
     * @param maxMatchCount 指定Class最大匹配数量，默认50
     * @param timeout 命令执行超时时间，单位为秒，默认300秒。超时后命令自动退出
     * @param toolContext 工具上下文对象，包含执行环境信息
     * @return trace命令的执行结果，包含方法调用路径和耗时信息
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

            @ToolParam(description = "执行次数限制，默认值为1。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "指定Class最大匹配数量，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认" + AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS +  "秒。超时后命令自动退出", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        // 获取执行次数，如果未指定则使用默认值1
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        // 获取最大匹配数量，如果未指定则使用默认值50
        int maxMatch = getDefaultValue(maxMatchCount, DEFAULT_MAX_MATCH_COUNT);

        // 获取超时时间，如果未指定则使用默认值300秒
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        // 构建trace命令的基础部分
        StringBuilder cmd = buildCommand("trace");

        // 添加超时参数
        cmd.append(" --timeout ").append(timeoutSeconds);

        // 添加执行次数参数（-n）
        cmd.append(" -n ").append(execCount);

        // 添加最大匹配数量参数（-m）
        cmd.append(" -m ").append(maxMatch);

        // 如果开启了正则表达式匹配，添加-E参数
        addFlag(cmd, "-E", regex);

        // 添加类名模式参数
        addParameter(cmd, classPattern);

        // 如果指定了方法名模式，添加方法名参数
        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            cmd.append(" ").append(methodPattern.trim());
        }

        // 如果指定了条件表达式，添加条件参数（需要引号包裹）
        addQuotedParameter(cmd, condition);

        // 执行流式命令，返回追踪结果
        // 参数说明：
        // - toolContext: 工具上下文
        // - cmd.toString(): 完整的命令字符串
        // - execCount: 执行次数
        // - DEFAULT_POLL_INTERVAL_MS: 轮询间隔100毫秒
        // - timeoutSeconds * 1000: 超时时间（毫秒）
        // - "Trace execution completed successfully": 成功完成消息
        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS, timeoutSeconds * 1000,
                                "Trace execution completed successfully");
    }
}

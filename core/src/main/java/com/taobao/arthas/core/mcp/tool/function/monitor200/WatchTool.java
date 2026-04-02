package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * Watch方法执行观察工具类
 * 该工具用于观察指定方法的调用情况，包括入参、返回值和抛出异常等信息
 * 支持条件过滤、时机选择和实时流式输出
 * 继承自AbstractArthasTool，提供watch命令的MCP工具接口
 *
 * @author Arthas
 * @see AbstractArthasTool
 */
public class WatchTool extends AbstractArthasTool {

    /**
     * 默认执行次数
     * 表示命令执行多少次后自动停止，默认值为1
     */
    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;

    /**
     * 默认轮询间隔时间（毫秒）
     * 用于流式输出时的轮询间隔，默认50毫秒
     */
    public static final int DEFAULT_POLL_INTERVAL_MS = 50;

    /**
     * 默认最大匹配数量
     * 指定Class的最大匹配数量，默认值为50
     */
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;

    /**
     * 默认展开层次
     * 指定输出结果的属性遍历深度，默认值为1
     */
    public static final int DEFAULT_EXPAND_LEVEL = 1;

    /**
     * 默认观察表达式
     * 用于指定观察的对象，默认包含参数、目标对象和返回值
     */
    public static final String DEFAULT_EXPRESS = "{params, target, returnObj}";

    /**
     * 执行watch方法执行观察
     * 该方法会构建watch命令并执行，用于观察指定方法的调用情况
     * 支持观察方法入参、返回值、异常等信息，支持条件过滤和实时流式输出
     *
     * @param classPattern 类名表达式匹配，支持通配符，如demo.MathGame
     * @param methodPattern 方法名表达式匹配，支持通配符，如primeFactors，可为空
     * @param express 观察表达式，默认为{params, target, returnObj}，支持OGNL表达式，可为空
     * @param condition OGNL条件表达式，满足条件的调用才会被观察，如params[0]<0，可为空
     * @param beforeMethod 在方法调用之前观察（-b），默认false
     * @param exceptionOnly 在方法抛出异常后观察（-e），默认false
     * @param successOnly 在方法正常返回后观察（-s），默认false
     * @param numberOfExecutions 执行次数限制，默认值为1。达到指定次数后自动停止
     * @param regex 开启正则表达式匹配，默认为通配符匹配，默认false
     * @param maxMatchCount 指定Class最大匹配数量，默认50
     * @param expandLevel 指定输出结果的属性遍历深度，默认1，最大4
     * @param sizeLimit 输出结果大小上限（字节），对应watch -M/--sizeLimit，默认 10 * 1024 * 1024
     * @param timeout 命令执行超时时间，单位为秒，默认300秒。超时后命令自动退出
     * @param toolContext 工具上下文对象，包含执行环境信息
     * @return watch命令的执行结果，包含观察到的方法调用信息
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

            @ToolParam(description = "执行次数限制，默认值为 1。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "指定Class最大匹配数量，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "指定输出结果的属性遍历深度，默认1，最大4", required = false)
            Integer expandLevel,

            @ToolParam(description = "输出结果大小上限(字节)。对应 watch -M/--sizeLimit，默认 10 * 1024 * 1024", required = false)
            Integer sizeLimit,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认" + AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS +  "秒。超时后命令自动退出", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        // 获取执行次数，如果未指定则使用默认值1
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        // 获取最大匹配数量，如果未指定则使用默认值50
        int maxMatch = getDefaultValue(maxMatchCount, DEFAULT_MAX_MATCH_COUNT);

        // 获取展开层次，必须在1-4之间，否则使用默认值1
        int expandDepth = (expandLevel != null && expandLevel >= 1 && expandLevel <= 4) ? expandLevel : DEFAULT_EXPAND_LEVEL;

        // 获取观察表达式，如果未指定则使用默认值{params, target, returnObj}
        String watchExpress = getDefaultValue(express, DEFAULT_EXPRESS);

        // 获取超时时间，如果未指定则使用默认值300秒
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        // 构建watch命令的基础部分
        StringBuilder cmd = buildCommand("watch");

        // 添加超时参数（--timeout）
        cmd.append(" --timeout ").append(timeoutSeconds);

        // 添加执行次数参数（-n）
        cmd.append(" -n ").append(execCount);

        // 添加最大匹配数量参数（-m）
        cmd.append(" -m ").append(maxMatch);

        // 添加展开层次参数（-x）
        cmd.append(" -x ").append(expandDepth);

        // 如果指定了输出结果大小上限，添加-M参数
        if (sizeLimit != null && sizeLimit > 0) {
            cmd.append(" -M ").append(sizeLimit);
        }

        // 如果开启了正则表达式匹配，添加-E参数
        addFlag(cmd, "-E", regex);

        // 添加观察时机参数
        // -b: 在方法调用之前观察
        // -e: 在方法抛出异常后观察
        // -s: 在方法正常返回后观察
        // -f: 在方法正常返回后观察（默认，与-s相同）
        if (Boolean.TRUE.equals(beforeMethod)) {
            cmd.append(" -b");
        } else if (Boolean.TRUE.equals(exceptionOnly)) {
            cmd.append(" -e");
        } else if (Boolean.TRUE.equals(successOnly)) {
            cmd.append(" -s");
        } else {
            cmd.append(" -f");
        }

        // 添加类名模式参数
        addParameter(cmd, classPattern);

        // 如果指定了方法名模式，添加方法名参数
        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            cmd.append(" ").append(methodPattern.trim());
        }

        // 添加观察表达式参数（需要引号包裹）
        addQuotedParameter(cmd, watchExpress);

        // 如果指定了条件表达式，添加条件参数（需要引号包裹）
        addQuotedParameter(cmd, condition);

        // 执行流式命令，返回观察结果
        // 参数说明：
        // - toolContext: 工具上下文
        // - cmd.toString(): 完整的命令字符串
        // - execCount: 执行次数
        // - DEFAULT_POLL_INTERVAL_MS: 轮询间隔50毫秒
        // - timeoutSeconds * 1000: 超时时间（毫秒）
        // - "Watch execution completed successfully": 成功完成消息
        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS, timeoutSeconds * 1000,
                                "Watch execution completed successfully");
    }
}

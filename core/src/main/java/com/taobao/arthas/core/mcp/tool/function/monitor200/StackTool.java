package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 调用堆栈跟踪工具（Stack Tool）
 * 对应 Arthas 的 stack 命令
 *
 * 功能说明：
 * 输出当前方法被调用的调用路径，帮助分析方法的调用链路
 * 支持条件表达式过滤，只在满足条件时才输出调用堆栈
 *
 * 使用场景：
 * - 查看方法是被哪里调用的
 * - 分析方法调用链路
 * - 排查方法调用来源问题
 * - 结合条件表达式，只在特定条件下查看调用栈
 */
public class StackTool extends AbstractArthasTool {

    /**
     * 默认捕获次数
     * 默认只捕获 1 次方法调用后就停止
     */
    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;

    /**
     * 默认轮询间隔（毫秒）
     * 每隔 50ms 检查一次命令执行状态
     */
    public static final int DEFAULT_POLL_INTERVAL_MS = 50;

    /**
     * 调用堆栈跟踪方法
     *
     * 对应 Arthas 命令：stack [options] classPattern methodPattern [condition]
     *
     * 功能：
     * - 输出方法被调用的调用路径
     * - 支持通配符和正则表达式匹配类名和方法名
     * - 支持条件表达式过滤
     * - 可设置捕获次数限制
     *
     * @param classPattern 类名表达式匹配
     *                     支持通配符，如 demo.MathGame
     *
     * @param methodPattern 方法名表达式匹配
     *                      支持通配符，如 primeFactors
     *                      可选参数
     *
     * @param condition OGNL条件表达式
     *                  满足条件的调用才会被跟踪
     *                  例如：params[0]<0
     *                  可选参数
     *
     * @param numberOfExecutions 捕获次数限制
     *                           默认值为 1
     *                           达到指定次数后自动停止
     *
     * @param regex 是否开启正则表达式匹配
     *              默认为通配符匹配
     *              默认值为 false
     *
     * @param timeout 命令执行超时时间
     *                单位为秒
     *                默认值为 AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS
     *                超时后命令自动退出
     *
     * @param toolContext 工具执行上下文
     *                    包含执行环境、会话信息等
     *
     * @return 命令执行结果，包含方法调用堆栈信息
     */
    @Tool(
        name = "stack",
        description = "Stack 调用堆栈跟踪工具: 输出当前方法被调用的调用路径，帮助分析方法的调用链路。对应 Arthas 的 stack 命令。",
        streamable = true
    )
    public String stack(
            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame")
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors", required = false)
            String methodPattern,

            @ToolParam(description = "OGNL条件表达式，满足条件的调用才会被跟踪，如params[0]<0", required = false)
            String condition,

            @ToolParam(description = "捕获次数限制，默认值为1。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认" + AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS +  "秒。超时后命令自动退出", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        // 获取捕获次数，如果未指定则使用默认值 1
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        // 获取超时时间，如果未指定则使用默认值
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        // 构建命令基础部分：stack
        StringBuilder cmd = buildCommand("stack");

        // 添加超时时间参数
        cmd.append(" --timeout ").append(timeoutSeconds);

        // 添加捕获次数参数
        cmd.append(" -n ").append(execCount);

        // 添加正则表达式标志
        addFlag(cmd, "-E", regex);

        // 添加类名模式
        addParameter(cmd, classPattern);

        // 添加方法名模式（如果指定）
        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            cmd.append(" ").append(methodPattern.trim());
        }

        // 添加条件表达式（如果指定）
        // 条件表达式需要用引号包裹
        addQuotedParameter(cmd, condition);

        // 执行可流式传输的命令
        // execCount: 捕获次数
        // DEFAULT_POLL_INTERVAL_MS: 轮询间隔 50ms
        // timeoutSeconds * 1000: 超时时间（毫秒）
        // "Stack execution completed successfully": 完成成功消息
        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS, timeoutSeconds * 1000,
                                "Stack execution completed successfully");
    }
}

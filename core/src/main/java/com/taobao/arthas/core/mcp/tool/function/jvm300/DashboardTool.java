package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * Dashboard 诊断工具类
 * 提供实时 JVM/应用面板数据的诊断功能，对应 Arthas 的 dashboard 命令
 *
 * 功能特性：
 * - 支持实时展示 JVM 运行时信息
 * - 支持自定义刷新间隔
 * - 支持限制执行次数
 * - 以流式方式返回诊断结果
 */
public class DashboardTool extends AbstractArthasTool {

    /**
     * 默认执行次数
     * 当用户未指定 numberOfExecutions 参数时使用此默认值
     */
    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;

    /**
     * 默认刷新间隔（毫秒）
     * 当用户未指定 intervalMs 参数时使用此默认值（3000ms = 3秒）
     */
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;

    /**
     * dashboard 实时面板命令
     *
     * 功能说明：
     * 实时展示 JVM 和应用的各种运行时数据，包括线程信息、内存使用情况、GC 信息等
     *
     * 支持的参数:
     * - intervalMs: 刷新间隔，单位 ms，默认 3000ms
     * - numberOfExecutions: 刷新次数限制，如果不指定则使用 DEFAULT_NUMBER_OF_EXECUTIONS (3次)
     *
     * @param intervalMs 刷新间隔（毫秒），默认 3000ms。用于控制输出频率
     * @param numberOfExecutions 执行次数限制，默认值为 3。达到指定次数后自动停止
     * @param toolContext 工具上下文对象，包含执行环境信息
     * @return 诊断结果的字符串形式，包含多次刷新的 JVM 面板数据
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
        // 获取刷新间隔，如果用户未指定则使用默认值 3000ms
        int interval = getDefaultValue(intervalMs, DEFAULT_REFRESH_INTERVAL_MS);

        // 获取执行次数，如果用户未指定则使用默认值 3 次
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        // 构建 dashboard 命令的基础部分
        StringBuilder cmd = buildCommand("dashboard");

        // 添加刷新间隔参数 -i
        cmd.append(" -i ").append(interval);

        // 添加执行次数参数 -n
        cmd.append(" -n ").append(execCount);

        // 计算超时时间
        // Dashboard 通常运行固定次数，超时时间基于（执行次数 * 刷新间隔）+ 缓冲时间
        // 缓冲时间设置为 5000ms，用于处理可能的延迟
        int calculatedTimeoutMs = execCount * interval + 5000;

        // 执行流式命令并返回结果
        // 参数说明：
        // - toolContext: 工具上下文
        // - cmd.toString(): 完整的命令字符串
        // - execCount: 预期的执行次数
        // - interval / 10: 流式输出的检查间隔（刷新间隔的 1/10）
        // - calculatedTimeoutMs: 计算得出的超时时间
        // - "Dashboard execution completed successfully": 成功完成时的提示信息
        return executeStreamable(toolContext, cmd.toString(), execCount, interval / 10, calculatedTimeoutMs,
                "Dashboard execution completed successfully");
    }
}

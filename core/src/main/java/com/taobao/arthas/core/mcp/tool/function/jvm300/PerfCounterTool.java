package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * PerfCounter 诊断工具类
 *
 * <p>该类提供了查看 JVM Perf Counter（性能计数器）信息的功能。
 * Perf Counter 是 JVM 内部用于记录各种性能指标的工具，通过这些计数器可以获取
 * JVM 的运行时性能数据。</p>
 *
 * <p>对应 Arthas 命令行工具中的 {@code perfcounter} 命令。</p>
 *
 * @author Arthas
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class PerfCounterTool extends AbstractArthasTool {

    /**
     * 查看 JVM Perf Counter 信息
     *
     * <p>该方法用于获取 JVM 的性能计数器信息。Perf Counter 包含了 JVM 运行时
     * 的各种性能指标，如类加载统计、内存使用情况、线程统计等信息。</p>
     *
     * <p>通过 {@code detailed} 参数可以控制是否显示更详细的信息。
     * 当 {@code detailed} 为 {@code true} 时，会添加 {@code -d} 参数来打印更多详情。</p>
     *
     * @param detailed 是否打印更多详情，对应命令行参数 {@code -d}。
     *                 当为 {@code true} 时会显示更详细的计数器信息，
     *                 为 {@code false} 或 {@code null} 时只显示基本信息。
     *                 该参数为可选参数。
     * @param toolContext 工具执行上下文，包含了执行该工具所需的所有上下文信息，
     *                    如会话信息、权限信息等。
     * @return 执行结果的 JSON 字符串格式，包含了 Perf Counter 的查询结果。
     *         结果中包含了 JVM 的各种性能计数器信息。
     */
    @Tool(
        name = "perfcounter",
        description = "PerfCounter 诊断工具: 查看 JVM Perf Counter 信息，对应 Arthas 的 perfcounter 命令。"
    )
    public String perfcounter(
            @ToolParam(description = "是否打印更多详情 (-d)", required = false)
            Boolean detailed,
            ToolContext toolContext
    ) {
        // 构建基础命令 "perfcounter"
        StringBuilder cmd = buildCommand("perfcounter");
        // 如果指定了 detailed 参数，则添加 -d 标志以显示详细信息
        addFlag(cmd, "-d", detailed);
        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

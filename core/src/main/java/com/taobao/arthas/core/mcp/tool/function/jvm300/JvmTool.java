package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;

/**
 * Jvm 诊断工具类
 * 提供查看当前 JVM 运行时信息的功能，对应 Arthas 的 jvm 命令
 *
 * 功能特性：
 * - 查看 JVM 的基本信息
 * - 查看线程相关的统计信息
 * - 查看 ClassLoader 相关信息
 * - 查看编译器相关信息
 * - 查看内存管理相关信息
 * - 查看操作系统相关信息
 * - 查看运行时参数和属性
 */
public class JvmTool extends AbstractArthasTool {

    /**
     * 查看当前 JVM 运行时信息
     *
     * 功能说明：
     * 返回当前 JVM 的详细运行时信息，包括但不限于：
     * - JVM 基本信息（版本、启动时间、运行时长等）
     * - 线程统计（活跃线程数、峰值线程数等）
     * - 内存统计（堆内存使用情况、GC 次数和时间等）
     * - ClassLoader 信息（已加载类数量、总加载类数量等）
     * - 编译器信息（JIT 编译统计等）
     * - 操作系统信息（操作系统类型、架构、CPU 核数等）
     * - JVM 运行时参数和系统属性
     *
     * @param toolContext 工具上下文对象，包含执行环境信息
     * @return 诊断结果的字符串形式，包含 JVM 的详细运行时信息
     */
    @Tool(
        name = "jvm",
        description = "Jvm 诊断工具: 查看当前 JVM 运行时信息。对应 Arthas 的 jvm 命令。"
    )
    public String jvm(ToolContext toolContext) {
        // 同步执行 jvm 命令并返回结果
        // 该命令不需要任何参数，直接执行即可获取 JVM 的所有运行时信息
        return executeSync(toolContext, "jvm");
    }
}

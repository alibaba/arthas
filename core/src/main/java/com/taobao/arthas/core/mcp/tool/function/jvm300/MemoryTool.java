package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;

/**
 * Memory 诊断工具类
 * 提供 JVM 内存使用情况的查询和监控功能
 * 对应 Arthas 命令行工具的 memory 命令
 *
 * 功能说明：
 * - 查看 JVM 堆内存和非堆内存的使用情况
 * - 统计各个内存区域的使用量、已用量和剩余量
 * - 帮助诊断内存泄漏和内存溢出问题
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class MemoryTool extends AbstractArthasTool {

    /**
     * memory 诊断工具方法: 查看 JVM 内存使用情况
     *
     * 功能说明：
     * - 显示 JVM 的内存统计信息，包括：
     *   - 堆内存（Heap Memory）
     *   - 非堆内存（Non-Heap Memory）
     *   - 直接内存（Direct Memory）
     * - 每个内存区域会显示：
     *   - init: 初始化大小
     *   - used: 已使用大小
     *   - committed: 已提交大小
     *   - max: 最大可用大小
     *
     * 使用场景：
     * - 诊断内存泄漏问题
     * - 监控内存使用趋势
     * - 分析内存分配是否合理
     * - 排查 OutOfMemoryError 问题
     *
     * @param toolContext 工具执行上下文，包含会话信息等
     * @return 内存使用情况的统计信息字符串
     */
    @Tool(
        name = "memory",
        description = "Memory 诊断工具: 查看 JVM 内存使用情况，对应 Arthas 的 memory 命令。"
    )
    public String memory(ToolContext toolContext) {
        // 同步执行 memory 命令并返回结果
        // 该命令会输出 JVM 各个内存区域的详细使用情况
        return executeSync(toolContext, "memory");
    }
}

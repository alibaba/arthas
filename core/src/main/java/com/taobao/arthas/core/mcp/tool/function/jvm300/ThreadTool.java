package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 线程诊断工具
 * 提供 JVM 线程信息的查看和诊断功能，对应 Arthas 的 thread 命令
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class ThreadTool extends AbstractArthasTool {

    /**
     * thread 诊断工具: 查看线程信息及堆栈
     * 支持:
     * - threadId: 线程 ID，required=false
     * - topN: 最忙前 N 个线程并打印堆栈 (-n)，required=false
     * - blocking: 是否查找阻塞其他线程的线程 (-b)，required=false
     * - all: 是否显示所有匹配线程 (--all)，required=false
     *
     * @param threadId    线程 ID，指定要查看的具体线程
     * @param topN        显示 CPU 使用率最高的前 N 个线程，并打印其堆栈信息
     * @param blocking    是否查找当前阻塞其他线程的线程
     * @param all         是否显示所有匹配的线程
     * @param toolContext 工具上下文，包含执行环境等信息
     * @return 线程诊断结果的字符串表示
     */
    @Tool(
        name = "thread",
        description = "Thread 诊断工具: 查看线程信息及堆栈，对应 Arthas 的 thread 命令。一次性输出结果。"
    )
    public String thread(
            @ToolParam(description = "线程 ID", required = false)
            Long threadId,

            @ToolParam(description = "最忙前 N 个线程并打印堆栈 (-n)", required = false)
            Integer topN,

            @ToolParam(description = "是否查找阻塞其他线程的线程 (-b)", required = false)
            Boolean blocking,

            @ToolParam(description = "是否显示所有匹配线程 (--all)", required = false)
            Boolean all,

            ToolContext toolContext
    ) {
        // 构建 thread 命令的基础部分
        StringBuilder cmd = buildCommand("thread");

        // 添加阻塞选项
        addFlag(cmd, "-b", blocking);
        // 添加 topN 选项，只处理有效的正数值
        if (topN != null && topN > 0) {
            cmd.append(" -n ").append(topN);
        }
        // 添加 all 选项
        addFlag(cmd, "--all", all);
        // 添加线程 ID，只处理有效的正数值
        if (threadId != null && threadId > 0) {
            cmd.append(" ").append(threadId);
        }

        // 记录执行的命令
        logger.info("Executing thread command: {}", cmd.toString());
        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

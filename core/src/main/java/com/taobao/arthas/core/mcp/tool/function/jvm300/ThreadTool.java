package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

public class ThreadTool extends AbstractArthasTool {

    /**
     * thread 诊断工具: 查看线程信息及堆栈
     * 支持:
     * - threadId: 线程 ID，required=false
     * - topN: 最忙前 N 个线程并打印堆栈 (-n)，required=false
     * - blocking: 是否查找阻塞其他线程的线程 (-b)，required=false
     * - all: 是否显示所有匹配线程 (--all)，required=false
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
        StringBuilder cmd = buildCommand("thread");

        addFlag(cmd, "-b", blocking);
        if (topN != null && topN > 0) {
            cmd.append(" -n ").append(topN);
        }
        addFlag(cmd, "--all", all);
        if (threadId != null && threadId > 0) {
            cmd.append(" ").append(threadId);
        }

        logger.info("Executing thread command: {}", cmd.toString());
        return executeSync(toolContext, cmd.toString());
    }
}

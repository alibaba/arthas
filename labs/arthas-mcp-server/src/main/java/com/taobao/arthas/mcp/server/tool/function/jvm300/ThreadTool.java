package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.*;

public class ThreadTool {

    private static final Logger logger = LoggerFactory.getLogger(ThreadTool.class);

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
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        McpTransportContext mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
        Object authSubject = mcpTransportContext.get(MCP_AUTH_SUBJECT_KEY);

        StringBuilder cmd = new StringBuilder("thread");

        if (Boolean.TRUE.equals(blocking)) {
            cmd.append(" -b");
        }
        if (topN != null && topN > 0) {
            cmd.append(" -n ").append(topN);
        }
        if (Boolean.TRUE.equals(all)) {
            cmd.append(" --all");
        }
        if (threadId != null && threadId > 0) {
            cmd.append(" ").append(threadId);
        }

        String commandStr = cmd.toString();
        logger.info("Executing thread command: {}", commandStr);

        return JsonParser.toJson(commandContext.executeSync(commandStr, authSubject));
    }
}

package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class PerfCounterTool {

    @Tool(
        name = "perfcounter",
        description = "PerfCounter 诊断工具: 查看 JVM Perf Counter 信息，对应 Arthas 的 perfcounter 命令。"
    )
    public String perfcounter(
            @ToolParam(description = "是否打印更多详情 (-d)", required = false)
            Boolean detailed,
            ToolContext toolContext
    ) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        StringBuilder cmd = new StringBuilder("perfcounter");
        if (Boolean.TRUE.equals(detailed)) {
            cmd.append(" -d");
        }
        String commandStr = cmd.toString();
        return JsonParser.toJson(commandContext.executeSync(commandStr));
    }
}

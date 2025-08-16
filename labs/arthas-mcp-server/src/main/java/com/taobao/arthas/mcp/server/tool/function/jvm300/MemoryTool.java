package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class MemoryTool {

    @Tool(
        name = "memory",
        description = "Memory 诊断工具: 查看 JVM 内存使用情况，对应 Arthas 的 memory 命令。"
    )
    public String memory(ToolContext toolContext) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        String commandStr = "memory";
        return JsonParser.toJson(commandContext.executeSync(commandStr));
    }
}

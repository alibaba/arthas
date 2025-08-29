package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class JvmTool {

    @Tool(
        name = "jvm",
        description = "Jvm 诊断工具: 查看当前 JVM 运行时信息。对应 Arthas 的 jvm 命令。"
    )
    public String jvm(ToolContext toolContext) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        String commandStr = "jvm";
        return JsonParser.toJson(commandContext.executeSync(commandStr));
    }
}

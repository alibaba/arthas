package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class VMOptionTool {

    @Tool(
        name = "vmoption",
        description = "VMOption 诊断工具: 查看或更新 JVM VM options，对应 Arthas 的 vmoption 命令。"
    )
    public String vmoption(
            @ToolParam(description = "Name of the VM option.", required = false)
            String key,

            @ToolParam(description = "更新值，仅在更新时使用", required = false)
            String value,

            ToolContext toolContext
    ) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        StringBuilder cmd = new StringBuilder("vmoption");
        if (key != null && !key.trim().isEmpty()) {
            cmd.append(" ").append(key.trim());
            if (value != null && !value.trim().isEmpty()) {
                cmd.append(" ").append(value.trim());
            }
        }
        String commandStr = cmd.toString();
        return JsonParser.toJson(commandContext.executeSync(commandStr));
    }
}

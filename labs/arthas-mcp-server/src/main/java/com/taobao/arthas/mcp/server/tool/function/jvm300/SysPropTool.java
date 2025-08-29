package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class SysPropTool {

    @Tool(
        name = "sysprop",
        description = "SysProp 诊断工具: 查看或修改系统属性，对应 Arthas 的 sysprop 命令。"
    )
    public String sysprop(
            @ToolParam(description = "属性名", required = false)
            String propertyName,

            @ToolParam(description = "属性值；若指定则修改，否则查看", required = false)
            String propertyValue,

            ToolContext toolContext
    ) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        StringBuilder cmd = new StringBuilder("sysprop");
        if (propertyName != null && !propertyName.trim().isEmpty()) {
            cmd.append(" ").append(propertyName.trim());
            if (propertyValue != null && !propertyValue.trim().isEmpty()) {
                cmd.append(" ").append(propertyValue.trim());
            }
        }
        String commandStr = cmd.toString();
        return JsonParser.toJson(commandContext.executeSync(commandStr));
    }
}

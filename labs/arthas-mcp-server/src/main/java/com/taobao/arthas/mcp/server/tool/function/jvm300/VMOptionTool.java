package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class VMOptionTool extends AbstractArthasTool {

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
        StringBuilder cmd = buildCommand("vmoption");
        if (key != null && !key.trim().isEmpty()) {
            cmd.append(" ").append(key.trim());
            if (value != null && !value.trim().isEmpty()) {
                cmd.append(" ").append(value.trim());
            }
        }
        return executeSync(toolContext, cmd.toString());
    }
}

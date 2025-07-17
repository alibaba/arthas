package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.ArthasCommandExecutor;

public class SysEnvTool {

    @Tool(
        name = "sysenv",
        description = "SysEnv 诊断工具: 查看系统环境变量，对应 Arthas 的 sysenv 命令。"
    )
    public String sysenv(
            @ToolParam(description = "环境变量名。若 null/empty，则查看所有变量；否则查看单个变量值。", required = false)
            String envName
    ) {
        StringBuilder cmd = new StringBuilder("sysenv");
        if (envName != null && !envName.trim().isEmpty()) {
            cmd.append(" ").append(envName.trim());
        }
        String commandStr = cmd.toString();
        return ArthasCommandExecutor.executeCommand(commandStr);
    }
}

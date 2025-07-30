package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.function.ArthasCommandExecutor;

public class JvmTool {

    @Tool(
        name = "jvm",
        description = "Jvm 诊断工具: 查看当前 JVM 运行时信息。对应 Arthas 的 jvm 命令。"
    )
    public String jvm() {
        String commandStr = "jvm";
        return ArthasCommandExecutor.executeCommand(commandStr);
    }
}

package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;

public class JvmTool extends AbstractArthasTool {

    @Tool(
        name = "jvm",
        description = "Jvm 诊断工具: 查看当前 JVM 运行时信息。对应 Arthas 的 jvm 命令。"
    )
    public String jvm(ToolContext toolContext) {
        return executeSync(toolContext, "jvm");
    }
}

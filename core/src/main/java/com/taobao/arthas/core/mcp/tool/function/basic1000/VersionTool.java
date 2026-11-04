package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;

/**
 * Version MCP Tool: 查看当前 JVM 内运行的 Arthas 版本。
 */
public class VersionTool extends AbstractArthasTool {

    @Tool(
            name = "version",
            description = "Version 诊断工具: 查看当前 JVM 内运行的 Arthas 版本，对应 Arthas 的 version 命令。"
    )
    public String version(ToolContext toolContext) {
        return executeSync(toolContext, "version");
    }
}

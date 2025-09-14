package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.MCP_TRANSPORT_CONTEXT;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class SysEnvTool {

    @Tool(
        name = "sysenv",
        description = "SysEnv 诊断工具: 查看系统环境变量，对应 Arthas 的 sysenv 命令。"
    )
    public String sysenv(
            @ToolParam(description = "环境变量名。若为空或空字符串，则查看所有变量；否则查看单个变量值。", required = false)
            String envName,
            ToolContext toolContext
    ) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        McpTransportContext mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
        Object authSubject = mcpTransportContext.get(MCP_AUTH_SUBJECT_KEY);
        StringBuilder cmd = new StringBuilder("sysenv");
        if (envName != null && !envName.trim().isEmpty()) {
            cmd.append(" ").append(envName.trim());
        }
        String commandStr = cmd.toString();
        return JsonParser.toJson(commandContext.executeSync(commandStr, authSubject));
    }
}

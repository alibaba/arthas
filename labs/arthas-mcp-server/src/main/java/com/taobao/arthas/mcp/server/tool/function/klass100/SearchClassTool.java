package com.taobao.arthas.mcp.server.tool.function.klass100;

import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.MCP_TRANSPORT_CONTEXT;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class SearchClassTool {

    @Tool(
            name = "getClassInfo",
            description = "获取指定类的详细信息，包括类加载器、方法、字段等信息"
    )
    public String sc(
            @ToolParam(description = "要查询的类名，支持全限定名") String className,
            ToolContext toolContext) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        McpTransportContext mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
        Object authSubject = mcpTransportContext.get(MCP_AUTH_SUBJECT_KEY);
        return JsonParser.toJson(commandContext.executeSync("sc -d " + className, authSubject));
    }


}

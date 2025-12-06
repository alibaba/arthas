package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

public class SearchClassTool extends AbstractArthasTool {

    @Tool(
            name = "getClassInfo",
            description = "获取指定类的详细信息，包括类加载器、方法、字段等信息"
    )
    public String sc(
            @ToolParam(description = "要查询的类名，支持全限定名") String className,
            ToolContext toolContext) {
        StringBuilder cmd = buildCommand("sc -d");
        addParameter(cmd, className);
        return executeSync(toolContext, cmd.toString());
    }
}

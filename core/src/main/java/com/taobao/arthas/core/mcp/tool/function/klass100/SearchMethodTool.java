package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

public class SearchMethodTool extends AbstractArthasTool {

    @Tool(
            name = "getClassMethods",
            description = "获取指定类的所有方法信息"
    )
    public String sm(
            @ToolParam(description = "要查询的类名，支持全限定名") String className,
            ToolContext toolContext) {
        StringBuilder cmd = buildCommand("sm");
        addParameter(cmd, className);
        return executeSync(toolContext, cmd.toString());
    }

}

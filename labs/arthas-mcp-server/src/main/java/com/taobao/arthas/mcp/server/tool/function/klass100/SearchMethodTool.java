package com.taobao.arthas.mcp.server.tool.function.klass100;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.ArthasCommandExecutor;

public class SearchMethodTool {

    @Tool(
            name = "getClassMethods",
            description = "获取指定类的所有方法信息"
    )
    public String sm(
            @ToolParam(description = "要查询的类名，支持全限定名") String className) {
        return ArthasCommandExecutor.executeCommand("sm " + className);
    }

}

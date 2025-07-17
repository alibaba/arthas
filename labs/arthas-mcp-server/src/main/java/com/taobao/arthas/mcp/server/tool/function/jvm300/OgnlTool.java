package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.ArthasCommandExecutor;

public class OgnlTool {

    @Tool(
        name = "ognl",
        description = "OGNL 诊断工具: 执行 OGNL 表达式，对应 Arthas 的 ognl 命令。"
    )
    public String ognl(
            @ToolParam(description = "OGNL 表达式")
            String expression,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "结果对象展开层次 (-x)，默认 1", required = false)
            Integer expandLevel
    ) {
        StringBuilder cmd = new StringBuilder("ognl");

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            cmd.append(" -c ").append(classLoaderHash.trim());
        }
        if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
        }
        if (expandLevel != null && expandLevel > 0) {
            cmd.append(" -x ").append(expandLevel);
        }
        cmd.append(" ").append(expression.trim());

        String commandStr = cmd.toString();
        return ArthasCommandExecutor.executeCommand(commandStr);
    }
}

package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class GetStaticTool {


    @Tool(
        name = "getstatic",
        description = "GetStatic 诊断工具: 查看类的静态字段值，可指定 ClassLoader，支持在返回结果上执行 OGNL 表达式。对应 Arthas 的 getstatic 命令。"
    )
    public String getstatic(
            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "类名表达式匹配，如java.lang.String或demo.MathGame")
            String className,

            @ToolParam(description = "静态字段名")
            String fieldName,

            @ToolParam(description = "OGNL 表达式", required = false)
            String ognlExpression,

            ToolContext toolContext
    ) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        StringBuilder cmd = new StringBuilder("getstatic");

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            cmd.append(" -c ").append(classLoaderHash.trim());
        }
        if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
        }
        cmd.append(" ").append(className.trim());
        cmd.append(" ").append(fieldName.trim());

        if (ognlExpression != null && !ognlExpression.trim().isEmpty()) {
            cmd.append(" ").append(ognlExpression.trim());
        }

        String commandStr = cmd.toString();
        return JsonParser.toJson(commandContext.executeSync(commandStr));
    }
}

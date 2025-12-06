package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

public class GetStaticTool extends AbstractArthasTool {


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
        StringBuilder cmd = buildCommand("getstatic");

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        addParameter(cmd, className);
        addParameter(cmd, fieldName);

        if (ognlExpression != null && !ognlExpression.trim().isEmpty()) {
            cmd.append(" ").append(ognlExpression.trim());
        }

        return executeSync(toolContext, cmd.toString());
    }
}

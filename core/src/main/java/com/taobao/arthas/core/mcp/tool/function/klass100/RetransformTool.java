package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

public class RetransformTool extends AbstractArthasTool {

    @Tool(
            name = "retransform",
            description = "热加载类的字节码，允许对已加载的类进行字节码修改并使其生效"
    )
    public String retransform(
            @ToolParam(description = "要操作的.class文件路径列表，用空格分隔；含空格的路径须用单引号或双引号包裹，反斜杠按原字符保留")
            String classFilePaths,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            ToolContext toolContext) {
        StringBuilder cmd = buildCommand("retransform");

        addFileParameters(cmd, classFilePaths);

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        return executeSync(toolContext, cmd.toString());
    }
}

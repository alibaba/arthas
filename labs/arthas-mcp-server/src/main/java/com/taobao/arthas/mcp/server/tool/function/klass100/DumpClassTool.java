package com.taobao.arthas.mcp.server.tool.function.klass100;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class DumpClassTool extends AbstractArthasTool {

    /**
     * Dump 命令 - 将JVM中已加载的类字节码导出到指定目录
     * 适用于批量下载指定包目录的class字节码
     */
    @Tool(
            name = "dump",
            description = "将JVM中实际运行的class字节码dump到指定目录，适用于批量下载指定包目录的class字节码"
    )
    public String dump(
            @ToolParam(description = "类名表达式匹配，如java.lang.String或demo.MathGame")
            String classPattern,

            @ToolParam(description = "指定输出目录，默认为arthas-output文件夹", required = false)
            String outputDir,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHashcode,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "是否包含子类，默认为false", required = false)
            Boolean includeInnerClasses,

            @ToolParam(description = "限制dump的类数量，避免输出过多文件", required = false)
            Integer limit,

            ToolContext toolContext) {
        StringBuilder cmd = buildCommand("dump");

        addParameter(cmd, classPattern);

        addParameter(cmd, "-d", outputDir);

        if (classLoaderHashcode != null && !classLoaderHashcode.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHashcode);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        addFlag(cmd, "--include-inner-classes", includeInnerClasses);

        if (limit != null && limit > 0) {
            cmd.append(" --limit ").append(limit);
        }

        return executeSync(toolContext, cmd.toString());
    }
}

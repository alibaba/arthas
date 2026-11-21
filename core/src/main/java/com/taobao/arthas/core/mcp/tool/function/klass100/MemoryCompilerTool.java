package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

import java.nio.file.Paths;

public class MemoryCompilerTool extends AbstractArthasTool {

    public static final String DEFAULT_DUMP_DIR = Paths.get("arthas-output").toAbsolutePath().toString();

    @Tool(
            name = "mc",
            description = "Memory Compiler/内存编译器，编译.java文件生成.class"
    )
    public String mc(
            @ToolParam(description = "要编译的.java文件路径列表，用空格分隔；含空格的路径须用单引号或双引号包裹，反斜杠按原字符保留")
            String javaFilePaths,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "指定输出目录，默认为工作目录下arthas-output文件夹", required = false)
            String outputDir,

            ToolContext toolContext) {
        StringBuilder cmd = buildCommand("mc");

        addFileParameters(cmd, javaFilePaths);

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        if (outputDir != null && !outputDir.trim().isEmpty()) {
            addParameter(cmd, "-d", outputDir);
        } else {
            addParameter(cmd, "-d", DEFAULT_DUMP_DIR);
        }

        return executeSync(toolContext, cmd.toString());
    }
}

package com.taobao.arthas.mcp.server.tool.function.klass100;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.ArthasCommandExecutor;

public class MemoryCompilerTool {

    @Tool(
            name = "mc",
            description = "Memory Compiler/内存编译器，编译.java文件生成.class"
    )
    public String mc(
            @ToolParam(description = "要编译的.java文件路径，支持多个文件，用空格分隔")
            String javaFilePaths,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "指定输出目录", required = false)
            String outputDir) {

        StringBuilder cmd = new StringBuilder("mc");

        cmd.append(" ").append(javaFilePaths);

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            cmd.append(" -c ").append(classLoaderHash.trim());
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
        }

        if (outputDir != null && !outputDir.trim().isEmpty()) {
            cmd.append(" -d ").append(outputDir.trim());
        }

        return ArthasCommandExecutor.executeCommand(cmd.toString());
    }
}

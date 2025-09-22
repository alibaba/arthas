package com.taobao.arthas.mcp.server.tool.function.klass100;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class JadTool extends AbstractArthasTool {

    @Tool(
            name = "jad",
            description = "反编译指定已加载类的源码，将JVM中实际运行的class的bytecode反编译成java代码"
    )
    public String jad(
            @ToolParam(description = "类名表达式匹配，如java.lang.String或demo.MathGame")
            String classPattern,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "反编译时只显示源代码，默认false", required = false)
            Boolean sourceOnly,

            @ToolParam(description = "反编译时不显示行号，默认false", required = false)
            Boolean noLineNumber,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean useRegex,

            @ToolParam(description = "指定dump class文件目录，默认会dump到logback.xml中配置的log目录下", required = false)
            String dumpDirectory,

            ToolContext toolContext) {
        
        StringBuilder cmd = buildCommand("jad");

        addParameter(cmd, classPattern);

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        addFlag(cmd, "--source-only", sourceOnly);
        addFlag(cmd, "-E", useRegex);
        
        if (Boolean.TRUE.equals(noLineNumber)) {
            cmd.append(" --lineNumber false");
        }

        addParameter(cmd, "-d", dumpDirectory);
        
        return executeSync(toolContext, cmd.toString());
    }
}

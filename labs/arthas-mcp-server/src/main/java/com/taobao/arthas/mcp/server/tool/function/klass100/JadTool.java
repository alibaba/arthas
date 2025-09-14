package com.taobao.arthas.mcp.server.tool.function.klass100;

import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.MCP_TRANSPORT_CONTEXT;
import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class JadTool {

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
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        McpTransportContext mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);
        Object authSubject = mcpTransportContext.get(MCP_AUTH_SUBJECT_KEY);

        StringBuilder cmd = new StringBuilder("jad");

        cmd.append(" ").append(classPattern);

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            cmd.append(" -c ").append(classLoaderHash.trim());
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
        }

        if (Boolean.TRUE.equals(sourceOnly)) {
            cmd.append(" --source-only");
        }

        if (Boolean.TRUE.equals(noLineNumber)) {
            cmd.append(" --lineNumber false");
        }

        if (Boolean.TRUE.equals(useRegex)) {
            cmd.append(" -E");
        }

        if (dumpDirectory != null && !dumpDirectory.trim().isEmpty()) {
            cmd.append(" -d ").append(dumpDirectory.trim());
        }

        return JsonParser.toJson(commandContext.executeSync(cmd.toString(), authSubject));
    }
}

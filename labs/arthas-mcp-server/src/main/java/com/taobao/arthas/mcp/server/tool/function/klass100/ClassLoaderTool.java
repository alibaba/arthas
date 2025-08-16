package com.taobao.arthas.mcp.server.tool.function.klass100;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.TOOL_CONTEXT_COMMAND_CONTEXT_KEY;

public class ClassLoaderTool {

    public static final String MODE_STATS = "stats";
    public static final String MODE_INSTANCES = "instances";
    public static final String MODE_TREE = "tree";
    public static final String MODE_ALL_CLASSES = "all-classes";
    public static final String MODE_URL_STATS = "url-stats";

    @Tool(
            name = "classloader",
            description = "ClassLoader 诊断工具，可以查看类加载器统计信息、继承树、URLs，以及进行资源查找和类加载操作"
    )
    public String classloader(
            @ToolParam(description = "显示模式：stats(统计信息，默认), instances(实例详情), tree(继承树), all-classes(所有类，慎用), url-stats(URL统计)", required = false)
            String mode,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "要查找的资源名称，如META-INF/MANIFEST.MF", required = false)
            String resource,

            @ToolParam(description = "要加载的类名，支持全限定名", required = false)
            String loadClass,

            ToolContext toolContext) {
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);

        StringBuilder cmd = new StringBuilder("classloader");

        if (mode != null) {
            switch (mode.toLowerCase()) {
                case MODE_INSTANCES:
                    cmd.append(" -l");
                    break;
                case MODE_TREE:
                    cmd.append(" -t");
                    break;
                case MODE_ALL_CLASSES:
                    cmd.append(" -a");
                    break;
                case MODE_URL_STATS:
                    cmd.append(" --url-stat");
                    break;
                case MODE_STATS:
                default:
                    break;
            }
        }

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            cmd.append(" -c ").append(classLoaderHash.trim());
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            cmd.append(" --classLoaderClass ").append(classLoaderClass.trim());
        }

        if (resource != null && !resource.trim().isEmpty()) {
            cmd.append(" -r ").append(resource.trim());
        }

        if (loadClass != null && !loadClass.trim().isEmpty()) {
            cmd.append(" --load ").append(loadClass.trim());
        }

        return JsonParser.toJson(commandContext.executeSync(cmd.toString()));
    }
}

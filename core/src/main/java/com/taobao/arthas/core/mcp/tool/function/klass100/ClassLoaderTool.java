package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

public class ClassLoaderTool extends AbstractArthasTool {

    public static final String MODE_STATS = "stats";
    public static final String MODE_INSTANCES = "instances";
    public static final String MODE_TREE = "tree";
    public static final String MODE_ALL_CLASSES = "all-classes";
    public static final String MODE_URL_STATS = "url-stats";
    public static final String MODE_URL_CLASSES = "url-classes";

    @Tool(
            name = "classloader",
            description = "ClassLoader 诊断工具，可以查看类加载器统计信息、继承树、URLs，以及进行资源查找和类加载操作。搜索类的场景优先使用 sc 工具"
    )
    public String classloader(
            @ToolParam(description = "显示模式：stats(统计信息，默认), instances(实例详情), tree(继承树), all-classes(所有类，慎用), url-stats(URL统计), url-classes(URL与类关系)", required = false)
            String mode,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "要查找的资源名称，如META-INF/MANIFEST.MF", required = false)
            String resource,

            @ToolParam(description = "要加载的类名，支持全限定名", required = false)
            String loadClass,

            @ToolParam(description = "详情模式：列出每个 URL/jar 中的类名（等价于 -d），仅在 mode=url-classes 时生效", required = false)
            Boolean details,

            @ToolParam(description = "按 jar 包名/URL 关键字过滤，仅在 mode=url-classes 时生效", required = false)
            String jar,

            @ToolParam(description = "按类名/包名关键字过滤，仅在 mode=url-classes 时生效", required = false)
            String classFilter,

            @ToolParam(description = "是否使用正则匹配 jar/class（等价于 -E），仅在 mode=url-classes 时生效", required = false)
            Boolean regex,

            @ToolParam(description = "详情模式下每个 URL/jar 最多展示类数量（等价于 -n），默认 100，仅在 mode=url-classes 时生效", required = false)
            Integer limit,

            ToolContext toolContext) {
        StringBuilder cmd = buildCommand("classloader");

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
                case MODE_URL_CLASSES:
                    cmd.append(" --url-classes");
                    break;
                case MODE_STATS:
                default:
                    break;
            }
        }

        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        addParameter(cmd, "-r", resource);

        addParameter(cmd, "--load", loadClass);

        if (mode != null && MODE_URL_CLASSES.equalsIgnoreCase(mode)) {
            addFlag(cmd, "-d", details);
            addFlag(cmd, "-E", regex);
            if (limit != null && limit > 0) {
                addParameter(cmd, "-n", String.valueOf(limit));
            }
            addParameter(cmd, "--jar", jar);
            addParameter(cmd, "--class", classFilter);
        }

        return executeSync(toolContext, cmd.toString());
    }
}

package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 类搜索工具，对应 Arthas 的 sc 命令
 * 用于搜索 JVM 中已加载的类，支持通配符和正则表达式匹配
 */
public class SearchClassTool extends AbstractArthasTool {

    @Tool(
            name = "sc",
            description = "搜索 JVM 中已加载的类。支持通配符(*)和正则表达式匹配，可查看类的详细信息（类加载器、接口、父类、注解等）和字段信息"
    )
    public String sc(
            @ToolParam(description = "类名模式，支持全限定名。可使用通配符如 *StringUtils 或 org.apache.commons.lang.*，类名分隔符支持 '.' 或 '/'")
            String classPattern,

            @ToolParam(description = "是否显示类的详细信息，包括类加载器、代码来源、接口、父类、注解等。默认为 true", required = false)
            Boolean detail,

            @ToolParam(description = "是否显示类的所有成员变量（字段）信息。需要 detail 为 true 时才生效", required = false)
            Boolean field,

            @ToolParam(description = "是否使用正则表达式匹配类名。默认为 false（使用通配符匹配）", required = false)
            Boolean regex,

            @ToolParam(description = "指定 ClassLoader 的 hashcode（16进制），用于在多个 ClassLoader 加载同名类时精确定位", required = false)
            String classLoaderHash,

            @ToolParam(description = "指定 ClassLoader 的完整类名，如 sun.misc.Launcher$AppClassLoader，可替代 hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "指定 ClassLoader 的 toString() 返回值，用于匹配特定的类加载器实例", required = false)
            String classLoaderStr,

            @ToolParam(description = "对象展开层级，用于展示更详细的对象结构。默认为 0", required = false)
            Integer expand,

            @ToolParam(description = "最大匹配类数量限制（仅在显示详细信息时生效）。默认为 100，防止返回过多结果", required = false)
            Integer limit,

            ToolContext toolContext) {

        StringBuilder cmd = buildCommand("sc");

        // 默认显示详细信息
        boolean showDetail = (detail == null || detail);
        addFlag(cmd, "-d", showDetail);

        // 显示字段信息
        addFlag(cmd, "-f", field);

        // 使用正则表达式匹配
        addFlag(cmd, "-E", regex);

        // 指定类加载器（三种方式，优先使用 hashcode）
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        } else if (classLoaderStr != null && !classLoaderStr.trim().isEmpty()) {
            addParameter(cmd, "-cs", classLoaderStr);
        }

        // 对象展开层级
        if (expand != null && expand > 0) {
            addParameter(cmd, "-x", String.valueOf(expand));
        }

        // 最大匹配数量限制
        if (limit != null && limit > 0) {
            addParameter(cmd, "-n", String.valueOf(limit));
        }

        // 类名模式（必需参数）
        addParameter(cmd, classPattern);

        return executeSync(toolContext, cmd.toString());
    }
}

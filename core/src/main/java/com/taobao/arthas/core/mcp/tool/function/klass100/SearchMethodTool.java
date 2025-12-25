package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 方法搜索工具，对应 Arthas 的 sm 命令
 * 用于搜索 JVM 中已加载类的方法，支持通配符和正则表达式匹配
 */
public class SearchMethodTool extends AbstractArthasTool {

    @Tool(
            name = "sm",
            description = "搜索 JVM 中已加载类的方法。支持通配符(*)和正则表达式匹配，可查看方法的详细信息（返回类型、参数类型、异常类型、注解等）"
    )
    public String sm(
            @ToolParam(description = "类名模式，支持全限定名。可使用通配符如 *StringUtils 或 org.apache.commons.lang.*，类名分隔符支持 '.' 或 '/'")
            String classPattern,

            @ToolParam(description = "方法名模式。可使用通配符如 get* 或 *Name。不指定时匹配所有方法", required = false)
            String methodPattern,

            @ToolParam(description = "是否显示方法的详细信息，包括返回类型、参数类型、异常类型、注解、类加载器等。默认为 true", required = false)
            Boolean detail,

            @ToolParam(description = "是否使用正则表达式匹配类名和方法名。默认为 false（使用通配符匹配）", required = false)
            Boolean regex,

            @ToolParam(description = "指定 ClassLoader 的 hashcode（16进制），用于在多个 ClassLoader 加载同名类时精确定位", required = false)
            String classLoaderHash,

            @ToolParam(description = "指定 ClassLoader 的完整类名，如 sun.misc.Launcher$AppClassLoader，可替代 hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "最大匹配类数量限制。默认为 100，防止返回过多结果", required = false)
            Integer limit,

            ToolContext toolContext) {

        StringBuilder cmd = buildCommand("sm");

        // 默认显示详细信息
        boolean showDetail = (detail == null || detail);
        addFlag(cmd, "-d", showDetail);

        // 使用正则表达式匹配
        addFlag(cmd, "-E", regex);

        // 指定类加载器（两种方式，优先使用 hashcode）
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 最大匹配类数量限制
        if (limit != null && limit > 0) {
            addParameter(cmd, "-n", String.valueOf(limit));
        }

        // 类名模式（必需参数）
        addParameter(cmd, classPattern);

        // 方法名模式（可选参数）
        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            addParameter(cmd, methodPattern);
        }

        return executeSync(toolContext, cmd.toString());
    }
}

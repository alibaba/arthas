package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 类搜索工具（Search Class Tool）
 * 对应 Arthas 的 sc 命令
 *
 * 功能说明：
 * 用于搜索 JVM 中已加载的类，支持通配符和正则表达式匹配
 * 可以查看类的详细信息，包括类加载器、接口、父类、注解等
 * 还可以查看类的字段信息
 *
 * 使用场景：
 * - 查看某个类是否已被 JVM 加载
 * - 查看类的详细信息（类加载器、代码来源等）
 * - 查看类实现的接口和继承的父类
 * - 查看类上的注解
 * - 查看类的字段信息
 * - 在多个 ClassLoader 环境中定位特定的类实例
 */
public class SearchClassTool extends AbstractArthasTool {

    /**
     * 搜索 JVM 中已加载的类
     *
     * 对应 Arthas 命令：sc [options] classPattern
     *
     * 功能：
     * - 搜索已加载的类信息
     * - 支持通配符(*)和正则表达式匹配
     * - 可查看类的详细信息（类加载器、接口、父类、注解等）
     * - 可查看类的字段信息
     *
     * @param classPattern 类名模式，支持全限定名
     *                     可使用通配符如 *StringUtils 或 org.apache.commons.lang.*
     *                     类名分隔符支持 '.' 或 '/'
     *
     * @param detail 是否显示类的详细信息
     *               包括类加载器、代码来源、接口、父类、注解等
     *               默认为 true
     *
     * @param field 是否显示类的所有成员变量（字段）信息
     *              需要 detail 为 true 时才生效
     *
     * @param regex 是否使用正则表达式匹配类名
     *              默认为 false（使用通配符匹配）
     *
     * @param classLoaderHash 指定 ClassLoader 的 hashcode（16进制）
     *                        用于在多个 ClassLoader 加载同名类时精确定位
     *
     * @param classLoaderClass 指定 ClassLoader 的完整类名
     *                         如 sun.misc.Launcher$AppClassLoader
     *                         可替代 hashcode 使用
     *
     * @param classLoaderStr 指定 ClassLoader 的 toString() 返回值
     *                       用于匹配特定的类加载器实例
     *
     * @param expand 对象展开层级
     *               用于展示更详细的对象结构
     *               默认为 0
     *
     * @param limit 最大匹配类数量限制
     *              仅在显示详细信息时生效
     *              默认为 100，防止返回过多结果
     *
     * @param toolContext 工具执行上下文
     *                    包含执行环境、会话信息等
     *
     * @return 命令执行结果，包含匹配的类信息
     */
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

        // 构建命令基础部分：sc
        StringBuilder cmd = buildCommand("sc");

        // 默认显示详细信息
        // 如果 detail 参数为 null 或 true，则添加 -d 标志
        boolean showDetail = (detail == null || detail);
        addFlag(cmd, "-d", showDetail);

        // 显示字段信息
        // 如果 field 参数为 true，则添加 -f 标志
        addFlag(cmd, "-f", field);

        // 使用正则表达式匹配
        // 如果 regex 参数为 true，则添加 -E 标志
        addFlag(cmd, "-E", regex);

        // 指定类加载器（三种方式，优先使用 hashcode）
        // 1. 优先使用 classLoaderHash（16进制 hashcode）
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        }
        // 2. 其次使用 classLoaderClass（完整类名）
        else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }
        // 3. 最后使用 classLoaderStr（toString() 返回值）
        else if (classLoaderStr != null && !classLoaderStr.trim().isEmpty()) {
            addParameter(cmd, "-cs", classLoaderStr);
        }

        // 对象展开层级
        // 如果 expand 参数大于 0，则添加 -x 参数
        if (expand != null && expand > 0) {
            addParameter(cmd, "-x", String.valueOf(expand));
        }

        // 最大匹配数量限制
        // 如果 limit 参数大于 0，则添加 -n 参数
        if (limit != null && limit > 0) {
            addParameter(cmd, "-n", String.valueOf(limit));
        }

        // 类名模式（必需参数）
        // 将类名模式添加到命令末尾
        addParameter(cmd, classPattern);

        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

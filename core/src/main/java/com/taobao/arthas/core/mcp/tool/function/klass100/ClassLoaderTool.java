package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * ClassLoader诊断工具类
 *
 * 该工具提供了查看和分析Java类加载器（ClassLoader）的功能，包括：
 * - 查看类加载器的统计信息
 * - 查看类加载器的实例详情
 * - 查看类加载器的继承树结构
 * - 查看所有已加载的类
 * - 查看URL统计信息
 * - 查看URL与类的映射关系
 * - 查找和加载资源
 * - 动态加载类
 *
 * @author Arthas Team
 * @see AbstractArthasTool
 */
public class ClassLoaderTool extends AbstractArthasTool {

    /**
     * 统计信息模式常量
     * 显示类加载器的统计信息，包括每个类加载器已加载类的数量
     */
    public static final String MODE_STATS = "stats";

    /**
     * 实例详情模式常量
     * 显示类加载器的详细信息，包括每个类加载器的实例、hashcode、已加载类数量等
     */
    public static final String MODE_INSTANCES = "instances";

    /**
     * 继承树模式常量
     * 以树形结构显示类加载器的继承关系
     */
    public static final String MODE_TREE = "tree";

    /**
     * 所有类模式常量
     * 显示所有已加载的类（注意：此模式会列出所有类，可能产生大量输出，请谨慎使用）
     */
    public static final String MODE_ALL_CLASSES = "all-classes";

    /**
     * URL统计模式常量
     * 显示类加载器URL的统计信息，包括每个URL加载的类数量
     */
    public static final String MODE_URL_STATS = "url-stats";

    /**
     * URL与类关系模式常量
     * 显示URL与类的映射关系，列出每个URL/jar包中包含的类
     */
    public static final String MODE_URL_CLASSES = "url-classes";

    /**
     * ClassLoader诊断工具的主方法
     *
     * 该方法根据传入的参数构建Arthas的classloader命令并执行。
     * 支持多种显示模式和参数组合，用于全面诊断JVM中的类加载器情况。
     *
     * @param mode 显示模式，支持：
     *             - stats: 统计信息模式（默认）
     *             - instances: 实例详情模式
     *             - tree: 继承树模式
     *             - all-classes: 所有类模式（慎用）
     *             - url-stats: URL统计模式
     *             - url-classes: URL与类关系模式
     * @param classLoaderHash 类加载器的hashcode（16进制格式），用于精确定位特定的类加载器实例
     * @param classLoaderClass 类加载器的完整类名（如sun.misc.Launcher$AppClassLoader），可作为hashcode的替代方式
     * @param resource 要查找的资源名称（如META-INF/MANIFEST.MF），用于在类加载器中查找特定资源
     * @param loadClass 要加载的类名（支持全限定名），用于测试类是否能被成功加载
     * @param details 详情模式标志，仅在mode=url-classes时生效，列出每个URL/jar中的所有类名
     * @param jar 按jar包名/URL关键字过滤，仅在mode=url-classes时生效
     * @param classFilter 按类名/包名关键字过滤，仅在mode=url-classes时生效
     * @param regex 是否使用正则表达式匹配jar/class，仅在mode=url-classes时生效
     * @param limit 详情模式下每个URL/jar最多展示的类数量，仅在mode=url-classes时生效
     * @param toolContext 工具上下文对象，包含执行环境和配置信息
     * @return 命令执行结果的字符串表示
     */
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
        // 构建基础命令 "classloader"
        StringBuilder cmd = buildCommand("classloader");

        // 根据模式参数添加相应的命令行选项
        if (mode != null) {
            switch (mode.toLowerCase()) {
                case MODE_INSTANCES:
                    // 实例详情模式：添加 -l 参数
                    cmd.append(" -l");
                    break;
                case MODE_TREE:
                    // 继承树模式：添加 -t 参数
                    cmd.append(" -t");
                    break;
                case MODE_ALL_CLASSES:
                    // 所有类模式：添加 -a 参数
                    cmd.append(" -a");
                    break;
                case MODE_URL_STATS:
                    // URL统计模式：添加 --url-stat 参数
                    cmd.append(" --url-stat");
                    break;
                case MODE_URL_CLASSES:
                    // URL与类关系模式：添加 --url-classes 参数
                    cmd.append(" --url-classes");
                    break;
                case MODE_STATS:
                default:
                    // 统计信息模式为默认模式，不需要额外参数
                    break;
            }
        }

        // 处理类加载器标识参数
        // 优先使用hashcode，如果未提供则使用类名
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            // 使用16进制的hashcode指定类加载器
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            // 使用完整类名指定类加载器
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 添加资源查找参数
        addParameter(cmd, "-r", resource);

        // 添加类加载参数
        addParameter(cmd, "--load", loadClass);

        // 处理url-classes模式的特殊参数
        if (mode != null && MODE_URL_CLASSES.equalsIgnoreCase(mode)) {
            // 添加详情模式标志
            addFlag(cmd, "-d", details);
            // 添加正则表达式匹配标志
            addFlag(cmd, "-E", regex);
            // 添加限制数量参数
            if (limit != null && limit > 0) {
                addParameter(cmd, "-n", String.valueOf(limit));
            }
            // 添加jar包过滤参数
            addParameter(cmd, "--jar", jar);
            // 添加类名过滤参数
            addParameter(cmd, "--class", classFilter);
        }

        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 类字节码导出工具类
 *
 * 该工具用于将JVM中已加载的类的字节码导出到指定目录，主要应用场景包括：
 * - 批量下载指定包目录的class字节码
 * - 获取JVM中实际运行的class文件（可能与源码编译后的class不同）
 * - 用于反编译分析或离线调试
 * - 备份生产环境运行的class文件
 *
 * 注意：导出的是JVM内存中实际运行的字节码，可能包含运行时增强、AOP代理等修改
 *
 * @author Arthas Team
 * @see AbstractArthasTool
 */
public class DumpClassTool extends AbstractArthasTool {

    /**
     * Dump命令的主方法
     *
     * 该方法将JVM中已加载的类字节码导出到指定的文件系统中。
     * 导出的class文件是JVM内存中实际运行的字节码，可能经过以下修改：
     * - 运行时字节码增强（如Agent、AOP）
     * - 热更新修改
     * - JVM优化调整
     *
     * 使用场景示例：
     * 1. 分析生产环境运行的代码与源码是否一致
     * 2. 提取class文件进行反编译分析
     * 3. 备份特定版本的class文件
     * 4. 排查类加载冲突问题
     *
     * @param classPattern 类名匹配表达式，支持：
     *                     - 完整类名：java.lang.String
     *                     - 通配符匹配：com.example.*
     *                     - 正则表达式（需启用）
     *                     此参数为必填项
     * @param outputDir 指定输出目录路径
     *                  - 如果不指定，默认使用当前目录下的arthas-output文件夹
     *                  - 目录不存在时会自动创建
     *                  - 支持相对路径和绝对路径
     * @param classLoaderHashcode 类加载器的hashcode（16进制格式）
     *                            - 用于指定从特定的类加载器导出类
     *                            - 当一个类被多个类加载器加载时，可用此参数区分
     *                            - 格式如：1a2b3c4d
     * @param classLoaderClass 类加载器的完整类名
     *                         - 如sun.misc.Launcher$AppClassLoader
     *                         - 可作为hashcode的替代方式
     *                         - 更直观但可能不够精确（可能有多个实例）
     * @param includeInnerClasses 是否包含内部类
     *                            - true: 导出外部类时同时导出所有内部类
     *                            - false: 仅导出指定的类（默认）
     *                            - 建议在需要完整分析时启用
     * @param limit 导出类数量的限制
     *              - 避免匹配表达式过于宽泛导致导出大量文件
     *              - 建议在不确定匹配结果数量时设置
     *              - 达到限制后会停止导出
     * @param toolContext 工具上下文对象，包含执行环境和配置信息
     * @return 命令执行结果的字符串表示，包含导出的文件路径和数量信息
     */
    /**
     * Dump 命令 - 将JVM中已加载的类字节码导出到指定目录
     * 适用于批量下载指定包目录的class字节码
     */
    @Tool(
            name = "dump",
            description = "将JVM中实际运行的class字节码dump到指定目录，适用于批量下载指定包目录的class字节码"
    )
    public String dump(
            @ToolParam(description = "类名表达式匹配，如java.lang.String或demo.MathGame")
            String classPattern,

            @ToolParam(description = "指定输出目录，默认为arthas-output文件夹", required = false)
            String outputDir,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHashcode,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "是否包含子类，默认为false", required = false)
            Boolean includeInnerClasses,

            @ToolParam(description = "限制dump的类数量，避免输出过多文件", required = false)
            Integer limit,

            ToolContext toolContext) {
        // 构建基础命令 "dump"
        StringBuilder cmd = buildCommand("dump");

        // 添加类名匹配表达式（必填参数）
        addParameter(cmd, classPattern);

        // 添加输出目录参数
        addParameter(cmd, "-d", outputDir);

        // 处理类加载器标识参数
        // 优先使用hashcode，如果未提供则使用类名
        if (classLoaderHashcode != null && !classLoaderHashcode.trim().isEmpty()) {
            // 使用16进制的hashcode指定类加载器
            addParameter(cmd, "-c", classLoaderHashcode);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            // 使用完整类名指定类加载器
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 添加是否包含内部类的标志
        addFlag(cmd, "--include-inner-classes", includeInnerClasses);

        // 添加导出数量限制参数
        // 只有当limit大于0时才添加此参数
        if (limit != null && limit > 0) {
            cmd.append(" --limit ").append(limit);
        }

        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

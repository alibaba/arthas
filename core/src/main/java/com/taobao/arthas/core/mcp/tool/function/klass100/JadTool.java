package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 类反编译工具类
 *
 * 该工具用于反编译JVM中已加载的类，将字节码转换为可读的Java源代码。
 * 主要应用场景包括：
 * - 查看生产环境运行的实际代码（可能包含热更新、AOP增强等）
 * - 在没有源码的情况下理解类的实现逻辑
 * - 对比编译后的class与源码的差异
 * - 快速定位线上问题代码
 * - 学习第三方库的实现方式
 *
 * 反编译特点：
 * - 反编译的是JVM内存中的字节码，而非磁盘上的class文件
 * - 支持显示行号，便于定位问题
 * - 可以将反编译后的代码保存到文件
 * - 注释和格式信息会在编译时丢失，无法还原
 *
 * @author Arthas Team
 * @see AbstractArthasTool
 */
public class JadTool extends AbstractArthasTool {

    /**
     * Jad反编译命令的主方法
     *
     * 该方法将JVM中实际运行的类字节码反编译为Java源代码并显示。
     * 反编译的代码可能包含：
     * - 运行时字节码增强（如Spring AOP代理）
     * - 热更新修改的代码
     * - 第三方框架注入的代码
     * - JVM优化的字节码
     *
     * 常见使用场景：
     * 1. 确认线上运行的代码版本
     * 2. 分析没有源码的第三方类
     * 3. 排查类加载问题
     * 4. 理解AOP代理的实现
     *
     * 注意事项：
     * - 反编译的代码不包含原始注释
     * - 变量名可能被混淆
     * - 语法结构可能与原始源码不同，但逻辑等价
     *
     * @param classPattern 类名匹配表达式，支持：
     *                     - 完整类名：java.lang.String
     *                     - 通配符匹配：com.example.*
     *                     - 正则表达式（需启用useRegex）
     *                     此参数为必填项
     * @param classLoaderHash 类加载器的hashcode（16进制格式）
     *                        - 用于指定反编译特定类加载器加载的类
     *                        - 当一个类被多个类加载器加载时特别有用
     *                        - 格式如：1a2b3c4d
     * @param classLoaderClass 类加载器的完整类名
     *                         - 如sun.misc.Launcher$AppClassLoader
     *                         - 可作为hashcode的替代方式
     *                         - 更直观但可能不够精确（可能有多个实例）
     * @param sourceOnly 是否只显示源代码
     *                   - true: 仅显示反编译的Java代码，不显示类信息、方法定位等
     *                   - false: 显示完整信息，包括类加载器、方法位置等（默认）
     *                   - 建议在只需查看代码逻辑时启用
     * @param noLineNumber 是否显示行号
     *                     - true: 不显示行号
     *                     - false: 显示行号，便于定位问题（默认）
     *                     - 行号基于字节码的行号表，可能与源码不完全对应
     * @param useRegex 是否使用正则表达式匹配类名
     *                 - true: classPattern作为正则表达式处理
     *                 - false: classPattern作为通配符处理，支持*和?（默认）
     *                 - 正则表达式更灵活但也更复杂
     * @param dumpDirectory 指定反编译代码的保存目录
     *                      - 如果指定，会将反编译的代码保存为.java文件
     *                      - 不指定则仅显示到控制台
     *                      - 默认会dump到logback.xml中配置的log目录下
     * @param toolContext 工具上下文对象，包含执行环境和配置信息
     * @return 反编译结果的字符串表示，包含Java源代码或文件保存路径
     */
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
        // 构建基础命令 "jad"
        StringBuilder cmd = buildCommand("jad");

        // 添加类名匹配表达式（必填参数）
        addParameter(cmd, classPattern);

        // 处理类加载器标识参数
        // 优先使用hashcode，如果未提供则使用类名
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            // 使用16进制的hashcode指定类加载器
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            // 使用完整类名指定类加载器
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 添加只显示源代码的标志
        addFlag(cmd, "--source-only", sourceOnly);

        // 添加使用正则表达式的标志
        addFlag(cmd, "-E", useRegex);

        // 处理行号显示参数
        // 注意：这里使用的是 --lineNumber false 来禁用行号
        if (Boolean.TRUE.equals(noLineNumber)) {
            cmd.append(" --lineNumber false");
        }

        // 添加反编译代码保存目录参数
        addParameter(cmd, "-d", dumpDirectory);

        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

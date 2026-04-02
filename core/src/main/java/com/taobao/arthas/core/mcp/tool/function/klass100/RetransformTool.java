package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 类热转换工具类
 *
 * 该类封装了Arthas的retransform命令，用于对已加载的类进行字节码修改并使其生效。
 * retransform命令通过Instrumentation API的retransformClasses方法实现类的字节码增强。
 *
 * 功能特性：
 * - 支持通过外部.class文件对JVM中已加载的类进行字节码增强
 * - 支持指定ClassLoader来定位要转换的类
 * - 支持通过ClassLoader的hashcode或类名来指定ClassLoader
 * - 常用于AOP、性能监控、日志记录等字节码增强场景
 *
 * 与redefine的区别：
 * - redefine：完全替换类的字节码，用于修复bug
 * - retransform：对类进行字节码增强（增强逻辑），通常不改变类的原有逻辑
 * - redefine改变了类的实现，而retransform是在原有基础上增加功能
 *
 * 使用场景：
 * - AOP切面编程（如添加方法调用日志）
 * - 性能监控（如记录方法执行时间）
 * - 安全检查（如添加权限验证）
 * - 调试诊断（如添加调用链跟踪）
 *
 * 注意事项：
 * - retransform会触发类的转换，所有已加载的该类的实例都会受影响
 * - 转换后的字节码必须兼容JVM的验证要求
 * - 某些系统类可能无法被retransform
 * - retransform会改变类的方法体，但不能改变类的schema
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.command.klass100.RetransformCommand
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-">Instrumentation.retransformClasses</a>
 */
public class RetransformTool extends AbstractArthasTool {

    /**
     * 执行类热转换操作
     *
     * 该方法是RetransformTool的核心方法，用于构建并执行retransform命令。
     * 通过加载外部编译的.class文件，对JVM中已加载的类进行字节码增强。
     *
     * 执行流程：
     * 1. 构建命令基础部分："retransform"
     * 2. 添加.class文件路径参数
     * 3. 如果指定了ClassLoader hashcode，添加-c参数
     * 4. 否则如果指定了ClassLoader类名，添加--classLoaderClass参数
     * 5. 执行同步命令并返回结果
     *
     * ClassLoader指定说明：
     * - 在多ClassLoader环境中，可能需要指定特定的ClassLoader
     * - 可以通过-c参数指定ClassLoader的hashcode（16进制）
     * - 也可以通过--classLoaderClass参数指定ClassLoader的完整类名
     * - 两种方式二选一，优先使用-c参数
     *
     * 典型使用场景：
     * 1. 编写增强后的类文件（如添加了日志输出）
     * 2. 使用javac编译生成.class文件
     * 3. 使用retransform命令加载并生效
     *
     * @param classFilePaths 要转换的.class文件路径，支持多个文件用空格分隔
     *                        文件路径可以是相对路径或绝对路径
     *                        例如："/tmp/EnhancedMyClass.class" 或 "EnhancedMyClass.class"
     * @param classLoaderHash ClassLoader的hashcode（16进制字符串），用于指定特定的ClassLoader
     *                        可以通过"sc -d <classname>"命令查看类的ClassLoader hashcode
     *                        例如："1b6d3580"
     * @param classLoaderClass ClassLoader的完整类名，可替代hashcode
     *                        当不知道hashcode时，可以通过指定ClassLoader的类名来定位
     *                        例如："sun.misc.Launcher$AppClassLoader"
     * @param toolContext 工具执行上下文，包含执行环境信息
     * @return retransform命令的执行结果
     */
    @Tool(
            name = "retransform",
            description = "热加载类的字节码，允许对已加载的类进行字节码修改并使其生效"
    )
    public String retransform(
            @ToolParam(description = "要操作的.class文件路径，支持多个文件，用空格分隔")
            String classFilePaths,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            ToolContext toolContext) {
        // 构建retransform命令的基础部分
        StringBuilder cmd = buildCommand("retransform");

        // 添加要转换的.class文件路径参数
        // 支持多个文件，用空格分隔
        addParameter(cmd, classFilePaths);

        // 如果指定了ClassLoader的hashcode，使用-c参数
        // hashcode是16进制字符串，可以通过"sc -d <classname>"查看
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        }
        // 否则如果指定了ClassLoader的类名，使用--classLoaderClass参数
        // 这是hashcode的替代方案，使用完整的类名来指定ClassLoader
        else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

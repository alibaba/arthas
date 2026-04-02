package com.taobao.arthas.core.mcp.tool.function.klass100;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 类重定义工具类
 *
 * 该类封装了Arthas的redefine命令，用于在JVM运行时重新加载已存在的类的字节码。
 * redefine命令通过Instrumentation API的redefineClasses方法实现类的热更新。
 *
 * 功能特性：
 * - 支持外部编译的.class文件热加载到JVM
 * - 支持指定ClassLoader来定位要重定义的类
 * - 支持通过ClassLoader的hashcode或类名来指定ClassLoader
 * - 可以实现不重启JVM的情况下修复代码bug
 *
 * 使用场景：
 * - 线上环境紧急bug修复
 * - 快速验证代码修改效果
 * - 临时添加日志或调试代码
 *
 * 注意事项：
 * - redefine会改变类的字节码，但不会改变类的元数据（如方法签名、字段等）
 * - 新的字节码必须与原有的类结构兼容（方法签名、字段等不能改变）
 * - redefine不能修改类的schema（如添加/删除方法或字段）
 * - 某些情况下可能需要配合retransform使用
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.command.klass100.RedefineCommand
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-">Instrumentation.redefineClasses</a>
 */
public class RedefineTool extends AbstractArthasTool {

    /**
     * 执行类重定义操作
     *
     * 该方法是RedefineTool的核心方法，用于构建并执行redefine命令。
     * 通过加载外部编译的.class文件，替换JVM中已加载的类的字节码。
     *
     * 执行流程：
     * 1. 构建命令基础部分："redefine"
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
     * @param classFilePaths 要重定义的.class文件路径，支持多个文件用空格分隔
     *                        文件路径可以是相对路径或绝对路径
     *                        例如："/tmp/MyClass.class" 或 "MyClass.class"
     * @param classLoaderHash ClassLoader的hashcode（16进制字符串），用于指定特定的ClassLoader
     *                        可以通过"sc -d <classname>"命令查看类的ClassLoader hashcode
     *                        例如："1b6d3580"
     * @param classLoaderClass ClassLoader的完整类名，可替代hashcode
     *                        当不知道hashcode时，可以通过指定ClassLoader的类名来定位
     *                        例如："sun.misc.Launcher$AppClassLoader"
     * @param toolContext 工具执行上下文，包含执行环境信息
     * @return redefine命令的执行结果
     */
    @Tool(
            name = "redefine",
            description = "重新加载类的字节码，允许在JVM运行时，重新加载已存在的类的字节码，实现热更新"
    )
    public String redefine(
            @ToolParam(description = "要重新定义的.class文件路径，支持多个文件，用空格分隔")
            String classFilePaths,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "指定执行表达式的ClassLoader的class name，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            ToolContext toolContext) {
        // 构建redefine命令的基础部分
        StringBuilder cmd = buildCommand("redefine");

        // 添加要重定义的.class文件路径参数
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

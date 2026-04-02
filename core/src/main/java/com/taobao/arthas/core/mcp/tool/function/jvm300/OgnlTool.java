package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * OGNL 诊断工具类
 * 提供执行 OGNL (Object-Graph Navigation Language) 表达式的功能
 * 对应 Arthas 命令行工具的 ognl 命令
 *
 * 功能说明：
 * - 支持执行任意 OGNL 表达式来获取、修改 Java 对象的属性
 * - 支持指定特定的 ClassLoader 来加载类
 * - 支持控制输出结果的展开层次
 * - 是 Arthas 中最强大的诊断工具之一
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class OgnlTool extends AbstractArthasTool {

    /**
     * ognl 诊断工具方法: 执行 OGNL 表达式
     *
     * 功能说明：
     * - 执行 OGNL 表达式来访问和操作 Java 对象
     * - 可以获取静态字段、调用静态方法、访问实例属性等
     * - 支持特殊的内置变量，如 @类名@静态字段、@类名@静态方法()
     * - 可以指定 ClassLoader 来查找类
     *
     * 使用场景：
     * - 查看类的静态变量值
     * - 调用类的静态方法
     * - 访问实例对象的属性和方法
     * - 获取系统属性、环境变量等
     * - 动态计算和调试
     *
     * 常用表达式示例：
     * - @System@getProperty("java.version")：获取 Java 版本
     * - @TestClass@staticField：获取静态字段值
     * - threadId：获取当前线程 ID
     * - #result：获取上一个命令的返回结果
     *
     * @param expression OGNL 表达式字符串，不能为空
     * @param classLoaderHash ClassLoader 的哈希码（16进制），对应 -c 参数
     *                       可以通过 sc -d 命令查看类的 ClassLoader hash
     * @param classLoaderClass ClassLoader 的完整类名，对应 --classLoaderClass 参数
     *                        例如: sun.misc.Launcher$AppClassLoader
     *                        可以替代 classLoaderHash 使用
     * @param expandLevel 结果对象的展开层次，对应 -x 参数
     *                    默认为 1，值越大展开的层次越深
     *                    用于控制复杂对象的输出详细程度
     * @param toolContext 工具执行上下文，包含会话信息等
     * @return OGNL 表达式执行结果字符串
     */
    @Tool(
        name = "ognl",
        description = "OGNL 诊断工具: 执行 OGNL 表达式，对应 Arthas 的 ognl 命令。"
    )
    public String ognl(
            @ToolParam(description = "OGNL 表达式")
            String expression,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "结果对象展开层次 (-x)，默认 1", required = false)
            Integer expandLevel,

            ToolContext toolContext
    ) {
        // 构建基础命令
        StringBuilder cmd = buildCommand("ognl");

        // 检查是否指定了 ClassLoader 哈希码
        // 如果指定了，使用 -c 参数指定 ClassLoader
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        }
        // 如果没有指定哈希码但指定了 ClassLoader 类名
        // 则使用 --classLoaderClass 参数
        else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 检查是否指定了展开层次
        // 如果指定了且值大于 0，使用 -x 参数
        // 这用于控制复杂对象的输出详细程度
        if (expandLevel != null && expandLevel > 0) {
            cmd.append(" -x ").append(expandLevel);
        }

        // 添加 OGNL 表达式本身
        addParameter(cmd, expression);

        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

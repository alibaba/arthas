package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * GetStatic 诊断工具类
 * 提供查看类的静态字段值的功能，对应 Arthas 的 getstatic 命令
 *
 * 功能特性：
 * - 查看类的静态属性值
 * - 支持通过 ClassLoader 的 hashcode 指定特定的 ClassLoader
 * - 支持通过 ClassLoader 的完整类名指定特定的 ClassLoader
 * - 支持在返回结果上执行 OGNL 表达式进行进一步处理
 */
public class GetStaticTool extends AbstractArthasTool {


    /**
     * 获取类的静态字段值
     *
     * 功能说明：
     * 查看指定类的静态属性值，可以通过 ClassLoader 来精确定位类，
     * 并支持使用 OGNL 表达式对结果进行进一步处理
     *
     * @param classLoaderHash ClassLoader 的 hashcode（16进制字符串），用于指定特定的 ClassLoader
     *                        与 classLoaderClass 参数二选一，优先级高于 classLoaderClass
     * @param classLoaderClass ClassLoader 的完整类名，如 sun.misc.Launcher$AppClassLoader
     *                         可作为 classLoaderHash 的替代方案
     * @param className 类名表达式，支持完全限定名，如 java.lang.String 或 demo.MathGame
     * @param fieldName 静态字段名，要查看的静态属性的名称
     * @param ognlExpression OGNL 表达式，可选参数，用于对返回的静态字段值进行进一步处理
     * @return 诊断结果的字符串形式，包含静态字段的值
     */
    @Tool(
        name = "getstatic",
        description = "GetStatic 诊断工具: 查看类的静态字段值，可指定 ClassLoader，支持在返回结果上执行 OGNL 表达式。对应 Arthas 的 getstatic 命令。"
    )
    public String getstatic(
            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "类名表达式匹配，如java.lang.String或demo.MathGame")
            String className,

            @ToolParam(description = "静态字段名")
            String fieldName,

            @ToolParam(description = "OGNL 表达式", required = false)
            String ognlExpression,

            ToolContext toolContext
    ) {
        // 构建 getstatic 命令的基础部分
        StringBuilder cmd = buildCommand("getstatic");

        // 处理 ClassLoader 参数
        // 优先使用 classLoaderHash（如果提供且不为空）
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            // 添加 -c 参数，后跟 ClassLoader 的 hashcode（16进制）
            addParameter(cmd, "-c", classLoaderHash);
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            // 如果没有提供 hashcode，则使用 classLoaderClass
            // 添加 --classLoaderClass 参数，后跟 ClassLoader 的完整类名
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }
        // 如果两个参数都未提供，则使用默认的 ClassLoader

        // 添加类名参数
        addParameter(cmd, className);

        // 添加字段名参数
        addParameter(cmd, fieldName);

        // 处理 OGNL 表达式参数（可选）
        if (ognlExpression != null && !ognlExpression.trim().isEmpty()) {
            // 如果提供了 OGNL 表达式，则将其追加到命令末尾
            // OGNL 表达式不需要参数前缀，直接附加即可
            cmd.append(" ").append(ognlExpression.trim());
        }

        // 同步执行命令并返回结果
        // executeSync 会等待命令执行完成并返回完整的输出
        return executeSync(toolContext, cmd.toString());
    }
}

package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * VMTool虚拟机工具类
 * 该工具提供了一系列JVM级别的操作，包括查询实例、强制GC、线程中断等功能
 * 继承自AbstractArthasTool，提供vmtool命令的MCP工具接口
 *
 * @author Arthas
 * @see AbstractArthasTool
 */
public class VMToolTool extends AbstractArthasTool {

    /**
     * 获取实例操作常量
     * 用于指定vmtool执行获取实例的操作
     */
    public static final String ACTION_GET_INSTANCES = "getInstances";

    /**
     * 中断线程操作常量
     * 用于指定vmtool执行中断线程的操作
     */
    public static final String ACTION_INTERRUPT_THREAD = "interruptThread";

    /**
     * 执行虚拟机工具操作
     * 该方法根据指定的action类型执行不同的JVM级别操作：
     * - getInstances: 获取指定类的所有实例
     * - forceGc: 强制执行垃圾回收
     * - interruptThread: 中断指定线程
     *
     * @param action 操作类型，支持getInstances/forceGc/interruptThread等，必填
     * @param classLoaderHash ClassLoader的hashcode（16进制），用于指定特定的ClassLoader，可为空
     * @param classLoaderClass ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode，可为空
     * @param className 类名，全限定名，getInstances时使用，可为空
     * @param limit 返回实例限制数量（-l），getInstances时使用，默认10；<=0表示不限制，可为空
     * @param expandLevel 结果对象展开层次（-x），默认1，可为空
     * @param express OGNL表达式，对getInstances返回的instances执行（--express），可为空
     * @param threadId 线程ID（-t），interruptThread时使用，可为空
     * @param toolContext 工具上下文对象，包含执行环境信息
     * @return vmtool命令的执行结果
     * @throws IllegalArgumentException 当action参数为空或interruptThread未指定threadId时抛出
     */
    @Tool(
            name = "vmtool",
            description = "虚拟机工具诊断工具: 查询实例、强制 GC、线程中断等，对应 Arthas 的 vmtool 命令。"
    )
    public String vmtool(
            @ToolParam(description = "操作类型: getInstances/forceGc/interruptThread 等")
            String action,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHash,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "类名，全限定（getInstances 时使用）", required = false)
            String className,

            @ToolParam(description = "返回实例限制数量 (-l)，getInstances 时使用，默认 10；<=0 表示不限制", required = false)
            Integer limit,

            @ToolParam(description = "结果对象展开层次 (-x)，默认 1", required = false)
            Integer expandLevel,

            @ToolParam(description = "OGNL 表达式，对 getInstances 返回的 instances 执行 (--express)", required = false)
            String express,

            @ToolParam(description = "线程 ID (-t)，interruptThread 时使用", required = false)
            Long threadId,

            ToolContext toolContext
    ) {
        // 构建vmtool命令的基础部分
        StringBuilder cmd = buildCommand("vmtool");

        // 校验action参数，不能为空
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("vmtool: action 参数不能为空");
        }

        // 添加操作类型参数（--action）
        cmd.append(" --action ").append(action.trim());

        // 处理ClassLoader相关参数
        // 如果提供了ClassLoader的hashcode，使用-c参数指定
        if (classLoaderHash != null && !classLoaderHash.trim().isEmpty()) {
            addParameter(cmd, "-c", classLoaderHash);
        }
        // 否则，如果提供了ClassLoader的类名，使用--classLoaderClass参数指定
        else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            addParameter(cmd, "--classLoaderClass", classLoaderClass);
        }

        // 如果操作类型是获取实例（getInstances）
        if (ACTION_GET_INSTANCES.equals(action.trim())) {
            // 添加类名参数（--className）
            if (className != null && !className.trim().isEmpty()) {
                addParameter(cmd, "--className", className);
            }

            // 添加返回实例数量限制参数（--limit）
            if (limit != null) {
                cmd.append(" --limit ").append(limit);
            }

            // 添加结果对象展开层次参数（-x），只有大于0时才添加
            if (expandLevel != null && expandLevel > 0) {
                cmd.append(" -x ").append(expandLevel);
            }

            // 添加OGNL表达式参数（--express），用于对返回的实例进行操作
            if (express != null && !express.trim().isEmpty()) {
                addParameter(cmd, "--express", express);
            }
        }

        // 如果操作类型是中断线程（interruptThread）
        if (ACTION_INTERRUPT_THREAD.equals(action.trim())) {
            // 校验threadId参数，必须提供且大于0
            if (threadId != null && threadId > 0) {
                // 添加线程ID参数（-t）
                cmd.append(" -t ").append(threadId);
            } else {
                throw new IllegalArgumentException("vmtool interruptThread 需要指定线程 ID (threadId)");
            }
        }

        // 执行同步命令，返回操作结果
        return executeSync(toolContext, cmd.toString());
    }


}

package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * MBean 诊断工具类
 * 提供 Java 管理扩展 (JMX) MBean 的查询和监控功能
 * 对应 Arthas 命令行工具的 mbean 命令
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class MBeanTool extends AbstractArthasTool {

    /**
     * 默认执行次数
     * 当用户未指定或指定值 <= 0 时使用此默认值
     */
    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;

    /**
     * 默认刷新间隔（毫秒）
     * 用于定时刷新 MBean 属性值，默认为 3000 毫秒（3秒）
     */
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;

    /**
     * mbean 诊断工具方法: 查看或监控 MBean 属性
     *
     * 功能说明：
     * - 查询和展示 MBean 的属性信息
     * - 支持通配符和正则表达式匹配 MBean 名称和属性名
     * - 支持定时刷新监控 MBean 属性值变化
     * - 支持查看 MBean 的元数据信息
     *
     * 支持的参数：
     * - namePattern: MBean 名称表达式，支持通配符或正则（需开启 -E）
     * - attributePattern: 属性名称表达式，支持通配符或正则（需开启 -E）
     * - metadata: 是否查看元信息 (-m)
     * - intervalMs: 刷新属性值时间间隔 (ms) (-i)，required=false
     * - numberOfExecutions: 刷新次数 (-n)，若未指定或 <= 0 则使用 DEFAULT_NUMBER_OF_EXECUTIONS
     * - regex: 是否启用正则匹配 (-E)，required=false
     *
     * @param namePattern MBean 名称表达式，例如: java.lang:type=GarbageCollector,name=*
     * @param attributePattern 属性名表达式，支持通配符，例如: CollectionCount
     * @param metadata 是否查看元信息，对应 -m 参数
     * @param intervalMs 刷新间隔（毫秒），对应 -i 参数
     * @param numberOfExecutions 执行次数限制，对应 -n 参数
     * @param regex 是否开启正则表达式匹配，对应 -E 参数
     * @param toolContext 工具执行上下文，包含会话信息等
     * @return 执行结果字符串
     */
    @Tool(
        name = "mbean",
        description = "MBean 诊断工具: 查看或监控 MBean 属性信息，对应 Arthas 的 mbean 命令。"
    )
    public String mbean(
            @ToolParam(description = "MBean名称表达式匹配，如java.lang:type=GarbageCollector,name=*")
            String namePattern,

            @ToolParam(description = "属性名表达式匹配，支持通配符如CollectionCount", required = false)
            String attributePattern,

            @ToolParam(description = "是否查看元信息 (-m)", required = false)
            Boolean metadata,

            @ToolParam(description = "刷新间隔，单位为毫秒，默认 3000ms。用于控制输出频率", required = false)
            Integer intervalMs,

            @ToolParam(description = "执行次数限制，默认值为 1。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            ToolContext toolContext
    ) {
        // 判断是否需要流式输出
        // 如果指定了刷新间隔（>0）或执行次数（>0），则需要开启流式输出
        boolean needStreamOutput = (intervalMs != null && intervalMs > 0) || (numberOfExecutions != null && numberOfExecutions > 0);

        // 获取刷新间隔，如果用户未指定则使用默认值
        int interval = getDefaultValue(intervalMs, DEFAULT_REFRESH_INTERVAL_MS);

        // 获取执行次数，如果用户未指定则使用默认值
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        // 构建基础命令
        StringBuilder cmd = buildCommand("mbean");

        // 添加 -m 参数（是否查看元信息）
        addFlag(cmd, "-m", metadata);

        // 添加 -E 参数（是否开启正则表达式）
        addFlag(cmd, "-E", regex);

        // 只有在需要流式输出且不是查看元数据时才添加 -i 和 -n 参数
        // 因为查看元数据是一次性操作，不需要刷新
        if (needStreamOutput && !Boolean.TRUE.equals(metadata)) {
            // 添加刷新间隔参数
            cmd.append(" -i ").append(interval);
            // 添加执行次数参数
            cmd.append(" -n ").append(execCount);
        }

        // 添加 MBean 名称表达式
        if (namePattern != null && !namePattern.trim().isEmpty()) {
            cmd.append(" ").append(namePattern.trim());
        }

        // 添加属性名称表达式
        if (attributePattern != null && !attributePattern.trim().isEmpty()) {
            cmd.append(" ").append(attributePattern.trim());
        }

        // 记录日志
        logger.info("Starting mbean execution: {}", cmd.toString());

        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }
}

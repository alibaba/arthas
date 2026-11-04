package com.taobao.arthas.core.mcp.tool.function.jvm300;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

public class MBeanTool extends AbstractArthasTool {

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;

    /**
     * mbean 诊断工具: 查看或监控 MBean 属性
     * 支持:
     * - namePattern: MBean 名称表达式，支持通配符或正则（需开启 -E）
     * - attributePattern: 属性名称表达式，支持通配符或正则（需开启 -E）
     * - metadata: 是否查看元信息 (-m)
     * - intervalMs: 刷新属性值时间间隔 (ms) (-i)，required=false
     * - numberOfExecutions: 刷新次数 (-n)，若未指定或 <=0 则使用 DEFAULT_NUMBER_OF_EXECUTIONS
     * - regex: 是否启用正则匹配 (-E)，required=false
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
        boolean needStreamOutput = (intervalMs != null && intervalMs > 0) || (numberOfExecutions != null && numberOfExecutions > 0);
        
        int interval = getDefaultValue(intervalMs, DEFAULT_REFRESH_INTERVAL_MS);
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        StringBuilder cmd = buildCommand("mbean");

        addFlag(cmd, "-m", metadata);
        addFlag(cmd, "-E", regex);
        
        // 只有在需要流式输出且不是查看元数据时才添加 -i 和 -n 参数
        if (needStreamOutput && !Boolean.TRUE.equals(metadata)) {
            cmd.append(" -i ").append(interval);
            cmd.append(" -n ").append(execCount);
        }
        
        if (namePattern != null && !namePattern.trim().isEmpty()) {
            cmd.append(" ").append(namePattern.trim());
        }
        if (attributePattern != null && !attributePattern.trim().isEmpty()) {
            cmd.append(" ").append(attributePattern.trim());
        }

        logger.info("Starting mbean execution: {}", cmd.toString());
        return executeSync(toolContext, cmd.toString());
    }
}

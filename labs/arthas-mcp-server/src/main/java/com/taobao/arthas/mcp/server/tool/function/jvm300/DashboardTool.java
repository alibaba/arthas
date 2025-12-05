package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class DashboardTool extends AbstractArthasTool {

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 3000;

    /**
     * dashboard 实时面板命令
     * 支持:
     * - intervalMs: 刷新间隔，单位 ms，默认 3000ms
     * - count: 刷新次数限制，即 -n 参数；如果不指定则使用 DEFAULT_NUMBER_OF_EXECUTIONS (3次)
     */
    @Tool(
            name = "dashboard",
            description = "Dashboard 诊断工具: 实时展示 JVM/应用面板，可利用参数控制诊断次数与间隔。对应 Arthas 的 dashboard 命令。",
            streamable = true
    )
    public String dashboard(
            @ToolParam(description = "刷新间隔，单位为毫秒，默认 3000ms。用于控制输出频率", required = false)
            Integer intervalMs,

            @ToolParam(description = "执行次数限制，默认值为 3。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            ToolContext toolContext
    ) {
        int interval = getDefaultValue(intervalMs, DEFAULT_REFRESH_INTERVAL_MS);
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        StringBuilder cmd = buildCommand("dashboard");
        cmd.append(" -i ").append(interval);
        cmd.append(" -n ").append(execCount);

        // 仪表板通常运行固定次数，超时时间基于 (次数 * 间隔) + 缓冲时间
        int calculatedTimeoutMs = execCount * interval + 5000;

        return executeStreamable(toolContext, cmd.toString(), execCount, interval / 10, calculatedTimeoutMs,
                "Dashboard execution completed successfully");
    }
}

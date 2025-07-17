package com.taobao.arthas.mcp.server.tool.function.jvm300;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.ArthasCommandExecutor;

public class PerfCounterTool {

    @Tool(
        name = "perfcounter",
        description = "PerfCounter 诊断工具: 查看 JVM Perf Counter 信息，对应 Arthas 的 perfcounter 命令。"
    )
    public String perfcounter(
            @ToolParam(description = "是否打印更多详情 (-d)", required = false)
            Boolean detailed
    ) {
        StringBuilder cmd = new StringBuilder("perfcounter");
        if (Boolean.TRUE.equals(detailed)) {
            cmd.append(" -d");
        }
        String commandStr = cmd.toString();
        return ArthasCommandExecutor.executeCommand(commandStr);
    }
}

package com.taobao.arthas.mcp.server.tool.function.monitor200;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class StackTool extends AbstractArthasTool {

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_POLL_INTERVAL_MS = 50;

    /**
     * stack 调用堆栈跟踪工具
     * 输出当前方法被调用的调用路径
     * 支持:
     * - classPattern: 类名表达式匹配，支持通配符
     * - methodPattern: 方法名表达式匹配，支持通配符 
     * - condition: OGNL条件表达式，满足条件的调用才会被跟踪
     * - numberOfExecutions: 捕获次数限制，达到指定次数后自动停止
     * - regex: 是否开启正则表达式匹配，默认false
     * - excludeClassPattern: 排除的类名模式
     */
    @Tool(
        name = "stack",
        description = "Stack 调用堆栈跟踪工具: 输出当前方法被调用的调用路径，帮助分析方法的调用链路。对应 Arthas 的 stack 命令。",
        streamable = true
    )
    public String stack(
            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame")
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors", required = false)
            String methodPattern,

            @ToolParam(description = "OGNL条件表达式，满足条件的调用才会被跟踪，如params[0]<0", required = false)
            String condition,

            @ToolParam(description = "捕获次数限制，默认值为3。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            ToolContext toolContext
    ) {
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        StringBuilder cmd = buildCommand("stack");
        cmd.append(" -n ").append(execCount);
        
        addFlag(cmd, "-E", regex);
        addParameter(cmd, classPattern);
        
        if (methodPattern != null && !methodPattern.trim().isEmpty()) {
            cmd.append(" ").append(methodPattern.trim());
        }
        
        addQuotedParameter(cmd, condition);

        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS, 
                                "Stack execution completed successfully");
    }
}

package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import static com.taobao.arthas.core.mcp.tool.function.StreamableToolUtils.createErrorResponse;

/**
 * line 源码行探针工具
 * 在指定源码行插入探针，观察方法入参、局部变量和表达式结果
 */
public class LineTool extends AbstractArthasTool {

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;
    public static final int DEFAULT_POLL_INTERVAL_MS = 100;
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;
    public static final int DEFAULT_EXPAND_LEVEL = 1;
    public static final int MAX_EXPAND_LEVEL = 4;
    public static final int DEFAULT_STACK_DEPTH = 32;
    public static final int MAX_STACK_DEPTH = 256;
    public static final String DEFAULT_EXPRESS = "{params, localVarMap}";

    /**
     * line 源码行探针工具
     * 在指定源码行观察方法入参、局部变量和表达式结果
     * 支持:
     * - classPattern: 类名表达式匹配，支持通配符
     * - methodPattern: 方法名表达式匹配，支持通配符
     * - methodDesc: JVM 方法描述符，用于区分重载方法
     * - line: 源码行号，支持逗号分隔
     * - express: 观察表达式，默认 {params, localVarMap}
     * - condition: OGNL 条件表达式，条件成立时才输出
     * - listLines: 只列出可插桩行号，不增强类
     * - regex: 是否开启正则表达式匹配，默认 false
     * - expandLevel: 输出对象展开层级，默认 1，最大 4
     * - maxMatchCount: 最大匹配类数量，防止匹配过多类影响性能，默认 50
     * - sizeLimit: 输出结果大小上限
     * - stack: 命中时是否输出当前线程栈
     * - stackDepth: 线程栈最大深度，默认 32，最大 256
     */
    @Tool(
        name = "line",
        description = "Line 源码行探针工具: 在指定源码行插入探针，观察该行处的方法入参、局部变量和表达式结果，对应 Arthas 的 line 命令。"
                + "建议先用 listLines=true 查看可插桩行号（不增强类）；探针命中次数由 numberOfExecutions 控制，达到次数或超时后自动退出。",
        streamable = true,
        taskSupport = McpSchema.TaskSupportMode.OPTIONAL
    )
    public String line(
            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame")
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors；不指定时匹配类中所有包含目标行的可插桩方法", required = false)
            String methodPattern,

            @ToolParam(description = "JVM 方法描述符，用于区分重载方法，如(I)Ljava/util/List;", required = false)
            String methodDesc,

            @ToolParam(description = "源码行号，支持逗号分隔，如51,57；单次最多 256 行。listLines=true 时无需指定", required = false)
            String line,

            @ToolParam(description = "观察表达式，默认为{params, localVarMap}，支持 OGNL 表达式", required = false)
            String express,

            @ToolParam(description = "OGNL 条件表达式，条件成立时才输出，如localVarMap[\"i\"] != null && localVarMap[\"i\"] > 10", required = false)
            String condition,

            @ToolParam(description = "只列出可插桩的源码行号，不增强目标类，默认false", required = false)
            Boolean listLines,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "输出结果的属性遍历深度，默认1，最大值4", required = false)
            Integer expandLevel,

            @ToolParam(description = "指定 Class 最大匹配数量，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "输出结果大小上限(字节)。对应 line -M/--sizeLimit，默认使用 options 中的 object-size-limit", required = false)
            Integer sizeLimit,

            @ToolParam(description = "探针命中时输出当前线程栈，默认false", required = false)
            Boolean stack,

            @ToolParam(description = "线程栈最大深度，默认32，最大256，仅在 stack=true 时生效", required = false)
            Integer stackDepth,

            @ToolParam(description = "执行次数限制，默认值为" + DEFAULT_NUMBER_OF_EXECUTIONS + "。达到指定次数后自动停止", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认" + AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS + "秒。超时后命令自动退出", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        if (classPattern == null || classPattern.trim().isEmpty()) {
            return JsonParser.toJson(createErrorResponse("line 的 classPattern 参数不能为空"));
        }

        boolean listOnly = Boolean.TRUE.equals(listLines);
        if (!listOnly && (line == null || line.trim().isEmpty())) {
            return JsonParser.toJson(createErrorResponse(
                    "line 参数不能为空；如只需查看可插桩行号，请设置 listLines=true"));
        }

        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);
        int maxMatch = getDefaultValue(maxMatchCount, DEFAULT_MAX_MATCH_COUNT);
        int expandDepth = (expandLevel != null && expandLevel >= 1 && expandLevel <= MAX_EXPAND_LEVEL)
                ? expandLevel : DEFAULT_EXPAND_LEVEL;
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        StringBuilder cmd = buildCommand("line");

        cmd.append(" --timeout ").append(timeoutSeconds);
        cmd.append(" -m ").append(maxMatch);

        addFlag(cmd, "-E", regex);

        addParameter(cmd, "--class", classPattern);
        addParameter(cmd, "--method", methodPattern);

        if (methodDesc != null && !methodDesc.trim().isEmpty()) {
            cmd.append(" --desc ");
            addQuotedParameter(cmd, methodDesc);
        }

        // list-lines 只读取 LineNumberTable，不增强类，执行完立即返回
        if (listOnly) {
            cmd.append(" --list-lines");
            return executeSync(toolContext, cmd.toString());
        }

        cmd.append(" -n ").append(execCount);
        cmd.append(" -x ").append(expandDepth);
        if (sizeLimit != null && sizeLimit > 0) {
            cmd.append(" -M ").append(sizeLimit);
        }

        addFlag(cmd, "--stack", stack);
        addParameter(cmd, "--line", line);

        cmd.append(" --express ");
        addQuotedParameter(cmd, getDefaultValue(express, DEFAULT_EXPRESS));

        if (condition != null && !condition.trim().isEmpty()) {
            cmd.append(" --condition ");
            addQuotedParameter(cmd, condition);
        }

        if (Boolean.TRUE.equals(stack)) {
            int depth = (stackDepth != null && stackDepth >= 1 && stackDepth <= MAX_STACK_DEPTH)
                    ? stackDepth : DEFAULT_STACK_DEPTH;
            cmd.append(" --stack-depth ").append(depth);
        }

        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS,
                timeoutSeconds * 1000, "Line execution completed successfully");
    }
}

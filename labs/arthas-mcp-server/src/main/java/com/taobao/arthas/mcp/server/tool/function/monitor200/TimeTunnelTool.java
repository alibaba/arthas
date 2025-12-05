package com.taobao.arthas.mcp.server.tool.function.monitor200;

import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.function.AbstractArthasTool;

public class TimeTunnelTool extends AbstractArthasTool {

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 3;
    public static final int DEFAULT_POLL_INTERVAL_MS = 100;
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;

    /**
     * tt 时空隧道工具 (TimeTunnel)
     * 方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测
     */
    @Tool(
            name = "tt",
            description = "TimeTunnel 时空隧道工具: 方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，对应 Arthas 的 tt 命令。支持记录、列表、搜索、查看详情、重放、删除等操作。",
            streamable = true
    )
    public String timeTunnel(
            @ToolParam(description = "操作类型: record/t(记录), list/l(列表), search/s(搜索), info/i(查看详情), replay/p(重放), delete/d(删除), deleteAll/da(删除所有)，默认record")
            String action,

            @ToolParam(description = "类名表达式匹配，支持通配符，如demo.MathGame或*Test。record操作时必需", required = false)
            String classPattern,

            @ToolParam(description = "方法名表达式匹配，支持通配符，如primeFactors或*method。record操作时必需", required = false)
            String methodPattern,

            @ToolParam(description = "OGNL条件表达式，满足条件的调用才会被记录，如params[0]<0或'params.length==1'", required = false)
            String condition,

            @ToolParam(description = "记录次数限制，默认值为 3。达到指定次数后自动停止（仅record操作）", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "指定索引，用于info/replay/delete等操作", required = false)
            Integer index,

            @ToolParam(description = "搜索表达式，用于search操作，支持OGNL表达式如'method.name==\"primeFactors\"'", required = false)
            String searchExpression,

            @ToolParam(description = "Class最大匹配数量，防止匹配到的Class数量太多导致JVM挂起，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认200秒。超时后命令自动退出（仅record操作）", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        String ttAction = normalizeAction(action);
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);
        int maxMatch = getDefaultValue(maxMatchCount, DEFAULT_MAX_MATCH_COUNT);
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        validateParameters(ttAction, classPattern, methodPattern, index, searchExpression);

        StringBuilder cmd = buildCommand("tt");

        switch (ttAction) {
            case "record":
                cmd = buildRecordCommand(cmd, classPattern, methodPattern, condition, execCount, maxMatch, regex, timeoutSeconds);
                break;
            case "list":
                cmd = buildListCommand(cmd, searchExpression);
                break;
            case "info":
                cmd.append(" -i ").append(index);
                break;
            case "search":
                cmd.append(" -s '").append(searchExpression.trim()).append("'");
                break;
            case "replay":
                cmd.append(" -i ").append(index).append(" -p");
                break;
            case "delete":
                cmd.append(" -i ").append(index).append(" -d");
                break;
            case "deleteall":
                cmd.append(" --delete-all");
                break;
            default:
                throw new IllegalArgumentException("Unsupported action: " + ttAction +
                        ". Supported actions: record(t), list(l), info(i), search(s), replay(p), delete(d), deleteAll(da)");
        }

        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS, timeoutSeconds * 1000,
                "TimeTunnel recording completed successfully");
    }

    /**
     * 验证参数
     */
    private void validateParameters(String action, String classPattern, String methodPattern, 
                                   Integer index, String searchExpression) {
        switch (action) {
            case "record":
                if (classPattern == null || classPattern.trim().isEmpty()) {
                    throw new IllegalArgumentException("classPattern is required for record operation");
                }
                if (methodPattern == null || methodPattern.trim().isEmpty()) {
                    throw new IllegalArgumentException("methodPattern is required for record operation");
                }
                break;
            case "info":
            case "replay":
                if (index == null) {
                    throw new IllegalArgumentException(action + " operation requires index parameter");
                }
                break;
            case "search":
                if (searchExpression == null || searchExpression.trim().isEmpty()) {
                    throw new IllegalArgumentException("search operation requires searchExpression parameter");
                }
                break;
            case "delete":
                if (index == null) {
                    throw new IllegalArgumentException("delete operation requires index parameter");
                }
                break;
            case "list":
            case "deleteall":
                break;
            default:
                throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    /**
     * 标准化操作类型
     */
    private String normalizeAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return "record";
        }

        String normalizedAction = action.trim().toLowerCase();

        switch (normalizedAction) {
            case "record":
            case "r":
            case "-t":
            case "t":
                return "record";

            case "list":
            case "l":
            case "-l":
                return "list";

            case "info":
            case "i":
            case "-i":
                return "info";

            case "search":
            case "s":
            case "-s":
                return "search";

            case "replay":
            case "p":
            case "-p":
                return "replay";

            case "delete":
            case "d":
            case "-d":
                return "delete";

            case "deleteall":
            case "da":
            case "--delete-all":
                return "deleteall";

            default:
                return normalizedAction;
        }
    }

    private StringBuilder buildRecordCommand(StringBuilder cmd, String classPattern, String methodPattern,
                                             String condition, int execCount, int maxMatch, Boolean regex, int timeoutSeconds) {

        cmd.append(" -t");

        cmd.append(" --timeout ").append(timeoutSeconds);
        cmd.append(" -n ").append(execCount);
        cmd.append(" -m ").append(maxMatch);

        if (Boolean.TRUE.equals(regex)) {
            cmd.append(" -E");
        }

        cmd.append(" '").append(classPattern.trim()).append("'");
        cmd.append(" '").append(methodPattern.trim()).append("'");

        if (condition != null && !condition.trim().isEmpty()) {
            cmd.append(" '").append(condition.trim()).append("'");
        }

        return cmd;
    }

    private StringBuilder buildListCommand(StringBuilder cmd, String searchExpression) {
        cmd.append(" -l");
        if (searchExpression != null && !searchExpression.trim().isEmpty()) {
            cmd.append(" '").append(searchExpression.trim()).append("'");
        }
        return cmd;
    }

}

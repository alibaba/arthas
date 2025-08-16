package com.taobao.arthas.mcp.server.tool.function.monitor200;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.taobao.arthas.mcp.server.tool.util.McpToolUtils.*;
import static com.taobao.arthas.mcp.server.tool.function.StreamableToolUtils.*;

public class TimeTunnelTool {

    private static final Logger logger = LoggerFactory.getLogger(TimeTunnelTool.class);

    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 100;
    public static final int DEFAULT_POLL_INTERVAL_MS = 100;
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;
    public static final int DEFAULT_REPLAY_TIMES = 1;
    public static final int DEFAULT_REPLAY_INTERVAL = 1000;
    public static final int DEFAULT_EXPAND_LEVEL = 1;

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

            @ToolParam(description = "记录次数限制，默认值为100。达到指定次数后自动停止（仅record操作）", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "排除的类名模式，支持通配符", required = false)
            String excludeClassPattern,

            @ToolParam(description = "指定索引，用于info/replay/delete等操作", required = false)
            Integer index,

            @ToolParam(description = "索引范围或模式，用于delete操作，如1001-1010或单个索引", required = false)
            String indexPattern,

            @ToolParam(description = "搜索表达式，用于search操作，支持OGNL表达式如'method.name==\"primeFactors\"'", required = false)
            String searchExpression,

            @ToolParam(description = "观察表达式，使用OGNL表达式，如target.field、params[0]、returnObj，默认为{params,returnObj,throwExp}", required = false)
            String watchExpression,

            @ToolParam(description = "输出结果属性遍历深度，默认为1", required = false)
            Integer expandLevel,

            @ToolParam(description = "详细输出模式，显示调用的详细信息", required = false)
            Boolean verbose,

            @ToolParam(description = "重放次数，用于replay操作，默认1次", required = false)
            Integer replayTimes,

            @ToolParam(description = "多次重放间隔时间，单位毫秒，默认1000ms", required = false)
            Integer replayInterval,

            @ToolParam(description = "Class最大匹配数量，防止匹配到的Class数量太多导致JVM挂起，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "ClassLoader的hashcode（16进制），用于指定特定的ClassLoader", required = false)
            String classLoaderHashcode,

            @ToolParam(description = "ClassLoader的完整类名，如sun.misc.Launcher$AppClassLoader，可替代hashcode", required = false)
            String classLoaderClass,

            @ToolParam(description = "是否包含子类，默认为false", required = false)
            Boolean includeSubClass,

            @ToolParam(description = "是否跳过JDK的方法，默认为true", required = false)
            Boolean skipJdkMethod,

            @ToolParam(description = "监听耗时超过指定时间的调用，单位为毫秒", required = false)
            Integer costThreshold,

            ToolContext toolContext
    ) {
        McpNettyServerExchange exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        ArthasCommandContext commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
        Integer progressToken = (Integer) toolContext.getContext().get(PROGRESS_TOKEN);

        // 设置默认操作类型
        String ttAction = normalizeAction(action);
        int execCount = (numberOfExecutions != null && numberOfExecutions > 0) ? numberOfExecutions : DEFAULT_NUMBER_OF_EXECUTIONS;
        int maxMatch = (maxMatchCount != null && maxMatchCount > 0) ? maxMatchCount : DEFAULT_MAX_MATCH_COUNT;
        int expandLv = (expandLevel != null && expandLevel > 0) ? expandLevel : DEFAULT_EXPAND_LEVEL;

        // 验证必需参数
        validateParameters(ttAction, classPattern, methodPattern, index, searchExpression, indexPattern);

        try {
            StringBuilder cmd = new StringBuilder("tt");

            // 根据不同的操作类型构建命令
            switch (ttAction) {
                case "record":
                    cmd = buildRecordCommand(cmd, classPattern, methodPattern, condition, execCount, maxMatch,
                            regex, excludeClassPattern, classLoaderHashcode, classLoaderClass,
                            includeSubClass, expandLv, skipJdkMethod, costThreshold, verbose);
                    break;

                case "list":
                    cmd = buildListCommand(cmd, searchExpression);
                    break;

                case "info":
                    cmd = buildInfoCommand(cmd, index, expandLv, watchExpression);
                    break;

                case "search":
                    cmd = buildSearchCommand(cmd, searchExpression, expandLv);
                    break;

                case "replay":
                    cmd = buildReplayCommand(cmd, index, expandLv, replayTimes, replayInterval);
                    break;

                case "delete":
                    cmd = buildDeleteCommand(cmd, index, indexPattern);
                    break;

                case "deleteall":
                    cmd = buildDeleteAllCommand(cmd);
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported action: " + ttAction +
                            ". Supported actions: record(t), list(l), info(i), search(s), replay(p), delete(d), deleteAll(da)");
            }

            String commandStr = cmd.toString();
            logger.info("Starting TimeTunnel execution: {}", commandStr);

            if ("record".equals(ttAction)) {
                Map<String, Object> asyncResult = commandContext.executeAsync(commandStr);
                logger.debug("Async execution started: {}", asyncResult);
                boolean success = pullResultsSync(exchange, commandContext, execCount, DEFAULT_POLL_INTERVAL_MS, progressToken);
                if (success) {
                    return JsonParser.toJson(createCompletedResponse("TimeTunnel recording completed successfully"));
                } else {
                    return JsonParser.toJson(createErrorResponse("TimeTunnel recording failed due to timeout or error limits exceeded"));
                }
            } else {
                // 其他操作（list/info/search/replay/delete/deleteAll）：使用同步执行，直接返回结果
                logger.info("Executing sync tt command: {}", commandStr);
                Map<String, Object> result = commandContext.executeSync(commandStr);
                return JsonParser.toJson(result);
            }

        } catch (Exception e) {
            logger.error("Error executing tt command", e);
            return JsonParser.toJson(createErrorResponse("Error executing tt: " + e.getMessage()));
        }
    }

    /**
     * 验证参数
     */
    private void validateParameters(String action, String classPattern, String methodPattern, 
                                   Integer index, String searchExpression, String indexPattern) {
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
                if (index == null && (indexPattern == null || indexPattern.trim().isEmpty())) {
                    throw new IllegalArgumentException("delete operation requires index or indexPattern parameter");
                }
                break;
            case "list":
            case "deleteall":
                // 这些操作不需要额外参数
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

        // 支持各种别名
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
                                             String condition, int execCount, int maxMatch, Boolean regex,
                                             String excludeClassPattern, String classLoaderHashcode, String classLoaderClass,
                                             Boolean includeSubClass, Integer expandLevel, Boolean skipJdkMethod,
                                             Integer costThreshold, Boolean verbose) {

        cmd.append(" -t");

        cmd.append(" -n ").append(execCount);
        cmd.append(" -m ").append(maxMatch);

        if (Boolean.TRUE.equals(regex)) {
            cmd.append(" -E");
        }

        if (Boolean.FALSE.equals(skipJdkMethod)) {
            cmd.append(" --skipJDKMethod false");
        }

        if (classLoaderHashcode != null && !classLoaderHashcode.trim().isEmpty()) {
            cmd.append(" -c ").append(classLoaderHashcode.trim());
        } else if (classLoaderClass != null && !classLoaderClass.trim().isEmpty()) {
            cmd.append(" --classLoaderClass '").append(classLoaderClass.trim()).append("'");
        }

        if (Boolean.TRUE.equals(includeSubClass)) {
            cmd.append(" --include-sub-class");
        }

        if (expandLevel != null && expandLevel > 0) {
            cmd.append(" -x ").append(expandLevel);
        }

        if (costThreshold != null && costThreshold > 0) {
            cmd.append(" --cost ").append(costThreshold);
        }

        if (Boolean.TRUE.equals(verbose)) {
            cmd.append(" -v");
        }

        if (excludeClassPattern != null && !excludeClassPattern.trim().isEmpty()) {
            cmd.append(" --exclude-class-pattern '").append(excludeClassPattern.trim()).append("'");
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
        // 支持可选的搜索表达式
        if (searchExpression != null && !searchExpression.trim().isEmpty()) {
            cmd.append(" '").append(searchExpression.trim()).append("'");
        }
        return cmd;
    }

    private StringBuilder buildInfoCommand(StringBuilder cmd, Integer index, Integer expandLevel, String watchExpression) {
        cmd.append(" -i ").append(index);

        if (expandLevel != null && expandLevel > 0) {
            cmd.append(" -x ").append(expandLevel);
        }
        if (watchExpression != null && !watchExpression.trim().isEmpty()) {
            cmd.append(" -w '").append(watchExpression.trim()).append("'");
        }
        return cmd;
    }

    private StringBuilder buildSearchCommand(StringBuilder cmd, String searchExpression, Integer expandLevel) {
        cmd.append(" -s '").append(searchExpression.trim()).append("'");
        if (expandLevel != null && expandLevel > 0) {
            cmd.append(" -x ").append(expandLevel);
        }
        return cmd;
    }

    private StringBuilder buildReplayCommand(StringBuilder cmd, Integer index, Integer expandLevel,
                                             Integer replayTimes, Integer replayInterval) {
        cmd.append(" -i ").append(index).append(" -p");
        if (expandLevel != null && expandLevel > 0) {
            cmd.append(" -x ").append(expandLevel);
        }

        int times = (replayTimes != null && replayTimes > 0) ? replayTimes : DEFAULT_REPLAY_TIMES;
        if (times > 1) {
            cmd.append(" --replay-times ").append(times);
        }

        int interval = (replayInterval != null && replayInterval > 0) ? replayInterval : DEFAULT_REPLAY_INTERVAL;
        if (times > 1 && interval != DEFAULT_REPLAY_INTERVAL) {
            cmd.append(" --replay-interval ").append(interval);
        }

        return cmd;
    }
    private StringBuilder buildDeleteCommand(StringBuilder cmd, Integer index, String indexPattern) {
        if (index != null) {
            cmd.append(" -i ").append(index).append(" -d");
        } else {
            cmd.append(" -d ").append(indexPattern.trim());
        }
        return cmd;
    }
    private StringBuilder buildDeleteAllCommand(StringBuilder cmd) {
        cmd.append(" --delete-all");
        return cmd;
    }
}
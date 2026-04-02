package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * 时空隧道工具（TimeTunnel Tool）
 * 对应 Arthas 的 tt 命令
 *
 * 功能说明：
 * 方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息
 * 并能对这些不同时间下的调用进行观测、重放、分析
 *
 * 使用场景：
 * - 记录方法调用信息（入参、返回值、耗时等）
 * - 查看历史调用记录列表
 * - 搜索满足特定条件的调用记录
 * - 查看某次调用的详细信息
 * - 重放历史调用（可用于调试）
 * - 删除指定的调用记录
 * - 删除所有调用记录
 */
public class TimeTunnelTool extends AbstractArthasTool {

    /**
     * 默认记录次数
     * 默认只记录 1 次方法调用后就停止
     */
    public static final int DEFAULT_NUMBER_OF_EXECUTIONS = 1;

    /**
     * 默认轮询间隔（毫秒）
     * 每隔 100ms 检查一次命令执行状态
     */
    public static final int DEFAULT_POLL_INTERVAL_MS = 100;

    /**
     * 默认最大匹配类数量
     * 防止匹配到的类数量太多导致 JVM 挂起
     */
    public static final int DEFAULT_MAX_MATCH_COUNT = 50;

    /**
     * 时空隧道工具主方法
     *
     * 对应 Arthas 命令：tt [options]
     *
     * 支持的操作类型：
     * - record/t: 记录方法调用
     * - list/l: 查看记录列表
     * - search/s: 搜索记录
     * - info/i: 查看记录详情
     * - replay/p: 重放记录
     * - delete/d: 删除记录
     * - deleteAll/da: 删除所有记录
     *
     * @param action 操作类型
     *               支持的值：
     *               - record/t: 记录方法调用
     *               - list/l: 查看记录列表
     *               - search/s: 搜索记录
     *               - info/i: 查看记录详情
     *               - replay/p: 重放记录
     *               - delete/d: 删除记录
     *               - deleteAll/da: 删除所有记录
     *               默认为 record
     *
     * @param classPattern 类名表达式匹配
     *                     支持通配符，如 demo.MathGame 或 *Test
     *                     record 操作时必需
     *
     * @param methodPattern 方法名表达式匹配
     *                      支持通配符，如 primeFactors 或 *method
     *                      record 操作时必需
     *
     * @param condition OGNL条件表达式
     *                  满足条件的调用才会被记录
     *                  例如：params[0]<0 或 'params.length==1'
     *
     * @param numberOfExecutions 记录次数限制
     *                           默认值为 1
     *                           达到指定次数后自动停止（仅 record 操作）
     *
     * @param regex 是否开启正则表达式匹配
     *              默认为通配符匹配
     *              默认值为 false
     *
     * @param index 记录索引
     *              用于 info/replay/delete 等操作
     *              指定要操作的具体记录
     *
     * @param searchExpression 搜索表达式
     *                         用于 search 操作
     *                         支持OGNL表达式，如 'method.name=="primeFactors"'
     *
     * @param maxMatchCount Class最大匹配数量
     *                      防止匹配到的Class数量太多导致JVM挂起
     *                      默认值为 50
     *
     * @param sizeLimit 输出结果大小上限（字节）
     *                  对应 tt -M/--sizeLimit
     *                  默认值为 10 * 1024 * 1024（10MB）
     *
     * @param timeout 命令执行超时时间
     *                单位为秒
     *                默认值为 AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS
     *                超时后命令自动退出（仅 record 操作）
     *
     * @param toolContext 工具执行上下文
     *                    包含执行环境、会话信息等
     *
     * @return 命令执行结果
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

            @ToolParam(description = "记录次数限制，默认值为 1。达到指定次数后自动停止（仅record操作）", required = false)
            Integer numberOfExecutions,

            @ToolParam(description = "开启正则表达式匹配，默认为通配符匹配，默认false", required = false)
            Boolean regex,

            @ToolParam(description = "指定索引，用于info/replay/delete等操作", required = false)
            Integer index,

            @ToolParam(description = "搜索表达式，用于search操作，支持OGNL表达式如'method.name==\"primeFactors\"'", required = false)
            String searchExpression,

            @ToolParam(description = "Class最大匹配数量，防止匹配到的Class数量太多导致JVM挂起，默认50", required = false)
            Integer maxMatchCount,

            @ToolParam(description = "输出结果大小上限(字节)。对应 tt -M/--sizeLimit，默认 10 * 1024 * 1024", required = false)
            Integer sizeLimit,

            @ToolParam(description = "命令执行超时时间，单位为秒，默认" + AbstractArthasTool.DEFAULT_TIMEOUT_SECONDS +  "秒。超时后命令自动退出（仅record操作）", required = false)
            Integer timeout,

            ToolContext toolContext
    ) {
        // 标准化操作类型，将各种简写形式转换为标准名称
        String ttAction = normalizeAction(action);

        // 获取记录次数，如果未指定则使用默认值 1
        int execCount = getDefaultValue(numberOfExecutions, DEFAULT_NUMBER_OF_EXECUTIONS);

        // 获取最大匹配数量，如果未指定则使用默认值 50
        int maxMatch = getDefaultValue(maxMatchCount, DEFAULT_MAX_MATCH_COUNT);

        // 获取超时时间，如果未指定则使用默认值
        int timeoutSeconds = getDefaultValue(timeout, DEFAULT_TIMEOUT_SECONDS);

        // 验证参数是否符合各操作的要求
        validateParameters(ttAction, classPattern, methodPattern, index, searchExpression);

        // 构建命令基础部分：tt
        StringBuilder cmd = buildCommand("tt");

        // 添加输出结果大小上限参数
        if (sizeLimit != null && sizeLimit > 0) {
            cmd.append(" -M ").append(sizeLimit);
        }

        // 根据操作类型构建不同的命令
        switch (ttAction) {
            case "record":
                // 构建记录命令
                cmd = buildRecordCommand(cmd, classPattern, methodPattern, condition, execCount, maxMatch, regex, timeoutSeconds);
                break;
            case "list":
                // 构建列表命令
                cmd = buildListCommand(cmd, searchExpression);
                break;
            case "info":
                // 构建查看详情命令
                cmd.append(" -i ").append(index);
                break;
            case "search":
                // 构建搜索命令
                cmd.append(" -s '").append(searchExpression.trim()).append("'");
                break;
            case "replay":
                // 构建重放命令
                cmd.append(" -i ").append(index).append(" -p");
                break;
            case "delete":
                // 构建删除命令
                cmd.append(" -i ").append(index).append(" -d");
                break;
            case "deleteall":
                // 构建删除所有命令
                cmd.append(" --delete-all");
                break;
            default:
                // 不支持的操作类型
                throw new IllegalArgumentException("Unsupported action: " + ttAction +
                        ". Supported actions: record(t), list(l), info(i), search(s), replay(p), delete(d), deleteAll(da)");
        }

        // 执行可流式传输的命令
        // execCount: 记录次数
        // DEFAULT_POLL_INTERVAL_MS: 轮询间隔 100ms
        // timeoutSeconds * 1000: 超时时间（毫秒）
        // "TimeTunnel recording completed successfully": 完成成功消息
        return executeStreamable(toolContext, cmd.toString(), execCount, DEFAULT_POLL_INTERVAL_MS, timeoutSeconds * 1000,
                "TimeTunnel recording completed successfully");
    }

    /**
     * 验证参数是否符合各操作的要求
     *
     * @param action 操作类型
     * @param classPattern 类名模式
     * @param methodPattern 方法名模式
     * @param index 记录索引
     * @param searchExpression 搜索表达式
     *
     * @throws IllegalArgumentException 当参数不符合要求时抛出异常
     */
    private void validateParameters(String action, String classPattern, String methodPattern,
                                   Integer index, String searchExpression) {
        switch (action) {
            case "record":
                // record 操作需要类名和方法名
                if (classPattern == null || classPattern.trim().isEmpty()) {
                    throw new IllegalArgumentException("classPattern is required for record operation");
                }
                if (methodPattern == null || methodPattern.trim().isEmpty()) {
                    throw new IllegalArgumentException("methodPattern is required for record operation");
                }
                break;
            case "info":
            case "replay":
                // info 和 replay 操作需要索引
                if (index == null) {
                    throw new IllegalArgumentException(action + " operation requires index parameter");
                }
                break;
            case "search":
                // search 操作需要搜索表达式
                if (searchExpression == null || searchExpression.trim().isEmpty()) {
                    throw new IllegalArgumentException("search operation requires searchExpression parameter");
                }
                break;
            case "delete":
                // delete 操作需要索引
                if (index == null) {
                    throw new IllegalArgumentException("delete operation requires index parameter");
                }
                break;
            case "list":
            case "deleteall":
                // list 和 deleteall 操作不需要额外参数
                break;
            default:
                // 不支持的操作类型
                throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    /**
     * 标准化操作类型
     * 将各种简写形式转换为标准名称
     *
     * 支持的别名：
     * - record: r, -t, t
     * - list: l, -l
     * - info: i, -i
     * - search: s, -s
     * - replay: p, -p
     * - delete: d, -d
     * - deleteall: da, --delete-all
     *
     * @param action 原始操作类型
     * @return 标准化后的操作类型
     */
    private String normalizeAction(String action) {
        // 如果未指定操作类型，默认为 record
        if (action == null || action.trim().isEmpty()) {
            return "record";
        }

        // 转换为小写并去除首尾空格
        String normalizedAction = action.trim().toLowerCase();

        // 根据不同的别名返回标准操作类型
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
                // 未知的操作类型，原样返回
                return normalizedAction;
        }
    }

    /**
     * 构建记录命令
     *
     * 对应 Arthas 命令：tt -t --timeout X -n N -m M [-E] classPattern methodPattern [condition]
     *
     * @param cmd 命令构建器
     * @param classPattern 类名模式
     * @param methodPattern 方法名模式
     * @param condition 条件表达式
     * @param execCount 记录次数
     * @param maxMatch 最大匹配数量
     * @param regex 是否使用正则表达式
     * @param timeoutSeconds 超时时间（秒）
     * @return 构建完成的命令
     */
    private StringBuilder buildRecordCommand(StringBuilder cmd, String classPattern, String methodPattern,
                                             String condition, int execCount, int maxMatch, Boolean regex, int timeoutSeconds) {

        // 添加 -t 标志，表示记录操作
        cmd.append(" -t");

        // 添加超时时间参数
        cmd.append(" --timeout ").append(timeoutSeconds);

        // 添加记录次数参数
        cmd.append(" -n ").append(execCount);

        // 添加最大匹配数量参数
        cmd.append(" -m ").append(maxMatch);

        // 如果启用正则表达式，添加 -E 标志
        if (Boolean.TRUE.equals(regex)) {
            cmd.append(" -E");
        }

        // 添加类名模式（用引号包裹）
        cmd.append(" '").append(classPattern.trim()).append("'");

        // 添加方法名模式（用引号包裹）
        cmd.append(" '").append(methodPattern.trim()).append("'");

        // 如果指定了条件表达式，添加到命令中（用引号包裹）
        if (condition != null && !condition.trim().isEmpty()) {
            cmd.append(" '").append(condition.trim()).append("'");
        }

        return cmd;
    }

    /**
     * 构建列表命令
     *
     * 对应 Arthas 命令：tt -l [searchExpression]
     *
     * @param cmd 命令构建器
     * @param searchExpression 搜索表达式（可选）
     * @return 构建完成的命令
     */
    private StringBuilder buildListCommand(StringBuilder cmd, String searchExpression) {
        // 添加 -l 标志，表示列表操作
        cmd.append(" -l");

        // 如果指定了搜索表达式，添加到命令中（用引号包裹）
        if (searchExpression != null && !searchExpression.trim().isEmpty()) {
            cmd.append(" '").append(searchExpression.trim()).append("'");
        }

        return cmd;
    }

}

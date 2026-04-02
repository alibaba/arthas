package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * Profiler性能分析工具类
 *
 * 该类封装了Async Profiler（async-profiler）的能力，对应Arthas的profiler命令。
 * Async Profiler是一个低开销的Java性能分析工具，可以采样CPU/内存分配/锁等事件，
 * 并生成火焰图（FlameGraph）、JFR等多种格式的输出。
 *
 * 主要功能：
 * - CPU性能采样：分析方法的CPU耗时
 * - 内存分配采样：分析对象的内存分配情况
 * - 锁竞争采样：分析锁的竞争情况
 * - Wall clock采样：分析实际运行时间
 * - 多种输出格式：火焰图、调用树、JFR、collapsed等
 *
 * 使用场景：
 * - 定位性能瓶颈
 * - 分析热点方法
 * - 优化内存分配
 * - 排查锁竞争问题
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.command.monitor200.ProfilerCommand
 */
public class ProfilerTool extends AbstractArthasTool {

    /**
     * 支持的profiler动作列表
     *
     * 各动作说明：
     * - start: 开始采样
     * - resume: 恢复之前暂停的采样
     * - stop: 停止采样并输出结果
     * - dump: 输出当前采样结果（不停止采样）
     * - check: 检查profiler是否可用
     * - status: 查看profiler当前状态
     * - meminfo: 查看内存信息
     * - list: 列出支持的事件类型
     * - version: 查看profiler版本
     * - load: 加载外部profiler库
     * - execute: 直接执行async-profiler agent兼容参数
     * - dumpCollapsed: 输出collapsed格式
     * - dumpFlat: 输出flat格式
     * - dumpTraces: 输出traces格式
     * - getSamples: 获取采样数据
     * - actions: 列出支持的动作
     */
    private static final String[] SUPPORTED_ACTIONS = new String[] {
            "start",
            "resume",
            "stop",
            "dump",
            "check",
            "status",
            "meminfo",
            "list",
            "version",
            "load",
            "execute",
            "dumpCollapsed",
            "dumpFlat",
            "dumpTraces",
            "getSamples",
            "actions"
    };

    /**
     * 执行profiler性能分析
     *
     * 该方法是Profiler工具的核心方法，用于构建并执行profiler命令。
     * 根据用户传入的动作和参数，构建完整的命令字符串并执行。
     *
     * 执行流程：
     * 1. 规范化动作名称，确保大小写不敏感
     * 2. 验证execute动作必须有actionArg参数
     * 3. 构建命令基础部分："profiler <action>"
     * 4. 根据actionArg添加额外参数
     * 5. 添加各种profiler选项（事件、间隔、格式等）
     * 6. 添加过滤选项（include/exclude）
     * 7. 添加输出选项（文件格式、标题等）
     * 8. 执行命令并返回结果
     *
     * @param action 动作名称（必填），如start/stop/dump等
     * @param actionArg 动作参数（可选），当action=execute时必填
     * @param event 采样事件，如cpu/alloc/lock/wall
     * @param interval 采样间隔（纳秒）
     * @param jstackdepth 最大Java栈深度
     * @param file 输出文件路径
     * @param format 输出格式
     * @param alloc alloc事件的采样间隔字节数
     * @param live 是否仅统计存活对象
     * @param lock lock事件的阈值
     * @param jfrsync 是否同步启动JFR
     * @param wall wall clock采样间隔（毫秒）
     * @param threads 是否按线程区分采样
     * @param sched 是否按调度策略分组线程
     * @param cstack C栈采样方式
     * @param simple 是否使用简单类名
     * @param sig 是否打印方法签名
     * @param ann 是否注解Java方法
     * @param lib 是否前置库名
     * @param allUser 是否仅包含用户态事件
     * @param norm 是否规范化方法名
     * @param include 包含的栈帧模式
     * @param exclude 排除的栈帧模式
     * @param begin 自动开始的native函数
     * @param end 自动停止的native函数
     * @param ttsp time-to-safepoint采样别名
     * @param title FlameGraph标题
     * @param minwidth FlameGraph最小帧宽百分比
     * @param reverse 是否生成反向FlameGraph
     * @param total 是否统计总量而非样本数
     * @param chunksize JFR chunk大小
     * @param chunktime JFR chunk时间
     * @param loop 循环采样参数
     * @param timeout 自动停止时间
     * @param duration 持续采样秒数
     * @param features 启用的特性集合
     * @param signal 采样信号
     * @param clock 时间戳时钟源
     * @param toolContext 工具执行上下文
     * @return profiler执行的输出结果
     */
    @Tool(
            name = "profiler",
            description = "Async Profiler 诊断工具: 对应 Arthas 的 profiler 命令，用于采样 CPU/alloc/lock 等事件并输出 flamegraph/jfr 等格式。\n"
                    + "常用示例:\n"
                    + "- start: 开始采样，如 action=start, event=cpu\n"
                    + "- stop: 停止并输出文件，如 action=stop, format=flamegraph, file=/tmp/result.html\n"
                    + "- status/list/actions: 查看状态/支持事件/支持动作\n"
                    + "- execute: 直接传递 async-profiler agent 兼容参数，如 action=execute, actionArg=\"stop,file=/tmp/result.html\""
    )
    public String profiler(
            @ToolParam(description = "动作（必填），可选值: start/resume/stop/dump/check/status/meminfo/list/version/load/execute/dumpCollapsed/dumpFlat/dumpTraces/getSamples/actions")
            String action,

            @ToolParam(description = "动作参数（可选）。当 action=execute 时必填，示例: \"stop,file=/tmp/result.html\"", required = false)
            String actionArg,

            @ToolParam(description = "采样事件 (--event)，如 cpu/alloc/lock/wall，默认 cpu", required = false)
            String event,

            @ToolParam(description = "采样间隔 ns (--interval)，默认 10000000(10ms)", required = false)
            Long interval,

            @ToolParam(description = "最大 Java 栈深 (--jstackdepth)，默认 2048", required = false)
            Integer jstackdepth,

            @ToolParam(description = "输出文件路径 (--file)。如果以 .html/.jfr 结尾可推断 format；也可包含 %t 等占位符（如 /tmp/result-%t.html）", required = false)
            String file,

            @ToolParam(description = "输出格式 (--format)，支持 flat[=N]|traces[=N]|collapsed|flamegraph|tree|jfr|md[=N]（兼容传入 html）", required = false)
            String format,

            @ToolParam(description = "alloc 事件采样间隔字节数 (--alloc)，如 1m/512k/1000 等", required = false)
            String alloc,

            @ToolParam(description = "仅对存活对象做 alloc 统计 (--live)", required = false)
            Boolean live,

            @ToolParam(description = "lock 事件阈值 ns (--lock)，如 10ms/10000000 等", required = false)
            String lock,

            @ToolParam(description = "与 profiler 一起启动 JFR (--jfrsync)，可为预置 profile 名称、.jfc 路径或 + 事件列表", required = false)
            String jfrsync,

            @ToolParam(description = "wall clock 采样间隔 ms (--wall)，推荐 200", required = false)
            Long wall,

            @ToolParam(description = "按线程区分采样 (--threads)", required = false)
            Boolean threads,

            @ToolParam(description = "按调度策略分组线程 (--sched)", required = false)
            Boolean sched,

            @ToolParam(description = "C 栈采样方式 (--cstack)，可选 fp|dwarf|lbr|no", required = false)
            String cstack,

            @ToolParam(description = "使用简单类名 (-s)", required = false)
            Boolean simple,

            @ToolParam(description = "打印方法签名 (-g)", required = false)
            Boolean sig,

            @ToolParam(description = "注解 Java 方法 (-a)", required = false)
            Boolean ann,

            @ToolParam(description = "前置库名 (-l)", required = false)
            Boolean lib,

            @ToolParam(description = "仅包含用户态事件 (--all-user)", required = false)
            Boolean allUser,

            @ToolParam(description = "规范化方法名，移除 lambda 类的数字后缀 (--norm)", required = false)
            Boolean norm,

            @ToolParam(description = "仅包含匹配的栈帧（可重复多次），等价 --include 'java/*'。传入数组。", required = false)
            String[] include,

            @ToolParam(description = "排除匹配的栈帧（可重复多次），等价 --exclude '*Unsafe.park*'。传入数组。", required = false)
            String[] exclude,

            @ToolParam(description = "当指定 native 函数执行时自动开始采样 (--begin)", required = false)
            String begin,

            @ToolParam(description = "当指定 native 函数执行时自动停止采样 (--end)", required = false)
            String end,

            @ToolParam(description = "time-to-safepoint 采样别名开关 (--ttsp)，等价设置 begin/end 为特定函数", required = false)
            Boolean ttsp,

            @ToolParam(description = "FlameGraph 标题 (--title)", required = false)
            String title,

            @ToolParam(description = "FlameGraph 最小帧宽百分比 (--minwidth)", required = false)
            String minwidth,

            @ToolParam(description = "生成反向 FlameGraph/Call tree (--reverse)", required = false)
            Boolean reverse,

            @ToolParam(description = "统计总量而非样本数 (--total)", required = false)
            Boolean total,

            @ToolParam(description = "JFR chunk 大小 (--chunksize)，默认 100MB 或其它单位", required = false)
            String chunksize,

            @ToolParam(description = "JFR chunk 时间 (--chunktime)，默认 1h", required = false)
            String chunktime,

            @ToolParam(description = "循环采样参数 (--loop)，用于 continuous profiling，如 300s", required = false)
            String loop,

            @ToolParam(description = "自动停止时间 (--timeout)，绝对或相对时间，如 300s", required = false)
            String timeout,

            @ToolParam(description = "持续采样秒数 (--duration)。注意：到时自动 stop 在后台执行，stop 的结果不会回传到当前命令输出。", required = false)
            Long duration,

            @ToolParam(description = "启用的特性集合 (--features)", required = false)
            String features,

            @ToolParam(description = "采样信号 (--signal)", required = false)
            String signal,

            @ToolParam(description = "时间戳时钟源 (--clock)，可选 monotonic 或 tsc", required = false)
            String clock,

            ToolContext toolContext
    ) {
        // 规范化动作名称，确保与支持的动作列表匹配（大小写不敏感）
        String normalizedAction = normalizeAction(action);
        // 验证execute动作必须有actionArg参数
        if ("execute".equals(normalizedAction) && (actionArg == null || actionArg.trim().isEmpty())) {
            throw new IllegalArgumentException("actionArg is required when action=execute");
        }

        // 构建profiler命令的基础部分
        StringBuilder cmd = buildCommand("profiler");
        cmd.append(" ").append(normalizedAction);

        // 如果提供了actionArg参数，添加到命令中
        if (actionArg != null && !actionArg.trim().isEmpty()) {
            addParameter(cmd, actionArg);
        }

        // ========== 添加profiler选项 ==========

        // 采样相关选项
        addOption(cmd, "--event", event);           // 采样事件
        addOption(cmd, "--alloc", alloc);           // alloc事件采样间隔字节数
        addFlag(cmd, "--live", live);               // 仅统计存活对象
        addOption(cmd, "--lock", lock);             // lock事件阈值
        addOption(cmd, "--jfrsync", jfrsync);       // 同步启动JFR

        // 输出相关选项
        addOption(cmd, "--file", file);             // 输出文件路径
        addOption(cmd, "--format", format);         // 输出格式
        addOption(cmd, "--interval", interval);     // 采样间隔
        addOption(cmd, "--jstackdepth", jstackdepth); // 最大Java栈深度
        addOption(cmd, "--wall", wall);             // wall clock采样间隔

        // 高级选项
        addOption(cmd, "--features", features);     // 启用的特性集合
        addOption(cmd, "--signal", signal);         // 采样信号
        addOption(cmd, "--clock", clock);           // 时间戳时钟源

        // 线程和栈相关选项
        addFlag(cmd, "--threads", threads);         // 按线程区分采样
        addFlag(cmd, "--sched", sched);             // 按调度策略分组线程
        addOption(cmd, "--cstack", cstack);         // C栈采样方式

        // 显示格式选项
        addFlag(cmd, "-s", simple);                 // 使用简单类名
        addFlag(cmd, "-g", sig);                    // 打印方法签名
        addFlag(cmd, "-a", ann);                    // 注解Java方法
        addFlag(cmd, "-l", lib);                    // 前置库名
        addFlag(cmd, "--all-user", allUser);        // 仅包含用户态事件
        addFlag(cmd, "--norm", norm);               // 规范化方法名

        // 过滤选项
        addRepeatableOption(cmd, "--include", include);   // 包含的栈帧模式
        addRepeatableOption(cmd, "--exclude", exclude);   // 排除的栈帧模式

        // 触发选项
        addOption(cmd, "--begin", begin);           // 自动开始的native函数
        addOption(cmd, "--end", end);               // 自动停止的native函数
        addFlag(cmd, "--ttsp", ttsp);               // time-to-safepoint采样别名

        // 火焰图相关选项
        addOption(cmd, "--title", title);           // FlameGraph标题
        addOption(cmd, "--minwidth", minwidth);     // FlameGraph最小帧宽百分比
        addFlag(cmd, "--reverse", reverse);         // 生成反向FlameGraph
        addFlag(cmd, "--total", total);             // 统计总量而非样本数

        // JFR相关选项
        addOption(cmd, "--chunksize", chunksize);   // JFR chunk大小
        addOption(cmd, "--chunktime", chunktime);   // JFR chunk时间

        // 循环和超时选项
        addOption(cmd, "--loop", loop);             // 循环采样参数
        addOption(cmd, "--timeout", timeout);       // 自动停止时间
        addOption(cmd, "--duration", duration);     // 持续采样秒数

        // 记录执行的命令（用于调试）
        logger.info("Executing profiler command: {}", cmd);
        // 同步执行命令并返回结果
        return executeSync(toolContext, cmd.toString());
    }

    /**
     * 规范化动作名称
     *
     * 将用户输入的动作名称转换为标准格式，确保大小写不敏感匹配。
     * 如果输入的动作名称不在支持列表中，抛出异常并提示所有支持的动作。
     *
     * @param action 用户输入的动作名称
     * @return 标准化的动作名称（小写）
     * @throws IllegalArgumentException 如果action为空或不在支持列表中
     */
    private static String normalizeAction(String action) {
        // 验证动作参数不能为空
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("action is required");
        }

        // 去除首尾空格
        String input = action.trim();
        // 在支持的动作列表中查找匹配的动作（大小写不敏感）
        for (String supported : SUPPORTED_ACTIONS) {
            if (supported.equalsIgnoreCase(input)) {
                return supported;
            }
        }

        // 如果没有找到匹配的动作，构建支持的动作列表并抛出异常
        StringBuilder supportedList = new StringBuilder();
        for (int i = 0; i < SUPPORTED_ACTIONS.length; i++) {
            if (i > 0) {
                supportedList.append(", ");
            }
            supportedList.append(SUPPORTED_ACTIONS[i]);
        }
        throw new IllegalArgumentException("Unsupported action: " + input + ". Supported actions: " + supportedList);
    }

    /**
     * 添加字符串类型的选项到命令中
     *
     * 如果选项值不为空，将选项和值添加到命令字符串中。
     * 使用addParameter方法确保值被正确处理（如包含空格时加引号）。
     *
     * @param cmd 命令字符串构建器
     * @param option 选项名称，如"--event"
     * @param value 选项值，如果为null或空则不添加
     */
    private void addOption(StringBuilder cmd, String option, String value) {
        // 如果值为空，直接返回不添加
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        // 添加选项名称
        cmd.append(" ").append(option);
        // 添加选项值（会自动处理引号等）
        addParameter(cmd, value);
    }

    /**
     * 添加Long类型的选项到命令中
     *
     * 用于处理数值类型的选项，如采样间隔、栈深度等。
     *
     * @param cmd 命令字符串构建器
     * @param option 选项名称，如"--interval"
     * @param value 选项值，如果为null则不添加
     */
    private void addOption(StringBuilder cmd, String option, Long value) {
        // 如果值为null，直接返回不添加
        if (value == null) {
            return;
        }
        // 直接添加选项和值（数值不需要引号）
        cmd.append(" ").append(option).append(" ").append(value);
    }

    /**
     * 添加Integer类型的选项到命令中
     *
     * 用于处理整数类型的选项，如栈深度等。
     *
     * @param cmd 命令字符串构建器
     * @param option 选项名称，如"--jstackdepth"
     * @param value 选项值，如果为null则不添加
     */
    private void addOption(StringBuilder cmd, String option, Integer value) {
        // 如果值为null，直接返回不添加
        if (value == null) {
            return;
        }
        // 直接添加选项和值（整数不需要引号）
        cmd.append(" ").append(option).append(" ").append(value);
    }

    /**
     * 添加可重复的选项到命令中
     *
     * 某些选项可以多次出现，如--include和--exclude。
     * 该方法会遍历数组，为每个非空值添加一次选项。
     *
     * @param cmd 命令字符串构建器
     * @param option 选项名称，如"--include"
     * @param values 选项值数组，如果为null或空数组则不添加
     */
    private void addRepeatableOption(StringBuilder cmd, String option, String[] values) {
        // 如果数组为空或null，直接返回不添加
        if (values == null || values.length == 0) {
            return;
        }
        // 遍历数组，为每个非空值添加选项
        for (String value : values) {
            // 跳过空值
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            // 添加选项名称
            cmd.append(" ").append(option);
            // 添加选项值
            addParameter(cmd, value);
        }
    }
}

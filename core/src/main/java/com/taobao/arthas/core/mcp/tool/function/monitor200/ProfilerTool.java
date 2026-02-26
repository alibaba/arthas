package com.taobao.arthas.core.mcp.tool.function.monitor200;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

/**
 * profiler MCP Tool: Async Profiler（async-profiler）能力封装，对应 Arthas 的 profiler 命令。
 * <p>
 * 参考：{@link com.taobao.arthas.core.command.monitor200.ProfilerCommand}
 */
public class ProfilerTool extends AbstractArthasTool {

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
        String normalizedAction = normalizeAction(action);
        if ("execute".equals(normalizedAction) && (actionArg == null || actionArg.trim().isEmpty())) {
            throw new IllegalArgumentException("actionArg is required when action=execute");
        }

        StringBuilder cmd = buildCommand("profiler");
        cmd.append(" ").append(normalizedAction);

        if (actionArg != null && !actionArg.trim().isEmpty()) {
            addParameter(cmd, actionArg);
        }

        // profiler options
        addOption(cmd, "--event", event);
        addOption(cmd, "--alloc", alloc);
        addFlag(cmd, "--live", live);
        addOption(cmd, "--lock", lock);
        addOption(cmd, "--jfrsync", jfrsync);

        addOption(cmd, "--file", file);
        addOption(cmd, "--format", format);
        addOption(cmd, "--interval", interval);
        addOption(cmd, "--jstackdepth", jstackdepth);
        addOption(cmd, "--wall", wall);

        addOption(cmd, "--features", features);
        addOption(cmd, "--signal", signal);
        addOption(cmd, "--clock", clock);

        addFlag(cmd, "--threads", threads);
        addFlag(cmd, "--sched", sched);
        addOption(cmd, "--cstack", cstack);

        addFlag(cmd, "-s", simple);
        addFlag(cmd, "-g", sig);
        addFlag(cmd, "-a", ann);
        addFlag(cmd, "-l", lib);
        addFlag(cmd, "--all-user", allUser);
        addFlag(cmd, "--norm", norm);

        addRepeatableOption(cmd, "--include", include);
        addRepeatableOption(cmd, "--exclude", exclude);

        addOption(cmd, "--begin", begin);
        addOption(cmd, "--end", end);
        addFlag(cmd, "--ttsp", ttsp);

        addOption(cmd, "--title", title);
        addOption(cmd, "--minwidth", minwidth);
        addFlag(cmd, "--reverse", reverse);
        addFlag(cmd, "--total", total);

        addOption(cmd, "--chunksize", chunksize);
        addOption(cmd, "--chunktime", chunktime);
        addOption(cmd, "--loop", loop);
        addOption(cmd, "--timeout", timeout);
        addOption(cmd, "--duration", duration);

        logger.info("Executing profiler command: {}", cmd);
        return executeSync(toolContext, cmd.toString());
    }

    private static String normalizeAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("action is required");
        }

        String input = action.trim();
        for (String supported : SUPPORTED_ACTIONS) {
            if (supported.equalsIgnoreCase(input)) {
                return supported;
            }
        }

        StringBuilder supportedList = new StringBuilder();
        for (int i = 0; i < SUPPORTED_ACTIONS.length; i++) {
            if (i > 0) {
                supportedList.append(", ");
            }
            supportedList.append(SUPPORTED_ACTIONS[i]);
        }
        throw new IllegalArgumentException("Unsupported action: " + input + ". Supported actions: " + supportedList);
    }

    private void addOption(StringBuilder cmd, String option, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        cmd.append(" ").append(option);
        addParameter(cmd, value);
    }

    private void addOption(StringBuilder cmd, String option, Long value) {
        if (value == null) {
            return;
        }
        cmd.append(" ").append(option).append(" ").append(value);
    }

    private void addOption(StringBuilder cmd, String option, Integer value) {
        if (value == null) {
            return;
        }
        cmd.append(" ").append(option).append(" ").append(value);
    }

    private void addRepeatableOption(StringBuilder cmd, String option, String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            cmd.append(" ").append(option);
            addParameter(cmd, value);
        }
    }
}

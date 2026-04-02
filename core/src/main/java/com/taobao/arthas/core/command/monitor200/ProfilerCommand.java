package com.taobao.arthas.core.command.monitor200;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.OSUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ProfilerModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import arthas.VmTool;
import one.profiler.AsyncProfiler;
import one.profiler.Counter;

/**
 * Profiler 性能分析命令
 * 集成了 async-profiler，提供 Java 应用程序的性能分析功能
 *
 * async-profiler 参数说明文档：
 * https://github.com/async-profiler/async-profiler/blob/master/docs/ProfilerOptions.md
 *
 * @author hengyunabc 2019-10-31
 *
 */
//@formatter:off
@Name("profiler")
@Summary("Async Profiler. https://github.com/jvm-profiling-tools/async-profiler")
@Description(Constants.EXAMPLE
        + "  profiler start\n"
        + "  profiler stop\n"
        + "  profiler list                # list all supported events\n"
        + "  profiler actions             # list all supported actions\n"
        + "  profiler start --event alloc\n"
        + "  profiler start --timeout 300s"
        + "  profiler start --loop 300s -f /tmp/result-%t.html"
        + "  profiler start --duration 300"
        + "  profiler stop --format html   # output file format, support flat[=N]|traces[=N]|collapsed|flamegraph|tree|jfr\n"
        + "  profiler stop --format md     # output Markdown report (LLM friendly), support md[=N]\n"
        + "  profiler stop --file /tmp/result.html\n"
        + "  profiler stop --threads \n"
        + "  profiler stop --include 'java/*' --include 'com/demo/*' --exclude '*Unsafe.park*'\n"
        + "  profiler status\n"
        + "  profiler resume              # Start or resume profiling without resetting collected data.\n"
        + "  profiler getSamples          # Get the number of samples collected during the profiling session\n"
        + "  profiler dumpFlat            # Dump flat profile, i.e. the histogram of the hottest methods\n"
        + "  profiler dumpCollapsed       # Dump profile in 'collapsed stacktraces' format\n"
        + "  profiler dumpTraces          # Dump collected stack traces\n"
        + "  profiler execute 'stop,file=/tmp/result.html'   # Execute an agent-compatible profiling command\n"
        + Constants.WIKI + Constants.WIKI_HOME + "profiler")
//@formatter:on
public class ProfilerCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(ProfilerCommand.class);
    // 跟踪在 profiler start 时是否指定了文件
    private static String fileSpecifiedAtStart = null;

    // TODO start 时，没指定 file， 是否在  stop 时，能生成 html 或者 jfr 不？
    // 动作类型（start、stop、resume 等）
    private String action;
    // 动作参数（某些动作需要的额外参数）
    private String actionArg;

    /**
     * 要追踪的事件类型（cpu、wall、cache-misses 等）
     */
    private String event;

    /**
     * 按指定字节间隔进行内存分配分析
     * 根据 async-profiler README，alloc 可能包含非数字字符
     */
    private String alloc;

    /**
     * 仅从存活对象构建内存分配分析结果
     */
    private boolean live;

    /**
     * 分析时间超过指定阈值（纳秒）的锁竞争
     * 根据 async-profiler README，lock 可能包含非数字字符
     */
    private String lock;

    /**
     * 使用给定配置同时启动 Java Flight Recording
     */
    private String jfrsync;

    /**
     * 输出文件名
     */
    private String file;

    /**
     * 输出文件格式，默认值为 html
     */
    private String format;

    /**
     * 采样间隔（单位：纳秒，默认：10'000'000，即 10 毫秒）
     */
    private Long interval;

    /**
     * 最大 Java 栈深度（默认：2048）
     */
    private Integer jstackdepth;

    /**
     * 壁钟分析间隔
     */
    private Long wall;

    /**
     * 分别分析不同线程
     */
    private boolean threads;

    /**
     * 按调度策略分组线程
     */
    private boolean sched;

    /**
     * 如何在 Java 栈之外收集 C 栈帧
     * MODE 可以是 'fp'（帧指针）、'dwarf'、'lbr'（最后分支记录）或 'no'
     */
    private String cstack;

    /**
     * 使用简单类名而不是完全限定名
     */
    private boolean simple;

    /**
     * 打印方法签名
     */
    private boolean sig;

    /**
     * 注释 Java 方法
     */
    private boolean ann;

    /**
     * 在前面添加库名
     */
    private boolean lib;

    /**
     * 仅包含用户态事件
     */
    private boolean alluser;

    /**
     * 运行性能分析持续时长（秒）
     */
    private Long duration;

    /**
     * 仅包含包含 PATTERN 的堆栈跟踪
     */
    private List<String> includes;

    /**
     * 排除包含 PATTERN 的堆栈跟踪
     */
    private List<String> excludes;

    /**
     * 当执行指定的本地函数时自动开始分析
     */
    private String begin;

    /**
     * 当执行指定的本地函数时自动停止分析
     */
    private String end;

    /**
     * 到达安全点时间分析
     * --begin SafepointSynchronize::begin --end RuntimeService::record_safepoint_synchronized 的别名
     */
    private boolean ttsp;

    /**
     * 火焰图标题
     */
    private String title;

    /**
     * 火焰图最小帧宽度（百分比）
     */
    private String minwidth;

    /**
     * 生成栈反转的火焰图/调用树
     */
    private boolean reverse;

    /**
     * 计算总值（时间、字节等）而不是样本数
     */
    private boolean total;

    /**
     * JFR 块的近似大小（字节，默认：100 MB）
     */
    private String chunksize;

    /**
     * JFR 块的持续时间（秒，默认：1 小时）
     */
    private String chunktime;

    /**
     * 循环运行性能分析器（连续性能分析）
     */
    private String loop;

    /**
     * 在指定时间自动停止性能分析器（绝对或相对时间）
     */
    private String timeout;

    /**
     * 性能分析启用的功能特性
     */
    private String features;

    /**
     * 使用的性能分析信号
     */
    private String signal;

    /*
     * 采样时间戳的时钟源：monotonic 或 tsc
     */
    private String clock;

    /*
     * 通过移除 lambda 类的唯一数字后缀来规范化方法名
     */
    private boolean norm;

    // async-profiler 库文件路径
    private static String libPath;
    // AsyncProfiler 实例
    private static AsyncProfiler profiler = null;

    // 静态初始化块，用于确定并加载正确的 native 库
    static {
        String profilerSoPath = null;
        // 根据操作系统选择对应的库文件
        if (OSUtils.isMac()) {
            // FAT_BINARY 同时支持 x86_64/arm64
            profilerSoPath = "async-profiler/libasyncProfiler-mac.dylib";
        }
        if (OSUtils.isLinux()) {
            // 根据 CPU 架构选择对应的 Linux 库
            if (OSUtils.isX86_64()) {
                profilerSoPath = "async-profiler/libasyncProfiler-linux-x64.so";
            }  else if (OSUtils.isArm64()) {
                profilerSoPath = "async-profiler/libasyncProfiler-linux-arm64.so";
            }  else if (OSUtils.isLoongArch64()) {
                profilerSoPath = "async-profiler/libasyncProfiler-linux-loongarch64.so";
            }
        }

        // 如果找到了合适的库文件，则设置其绝对路径
        if (profilerSoPath != null) {
            // 获取当前类的代码源位置
            CodeSource codeSource = ProfilerCommand.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                try {
                    // 获取 arthas boot jar 文件路径
                    File bootJarPath = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                    // 构建 so 文件的完整路径
                    File soFile = new File(bootJarPath.getParentFile(), profilerSoPath);
                    // 如果文件存在，保存其绝对路径
                    if (soFile.exists()) {
                        libPath = soFile.getAbsolutePath();
                    }
                } catch (Throwable e) {
                    logger.error("can not find libasyncProfiler so", e);
                }
            }
        }

    }

    /**
     * 设置要执行的动作
     * @param action 动作名称
     */
    @Argument(argName = "action", index = 0, required = true)
    @Description("Action to execute")
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 设置动作参数
     * @param actionArg 动作参数
     */
    @Argument(argName = "actionArg", index = 1, required = false)
    @Description("Attribute name pattern.")
    public void setActionArg(String actionArg) {
        this.actionArg = actionArg;
    }

    /**
     * 设置采样间隔
     * @param interval 采样间隔（纳秒）
     */
    @Option(shortName = "i", longName = "interval")
    @Description("sampling interval in ns (default: 10'000'000, i.e. 10 ms)")
    @DefaultValue("10000000")
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * 设置最大 Java 栈深度
     * @param jstackdepth 最大栈深度
     */
    @Option(shortName = "j", longName = "jstackdepth")
    @Description("maximum Java stack depth (default: 2048)")
    public void setJstackdepth(int jstackdepth) {
        this.jstackdepth = jstackdepth;
    }

    /**
     * 设置输出文件
     * @param file 输出文件路径
     */
    @Option(shortName = "f", longName = "file")
    @Description("dump output to <filename>, if ends with html or jfr, content format can be infered")
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * 设置输出格式
     * @param format 输出格式（flat[=N]|traces[=N]|collapsed|flamegraph|tree|jfr|md[=N]）
     */
    @Option(shortName = "o", longName = "format")
    @Description("dump output content format(flat[=N]|traces[=N]|collapsed|flamegraph|tree|jfr|md[=N])")
    public void setFormat(String format) {
        // 为了向后兼容，将 html 转换为 flamegraph
        if ("html".equals(format)) {
            format = "flamegraph";
        }
        this.format = format;
    }

    /**
     * 判断是否为 Markdown 格式
     * @return true 如果是 Markdown 格式
     */
    private boolean isMarkdownFormat() {
        return this.format != null && this.format.toLowerCase().startsWith("md");
    }

    /**
     * 设置要追踪的事件
     * @param event 事件类型（cpu、alloc、lock、cache-misses 等）
     */
    @Option(shortName = "e", longName = "event")
    @Description("which event to trace (cpu, alloc, lock, cache-misses etc.), default value is cpu")
    @DefaultValue("cpu")
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * 设置内存分配分析间隔
     * @param alloc 分配间隔（字节）
     */
    @Option(longName = "alloc")
    @Description("allocation profiling interval in bytes")
    public void setAlloc(String alloc) {
        this.alloc = alloc;
    }

    /**
     * 设置是否仅分析存活对象
     * @param live true 表示仅分析存活对象
     */
    @Option(longName = "live", flag = true)
    @Description("build allocation profile from live objects only")
    public void setLive(boolean live) {
        this.live = live;
    }

    /**
     * 设置锁分析阈值
     * @param lock 锁分析阈值（纳秒）
     */
    @Option(longName = "lock")
    @Description("lock profiling threshold in nanoseconds")
    public void setLock(String lock) {
        this.lock = lock;
    }

    /**
     * 设置 JFR 同步配置
     * @param jfrsync JFR 配置
     */
    @Option(longName = "jfrsync")
    @Description("Start Java Flight Recording with the given config along with the profiler. "
            + "Accepts a predefined profile name, a path to a .jfc file, or a list of JFR events starting with '+'. ")
    public void setJfrsync(String jfrsync) {
        this.jfrsync = jfrsync;
    }

    /**
     * 设置壁钟分析间隔
     * @param wall 壁钟间隔（毫秒，推荐：200）
     */
    @Option(longName = "wall")
    @Description("wall clock profiling interval in milliseconds(recommended: 200)")
    public void setWall(Long wall) {
        this.wall = wall;
    }

    /**
     * 设置是否分别分析不同线程
     * @param threads true 表示分别分析不同线程
     */
    @Option(shortName = "t", longName = "threads", flag = true)
    @Description("profile different threads separately")
    public void setThreads(boolean threads) {
        this.threads = threads;
    }

    /**
     * 设置性能分析功能特性
     * @param features 功能特性
     */
    @Option(shortName = "F", longName = "features")
    @Description("Features enabled for profiling")
    public void setFeatures(String features) {
        this.features = features;
    }

    /**
     * 设置性能分析信号
     * @param signal 信号类型
     */
    @Option(longName = "signal")
    @Description("Set the profiling signal to use")
    public void setSignal(String signal) {
        this.signal = signal;
    }

    /**
     * 设置时钟源
     * @param clock 时钟源（monotonic 或 tsc）
     */
    @Option(longName = "clock")
    @Description("Clock source for sampling timestamps: monotonic or tsc")
    public void setClock(String clock) {
        this.clock = clock;
    }

    /**
     * 设置是否规范化方法名
     * @param norm true 表示规范化方法名
     */
    @Option(longName = "norm", flag = true)
    @Description("Normalize method names by removing unique numerical suffixes from lambda classes.")
    public void setNorm(boolean norm) {
        this.norm = norm;
    }

    /**
     * 设置是否按调度策略分组线程
     * @param sched true 表示按调度策略分组
     */
    @Option(longName = "sched", flag = true)
    @Description("group threads by scheduling policy")
    public void setSched(boolean sched) {
        this.sched = sched;
    }

    /**
     * 设置如何遍历 C 栈
     * @param cstack 遍历方式（fp|dwarf|lbr|no）
     */
    @Option(longName = "cstack")
    @Description("how to traverse C stack: fp|dwarf|lbr|no")
    public void setCstack(String cstack) {
        this.cstack = cstack;
    }

    /**
     * 设置是否使用简单类名
     * @param simple true 表示使用简单类名
     */
    @Option(shortName = "s", flag = true)
    @Description("use simple class names instead of FQN")
    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    /**
     * 设置是否打印方法签名
     * @param sig true 表示打印方法签名
     */
    @Option(shortName = "g", flag = true)
    @Description("print method signatures")
    public void setSig(boolean sig) {
        this.sig = sig;
    }

    /**
     * 设置是否注释 Java 方法
     * @param ann true 表示注释 Java 方法
     */
    @Option(shortName = "a", flag = true)
    @Description("annotate Java methods")
    public void setAnn(boolean ann) {
        this.ann = ann;
    }

    /**
     * 设置是否在前面添加库名
     * @param lib true 表示添加库名
     */
    @Option(shortName = "l", flag = true)
    @Description("prepend library names")
    public void setLib(boolean lib) {
        this.lib = lib;
    }

    /**
     * 设置是否仅包含用户态事件
     * @param alluser true 表示仅包含用户态事件
     */
    @Option(longName = "all-user", flag = true)
    @Description("include only user-mode events")
    public void setAlluser(boolean alluser) {
        this.alluser = alluser;
    }

    /**
     * 设置运行持续时间
     * @param duration 持续时间（秒）
     */
    @Option(shortName = "d", longName = "duration")
    @Description("run profiling for <duration> seconds")
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * 设置包含的堆栈跟踪模式
     * @param includes 包含模式列表
     */
    @Option(shortName = "I", longName = "include")
    @Description("include stack traces containing PATTERN, for example: 'java/*'")
    public void setInclude(List<String> includes) {
        this.includes = includes;
    }

    /**
     * 设置排除的堆栈跟踪模式
     * @param excludes 排除模式列表
     */
    @Option(shortName = "X", longName = "exclude")
    @Description("exclude stack traces containing PATTERN, for example: '*Unsafe.park*'")
    public void setExclude(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * 设置自动开始的本地函数
     * @param begin 本地函数名
     */
    @Option(longName = "begin")
    @Description("automatically start profiling when the specified native function is executed")
    public void setBegin(String begin) {
        this.begin = begin;
    }

    /**
     * 设置自动停止的本地函数
     * @param end 本地函数名
     */
    @Option(longName = "end")
    @Description("automatically stop profiling when the specified native function is executed")
    public void setEnd(String end) {
        this.end = end;
    }

    /**
     * 设置是否启用到达安全点时间分析
     * @param ttsp true 表示启用 ttsp 分析
     */
    @Option(longName = "ttsp", flag = true)
    @Description("time-to-safepoint profiling. "
        + "An alias for --begin SafepointSynchronize::begin --end RuntimeService::record_safepoint_synchronized")
    public void setTtsp(boolean ttsp) {
        this.ttsp = ttsp;
    }

    /**
     * 设置火焰图标题
     * @param title 火焰图标题
     */
    @Option(longName = "title")
    @Description("FlameGraph title")
    public void setTitle(String title) {
        // 转义 HTML 特殊字符
        // 转义逗号以避免与 JVM TI 冲突
        title = title.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace(",", "&#44;");
        this.title = title;
    }

    /**
     * 设置火焰图最小帧宽度
     * @param minwidth 最小帧宽度（百分比）
     */
    @Option(longName = "minwidth")
    @Description("FlameGraph minimum frame width in percent")
    public void setMinwidth(String minwidth) {
        this.minwidth = minwidth;
    }

    /**
     * 设置是否生成栈反转的火焰图
     * @param reverse true 表示生成栈反转的火焰图
     */
    @Option(longName = "reverse", flag = true)
    @Description("generate stack-reversed FlameGraph / Call tree")
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * 设置是否计算总值而不是样本数
     * @param total true 表示计算总值
     */
    @Option(longName = "total", flag = true)
    @Description("count the total value (time, bytes, etc.) instead of samples")
    public void setTotal(boolean total) {
        this.total = total;
    }

    /**
     * 设置 JFR 块的大小限制
     * @param chunksize 块大小（字节或其他单位）
     */
    @Option(longName = "chunksize")
    @Description("approximate size limits for a single JFR chunk in bytes (default: 100 MB) or other units")
    public void setChunksize(String chunksize) {
        this.chunksize = chunksize;
    }

    /**
     * 设置 JFR 块的时间限制
     * @param chunktime 块时间（秒或其他单位）
     */
    @Option(longName = "chunktime")
    @Description("approximate time limits for a single JFR chunk in second (default: 1 hour) or other units")
    public void setChunktime(String chunktime) {
        this.chunktime = chunktime;
    }

    /**
     * 设置循环运行模式
     * @param loop 循环间隔
     */
    @Option(longName = "loop")
    @Description("run profiler in a loop (continuous profiling)")
    public void setLoop(String loop) {
        this.loop = loop;
    }

    /**
     * 设置自动停止时间
     * @param timeout 停止时间（绝对或相对）
     */
    @Option(longName = "timeout")
    @Description("automatically stop profiler at TIME (absolute or relative)")
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }


    /**
     * 获取 AsyncProfiler 实例
     * 使用单例模式，确保只创建一个实例
     *
     * @return AsyncProfiler 实例
     */
    private AsyncProfiler profilerInstance() {
        // 如果已经创建过实例，直接返回
        if (profiler != null) {
            return profiler;
        }

        // 尝试从指定路径加载
        if (ProfilerAction.load.toString().equals(action)) {
            profiler = AsyncProfiler.getInstance(this.actionArg);
        }

        // 如果找到了库文件路径
        if (libPath != null) {
            // 从 arthas 目录加载
            // 尝试把 lib 文件复制到临时文件里，避免多次 attach 时出现 Native Library already loaded in another classloader
            FileOutputStream tmpLibOutputStream = null;
            FileInputStream libInputStream = null;
            try {
                // 创建临时文件
                File tmpLibFile = File.createTempFile(VmTool.JNI_LIBRARY_NAME, null);
                tmpLibOutputStream = new FileOutputStream(tmpLibFile);
                libInputStream = new FileInputStream(libPath);

                // 复制库文件到临时文件
                IOUtils.copy(libInputStream, tmpLibOutputStream);
                libPath = tmpLibFile.getAbsolutePath();
                logger.debug("copy {} to {}", libPath, tmpLibFile);
            } catch (Throwable e) {
                logger.error("try to copy lib error! libPath: {}", libPath, e);
            } finally {
                // 关闭流
                IOUtils.close(libInputStream);
                IOUtils.close(tmpLibOutputStream);
            }
            // 使用临时文件路径创建实例
            profiler = AsyncProfiler.getInstance(libPath);
        } else {
            // 如果找不到库文件，抛出异常
            if (OSUtils.isLinux() || OSUtils.isMac()) {
                throw new IllegalStateException("Can not find libasyncProfiler so, please check the arthas directory.");
            } else {
                throw new IllegalStateException("Current OS do not support AsyncProfiler, Only support Linux/Mac.");
            }
        }

        return profiler;
    }

    /**
     * 性能分析器动作枚举
     * 定义了所有支持的性能分析动作
     *
     * async-profiler 动作参考：
     * https://github.com/async-profiler/async-profiler/blob/v3.0/src/arguments.cpp#L131
     */
    public enum ProfilerAction {
        // start - 开始性能分析
        // resume - 恢复性能分析（不重置已收集的数据）
        // stop - 停止性能分析
        // dump - 导出性能分析结果
        // check - 检查性能分析状态
        // status - 获取性能分析状态信息
        // meminfo - 获取内存信息
        // list - 列出支持的事件
        start, resume, stop, dump, check, status, meminfo, list,
        version, // 获取版本信息

        load,    // 加载指定路径的 profiler 库
        execute, // 执行原生命令
        dumpCollapsed, // 导出折叠格式的堆栈
        dumpFlat,      // 导出平铺格式的方法统计
        dumpTraces,    // 导出堆栈跟踪
        getSamples,    // 获取样本数
        actions        // 列出所有支持的动作
    }

    /**
     * 构建执行参数字符串
     * 将所有配置参数转换为 async-profiler 可识别的参数格式
     *
     * @param action 性能分析动作
     * @return 参数字符串
     */
    private String executeArgs(ProfilerAction action) {
        StringBuilder sb = new StringBuilder();
        final char COMMA = ',';

        // 添加动作名称
        // start - 开始性能分析
        // resume - 恢复性能分析（不重置已收集的数据）
        // stop - 停止性能分析
        sb.append(action).append(COMMA);

        // 添加事件类型
        if (this.event != null) {
            sb.append("event=").append(this.event).append(COMMA);
        }
        // 添加内存分配分析间隔
        if (this.alloc!= null) {
            sb.append("alloc=").append(this.alloc).append(COMMA);
        }
        // 添加仅分析存活对象标志
        if (this.live) {
            sb.append("live").append(COMMA);
        }
        // 添加锁分析阈值
        if (this.lock!= null) {
            sb.append("lock=").append(this.lock).append(COMMA);
        }
        // 添加 JFR 同步配置
        if (this.jfrsync != null) {
            this.format = "jfr";
            sb.append("jfrsync=").append(this.jfrsync).append(COMMA);
        }
        // 判断是否为 Markdown 格式
        boolean markdown = isMarkdownFormat();
        // md 是 Arthas 侧的后处理格式，不应传递给 async-profiler（避免识别失败/输出到文件导致数据丢失等问题）
        boolean passFile = this.file != null;
        if (markdown && (action == ProfilerAction.start || action == ProfilerAction.resume || action == ProfilerAction.check)) {
            passFile = false;
        }
        // 添加输出文件参数
        if (passFile) {
            sb.append("file=").append(this.file).append(COMMA);
        }
        // 添加输出格式参数
        if (this.format != null && !markdown) {
            sb.append(this.format).append(COMMA);
        }
        // 添加采样间隔
        if (this.interval != null) {
            sb.append("interval=").append(this.interval).append(COMMA);
        }
        // 添加功能特性
        if (this.features != null) {
            sb.append("features=").append(this.features).append(COMMA);
        }
        // 添加信号类型
        if (this.signal != null) {
            sb.append("signal=").append(this.signal).append(COMMA);
        }
        // 添加时钟源
        if (this.clock != null) {
            sb.append("clock=").append(this.clock).append(COMMA);
        }
        // 添加栈深度
        if (this.jstackdepth != null) {
            sb.append("jstackdepth=").append(this.jstackdepth).append(COMMA);
        }
        // 添加分别分析线程标志
        if (this.threads) {
            sb.append("threads").append(COMMA);
        }
        // 添加按调度策略分组标志
        if (this.sched) {
            sb.append("sched").append(COMMA);
        }
        // 添加 C 栈遍历方式
        if (this.cstack != null) {
            sb.append("cstack=").append(this.cstack).append(COMMA);
        }
        // 添加使用简单类名标志
        if (this.simple) {
            sb.append("simple").append(COMMA);
        }
        // 添加打印方法签名标志
        if (this.sig) {
            sb.append("sig").append(COMMA);
        }
        // 添加注释 Java 方法标志
        if (this.ann) {
            sb.append("ann").append(COMMA);
        }
        // 添加添加库名标志
        if (this.lib) {
            sb.append("lib").append(COMMA);
        }
        // 添加仅用户态事件标志
        if (this.alluser) {
            sb.append("alluser").append(COMMA);
        }
        // 添加规范化方法名标志
        if (this.norm) {
            sb.append("norm").append(COMMA);
        }
        // 添加包含模式
        if (this.includes != null) {
            for (String include : includes) {
                sb.append("include=").append(include).append(COMMA);
            }
        }
        // 添加排除模式
        if (this.excludes != null) {
            for (String exclude : excludes) {
                sb.append("exclude=").append(exclude).append(COMMA);
            }
        }
        // 处理 ttsp 参数
        if (this.ttsp) {
            this.begin = "SafepointSynchronize::begin";
            this.end = "RuntimeService::record_safepoint_synchronized";
        }
        // 添加开始函数
        if (this.begin != null) {
            sb.append("begin=").append(this.begin).append(COMMA);
        }
        // 添加结束函数
        if (this.end != null) {
            sb.append("end=").append(this.end).append(COMMA);
        }
        // 添加壁钟间隔
        if (this.wall != null) {
            sb.append("wall=").append(this.wall).append(COMMA);
        }
        // 添加火焰图标题
        if (this.title != null) {
            sb.append("title=").append(this.title).append(COMMA);
        }
        // 添加最小帧宽度
        if (this.minwidth != null) {
            sb.append("minwidth=").append(this.minwidth).append(COMMA);
        }
        // 添加栈反转标志
        if (this.reverse) {
            sb.append("reverse").append(COMMA);
        }
        // 添加计算总值标志
        if (this.total) {
            sb.append("total").append(COMMA);
        }
        // 添加 JFR 块大小
        if (this.chunksize != null) {
            sb.append("chunksize=").append(this.chunksize).append(COMMA);
        }
        // 添加 JFR 块时间
        if (this.chunktime!= null) {
            sb.append("chunktime=").append(this.chunktime).append(COMMA);
        }
        // 添加循环模式
        if (this.loop != null) {
            sb.append("loop=").append(this.loop).append(COMMA);
        }
        // 添加超时时间
        if (this.timeout != null) {
            sb.append("timeout=").append(this.timeout).append(COMMA);
        }

        return sb.toString();
    }

    /**
     * 执行 async-profiler 命令
     *
     * @param asyncProfiler AsyncProfiler 实例
     * @param arg 命令参数
     * @return 执行结果
     * @throws IllegalArgumentException 参数非法
     * @throws IOException IO 异常
     */
    private static String execute(AsyncProfiler asyncProfiler, String arg)
            throws IllegalArgumentException, IOException {
        // 记录执行的参数
        logger.info("profiler execute args: {}", arg);
        // 执行命令
        String result = asyncProfiler.execute(arg);
        // 确保结果以换行符结尾
        if (!result.endsWith("\n")) {
            result += "\n";
        }
        return result;
    }

    /**
     * 处理 profiler 命令
     * 根据不同的动作类型执行相应的操作
     *
     * @param process 命令处理进程
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            // 将动作字符串转换为枚举
            ProfilerAction profilerAction = ProfilerAction.valueOf(action);

            // 处理 actions 动作：列出所有支持的动作
            if (ProfilerAction.actions.equals(profilerAction)) {
                process.appendResult(new ProfilerModel(actions()));
                process.end();
                return;
            }

            // 获取 AsyncProfiler 实例
            final AsyncProfiler asyncProfiler = this.profilerInstance();

            // 处理 execute 动作：执行原生命令
            if (ProfilerAction.execute.equals(profilerAction)) {
                if (actionArg == null) {
                    process.end(1, "actionArg can not be empty.");
                    return;
                }
                // 执行命令并输出结果
                String result = execute(asyncProfiler, this.actionArg);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.start.equals(profilerAction)) {
                // 处理 start 动作：开始性能分析
                // 跟踪在 start 时是否指定了文件参数
                boolean autoGeneratedFile = false;
                if (this.file != null) {
                    // 保存用户指定的文件路径
                    fileSpecifiedAtStart = this.file;
                    logger.debug("File specified during profiler start: {}", fileSpecifiedAtStart);
                } else if (this.timeout != null) {
                    // 如果指定了超时但未指定文件，自动生成文件名
                    try {
                        this.file = outputFile();
                        logger.debug("Auto-generated file for timeout: {}", this.file);
                        fileSpecifiedAtStart = this.file;
                        autoGeneratedFile = true;
                    } catch (IOException e) {
                        logger.warn("Failed to auto-generate file for timeout", e);
                    }
                }

                if (this.duration == null) {
                    // 没有指定持续时间，正常启动
                    String executeArgs = executeArgs(ProfilerAction.start);
                    String result = execute(asyncProfiler, executeArgs);
                    ProfilerModel profilerModel = createProfilerModel(result);

                    // 添加自动生成文件的信息
                    if (autoGeneratedFile && this.file != null) {
                        profilerModel.setOutputFile(this.file);
                        profilerModel.setExecuteResult(profilerModel.getExecuteResult()
                                + "\nAuto-generated output file will be: " + this.file + "\n");
                    }

                    process.appendResult(profilerModel);
                } else {
                    // 指定了持续时间，延时执行 stop
                    final String outputFile = outputFile();
                    String executeArgs = executeArgs(ProfilerAction.start);
                    String result = execute(asyncProfiler, executeArgs);
                    ProfilerModel profilerModel = createProfilerModel(result);
                    profilerModel.setOutputFile(outputFile);
                    profilerModel.setDuration(duration);

                    // 延时执行 stop
                    ArthasBootstrap.getInstance().getScheduledExecutorService().schedule(new Runnable() {
                        @Override
                        public void run() {
                            // 在异步线程执行，profiler 命令已经结束，不能输出到客户端
                            try {
                                logger.info("stopping profiler ...");
                                ProfilerModel model = processStop(asyncProfiler, ProfilerAction.stop);
                                logger.info("profiler output file: " + model.getOutputFile());
                                logger.info("stop profiler successfully.");
                            } catch (Throwable e) {
                                logger.error("stop profiler failure", e);
                            }
                        }
                    }, this.duration, TimeUnit.SECONDS);
                    process.appendResult(profilerModel);
                }

            } else if (ProfilerAction.stop.equals(profilerAction)) {
                // 处理 stop 动作：停止性能分析
                ProfilerModel profilerModel = processStop(asyncProfiler, profilerAction);
                process.appendResult(profilerModel);
            } else if (ProfilerAction.dump.equals(profilerAction)) {
                // 处理 dump 动作：导出性能分析结果
                ProfilerModel profilerModel = processStop(asyncProfiler, profilerAction);
                process.appendResult(profilerModel);
            } else if (ProfilerAction.resume.equals(profilerAction)) {
                // 处理 resume 动作：恢复性能分析
                String executeArgs = executeArgs(ProfilerAction.resume);
                String result = execute(asyncProfiler, executeArgs);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.check.equals(profilerAction)) {
                // 处理 check 动作：检查性能分析状态
                String executeArgs = executeArgs(ProfilerAction.check);
                String result = execute(asyncProfiler, executeArgs);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.version.equals(profilerAction)) {
                // 处理 version 动作：获取版本信息
                String result = asyncProfiler.execute("version=full");
                appendExecuteResult(process, result);
            } else if (ProfilerAction.status.equals(profilerAction)
                    || ProfilerAction.meminfo.equals(profilerAction)
                    || ProfilerAction.list.equals(profilerAction)) {
                // 处理 status、meminfo、list 动作
                String result = asyncProfiler.execute(profilerAction.toString());
                appendExecuteResult(process, result);
            } else if (ProfilerAction.dumpCollapsed.equals(profilerAction)) {
                // 处理 dumpCollapsed 动作：导出折叠格式
                if (actionArg == null) {
                    actionArg = "TOTAL";
                }
                actionArg = actionArg.toUpperCase();
                if ("TOTAL".equals(actionArg) || "SAMPLES".equals(actionArg)) {
                    String result = asyncProfiler.dumpCollapsed(Counter.valueOf(actionArg));
                    appendExecuteResult(process, result);
                } else {
                    process.end(1, "ERROR: dumpCollapsed argumment should be TOTAL or SAMPLES. ");
                    return;
                }
            } else if (ProfilerAction.dumpFlat.equals(profilerAction)) {
                // 处理 dumpFlat 动作：导出平铺格式
                int maxMethods = 0;
                if (actionArg != null) {
                    maxMethods = Integer.valueOf(actionArg);
                }
                String result = asyncProfiler.dumpFlat(maxMethods);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.dumpTraces.equals(profilerAction)) {
                // 处理 dumpTraces 动作：导出堆栈跟踪
                int maxTraces = 0;
                if (actionArg != null) {
                    maxTraces = Integer.valueOf(actionArg);
                }
                String result = asyncProfiler.dumpTraces(maxTraces);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.getSamples.equals(profilerAction)) {
                // 处理 getSamples 动作：获取样本数
                String result = "" + asyncProfiler.getSamples() + "\n";
                appendExecuteResult(process, result);
            }
            // 结束命令处理
            process.end();
        } catch (Throwable e) {
            // 发生异常时记录错误并结束
            logger.error("AsyncProfiler error", e);
            process.end(1, "AsyncProfiler error: "+e.getMessage());
        }
    }

    /**
     * 处理 stop/dump 动作
     *
     * @param asyncProfiler AsyncProfiler 实例
     * @param profilerAction 性能分析动作
     * @return 性能分析结果模型
     * @throws IOException IO 异常
     */
    private ProfilerModel processStop(AsyncProfiler asyncProfiler, ProfilerAction profilerAction) throws IOException {
        // profiler stop --file xxx.md：自动推断为 Markdown 输出
        if (this.format == null && this.file != null && this.file.toLowerCase().endsWith(".md")) {
            this.format = "md";
        }

        // 如果是 Markdown 格式，使用特殊的处理逻辑
        if (isMarkdownFormat() && (profilerAction == ProfilerAction.stop || profilerAction == ProfilerAction.dump)) {
            return processStopMarkdown(asyncProfiler, profilerAction);
        }

        String outputFile = null;

        // 如果在 stop 时且 start 阶段指定过文件，使用该文件
        if (profilerAction == ProfilerAction.stop && fileSpecifiedAtStart != null) {
            outputFile = fileSpecifiedAtStart;
            // 停止后重置跟踪变量
            logger.debug("Using file specified during start: {}", fileSpecifiedAtStart);
            fileSpecifiedAtStart = null;
        } else {
            // 否则生成或使用指定的输出文件
            outputFile = outputFile();
        }

        // 构建执行参数并执行
        String executeArgs = executeArgs(profilerAction);
        String result = execute(asyncProfiler, executeArgs);

        // 创建结果模型
        ProfilerModel profilerModel = createProfilerModel(result);
        if (outputFile != null) {
            profilerModel.setOutputFile(outputFile);
        }
        return profilerModel;
    }

    /**
     * 处理 Markdown 格式的 stop/dump 动作
     *
     * @param asyncProfiler AsyncProfiler 实例
     * @param profilerAction 性能分析动作
     * @return 性能分析结果模型
     * @throws IOException IO 异常
     */
    private ProfilerModel processStopMarkdown(AsyncProfiler asyncProfiler, ProfilerAction profilerAction) throws IOException {
        // Markdown 输出：先让 async-profiler 输出 collapsed 文本，再在 Arthas 侧做结构化汇总
        String userFormat = this.format;
        String userFile = this.file;
        int topN = mdTopN(userFormat);

        // stop 时如果 start 阶段指定过 file，需要清理掉，避免影响后续 stop 行为
        if (profilerAction == ProfilerAction.stop) {
            fileSpecifiedAtStart = null;
        }

        // 创建临时文件用于存储 collapsed 格式数据
        File collapsedFile = File.createTempFile("arthas-profiler-collapsed", ".txt");
        String collapsed;
        try {
            // 为避免 async-profiler 由于历史 file 配置导致返回 OK 而非 collapsed 文本，这里强制输出到临时文件后再读取
            this.file = collapsedFile.getAbsolutePath();
            this.format = "collapsed";

            String executeArgs = executeArgs(profilerAction);
            execute(asyncProfiler, executeArgs);

            // 读取 collapsed 格式数据
            collapsed = FileUtils.readFileToString(collapsedFile, StandardCharsets.UTF_8);
        } finally {
            // best-effort cleanup：尽力清理临时文件
            try {
                collapsedFile.delete();
            } catch (Throwable ignore) {
                // ignore
            }
            // 恢复原始配置
            this.format = userFormat;
            this.file = userFile;
        }

        // 将 collapsed 格式转换为 Markdown
        String markdown = ProfilerMarkdown.toMarkdown(new ProfilerMarkdown.Options()
                .action(profilerAction.name())
                .event(this.event)
                .threads(this.threads)
                .topN(topN)
                .collapsed(collapsed));

        // 如果指定了输出文件，写入文件
        String outputFile = null;
        if (userFile != null && !userFile.trim().isEmpty()) {
            outputFile = userFile;
            FileUtils.writeByteArrayToFile(new File(outputFile), markdown.getBytes(StandardCharsets.UTF_8));
        }

        // 创建结果模型
        ProfilerModel profilerModel = createProfilerModel(markdown);
        profilerModel.setFormat(userFormat);
        profilerModel.setOutputFile(outputFile);
        return profilerModel;
    }

    /**
     * 从 Markdown 格式字符串中提取 topN 参数
     *
     * @param format 格式字符串（如 "md", "md=10"）
     * @return topN 值，默认为 10
     */
    private int mdTopN(String format) {
        final int defaultTopN = 10;
        if (format == null) {
            return defaultTopN;
        }
        String f = format.trim().toLowerCase();
        if (!f.startsWith("md")) {
            return defaultTopN;
        }
        // 查找等号位置
        int idx = f.indexOf('=');
        if (idx < 0 || idx == f.length() - 1) {
            return defaultTopN;
        }
        try {
            // 解析 topN 值
            int n = Integer.parseInt(f.substring(idx + 1).trim());
            return n > 0 ? n : defaultTopN;
        } catch (Throwable e) {
            return defaultTopN;
        }
    }

    /**
     * 获取输出文件路径
     * 如果未指定文件，则自动生成文件名
     *
     * @return 输出文件路径
     * @throws IOException IO 异常
     */
    private String outputFile() throws IOException {
        if (this.file == null) {
            // 获取文件扩展名
            String fileExt = outputFileExt();
            File outputPath = ArthasBootstrap.getInstance().getOutputPath();
            if (outputPath != null) {
                // 使用配置的输出路径
                this.file = new File(outputPath,
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "." + fileExt)
                                .getAbsolutePath();
            } else {
                // 使用临时文件目录
                this.file = File.createTempFile("arthas-output", "." + fileExt).getAbsolutePath();
            }
        }
        return file;
    }

    /**
     * 获取输出文件扩展名
     * 根据输出格式确定文件扩展名
     *
     * 注意：此方法仅在 {@code this.file == null} 时调用
     * @return 文件扩展名
     */
    private String outputFileExt() {
        String fileExt = "";
        if (this.format == null) {
            // 默认使用 html 格式
            fileExt = "html";
        } else if (this.format.toLowerCase().startsWith("md")) {
            // Markdown 格式
            fileExt = "md";
        } else if (this.format.startsWith("flat") || this.format.startsWith("traces")
                || this.format.equals("collapsed")) {
            // 文本格式
            fileExt = "txt";
        } else if (this.format.equals("flamegraph") || this.format.equals("tree")) {
            // HTML 格式
            fileExt = "html";
        } else if (this.format.equals("jfr")) {
            // JFR 格式
            fileExt = "jfr";
        } else {
            // 非法的 -o 选项会让 async-profiler 使用 flat 格式
            fileExt = "txt";
        }
        return fileExt;
    }

    /**
     * 追加执行结果到进程
     *
     * @param process 命令处理进程
     * @param result 执行结果
     */
    private void appendExecuteResult(CommandProcess process, String result) {
        ProfilerModel profilerModel = createProfilerModel(result);
        process.appendResult(profilerModel);
    }

    /**
     * 创建性能分析结果模型
     *
     * @param result 执行结果
     * @return 性能分析结果模型
     */
    private ProfilerModel createProfilerModel(String result) {
        ProfilerModel profilerModel = new ProfilerModel();
        profilerModel.setAction(action);
        profilerModel.setActionArg(actionArg);
        profilerModel.setFormat(format);
        profilerModel.setExecuteResult(result);
        return profilerModel;
    }

    /**
     * 获取支持的事件列表
     *
     * @return 事件列表
     */
    private List<String> events() {
        List<String> result = new ArrayList<String>();

        String execute;
        try {
            /**
             * 获取支持的事件列表
             * <pre>
               Basic events:
                  cpu      - CPU 事件
                  alloc    - 内存分配事件
                  lock     - 锁事件
                  wall     - 壁钟事件
                  itimer   - 间隔定时器事件
             * </pre>
             */
            execute = this.profilerInstance().execute("list");
        } catch (Throwable e) {
            // 发生异常时返回空列表
            return result;
        }
        // 按行分割结果
        String lines[] = execute.split("\\r?\\n");

        // 提取事件名称（以空格开头的行）
        for (String line : lines) {
            if (line.startsWith(" ")) {
                result.add(line.trim());
            }
        }
        return result;
    }

    /**
     * 获取支持的动作列表
     *
     * @return 动作集合
     */
    private Set<String> actions() {
        Set<String> values = new HashSet<String>();
        // 遍历所有枚举值
        for (ProfilerAction action : ProfilerAction.values()) {
            values.add(action.toString());
        }
        return values;
    }

    /**
     * 命令自动补全
     * 根据用户输入提供智能补全建议
     *
     * @param completion 补全上下文
     */
    @Override
    public void complete(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String token = tokens.get(tokens.size() - 1).value();

        // 处理选项参数的补全
        if (tokens.size() >= 2) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            if (cliToken_1.isBlank()) {
                String token_2 = cliToken_2.value();
                // 补全事件类型
                if (token_2.equals("-e") || token_2.equals("--event")) {
                    CompletionUtils.complete(completion, events());
                    return;
                // 补全输出格式
                } else if (token_2.equals("-o") || token_2.equals("--format")) {
                    CompletionUtils.complete(completion, Arrays.asList(
                            "flamegraph", "tree", "jfr",
                            "flat", "traces", "collapsed",
                            "md", "md=10"
                    ));
                    return;
                }
            }
        }

        // 处理选项的补全
        if (token.startsWith("-")) {
            super.complete(completion);
            return;
        }

        // 补全动作类型
        CompletionUtils.complete(completion, actions());
    }

}

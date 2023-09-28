package com.taobao.arthas.core.command.monitor200;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        + "  profiler stop --format html   # output file format, support flat[=N]|traces[=N]|collapsed|flamegraph|tree|jfr\n"
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

    private String action;
    private String actionArg;

    /**
     * which event to trace (cpu, wall, cache-misses, etc.)
     */
    private String event;

    /**
     * profile allocations with BYTES interval
     * according to async-profiler README, alloc may contains non-numeric charactors
     */
    private String alloc;

    /**
     * build allocation profile from live objects only
     */
    private boolean live;

    /**
     * profile contended locks longer than DURATION ns
     * according to async-profiler README, alloc may contains non-numeric charactors
     */
    private String lock;

    /**
     * start Java Flight Recording with the given config along with the profiler
     */
    private String jfrsync;

    /**
     * output file name for dumping
     */
    private String file;

    /**
     * output file format, default value is html.
     */
    private String format;

    /**
     * sampling interval in ns (default: 10'000'000, i.e. 10 ms)
     */
    private Long interval;

    /**
     * maximum Java stack depth (default: 2048)
     */
    private Integer jstackdepth;

    /**
     * profile different threads separately
     */
    private boolean threads;

    /**
     * group threads by scheduling policy
     */
    private boolean sched;

    /**
     * how to collect C stack frames in addition to Java stack
     * MODE is 'fp' (Frame Pointer), 'dwarf', 'lbr' (Last Branch Record) or 'no'
     */
    private String cstack;

    /**
     * use simple class names instead of FQN
     */
    private boolean simple;

    /**
     * print method signatures
     */
    private boolean sig;

    /**
     * annotate Java methods
     */
    private boolean ann;

    /**
     * prepend library names
     */
    private boolean lib;

    /**
     * include only user-mode events
     */
    private boolean alluser;

    /**
     * run profiling for <duration> seconds
     */
    private Long duration;

    /**
     * include stack traces containing PATTERN
     */
    private List<String> includes;

    /**
     * exclude stack traces containing PATTERN
     */
    private List<String> excludes;

    /**
     * automatically start profiling when the specified native function is executed.
     */
    private String begin;

    /**
     * automatically stop profiling when the specified native function is executed.
     */
    private String end;

    /**
     * time-to-safepoint profiling.
     * An alias for --begin SafepointSynchronize::begin --end RuntimeService::record_safepoint_synchronized
     */
    private boolean ttsp;

    /**
     * FlameGraph title
     */
    private String title;

    /**
     * FlameGraph minimum frame width in percent
     */
    private String minwidth;

    /**
     * generate stack-reversed FlameGraph / Call tree
     */
    private boolean reverse;

    /**
     * count the total value (time, bytes, etc.) instead of samples
     */
    private boolean total;

    /**
     * approximate size of JFR chunk in bytes (default: 100 MB)
     */
    private String chunksize;

    /**
     * duration of JFR chunk in seconds (default: 1 hour)
     */
    private String chunktime;

    /**
     * run profiler in a loop (continuous profiling)
     */
    private String loop;

    /**
     * automatically stop profiler at TIME (absolute or relative)
     */
    private String timeout;

    private static String libPath;
    private static AsyncProfiler profiler = null;

    static {
        String profierSoPath = null;
        if (OSUtils.isMac()) {
            // FAT_BINARY support both x86_64/arm64
            profierSoPath = "async-profiler/libasyncProfiler-mac.so";
        }
        if (OSUtils.isLinux()) {
            if (OSUtils.isX86_64() && OSUtils.isMuslLibc()) {
                profierSoPath = "async-profiler/libasyncProfiler-linux-musl-x64.so";
            } else if(OSUtils.isX86_64()){
                profierSoPath = "async-profiler/libasyncProfiler-linux-x64.so";
            } else if (OSUtils.isArm64() && OSUtils.isMuslLibc()) {
                profierSoPath = "async-profiler/libasyncProfiler-linux-musl-arm64.so";
            } else if (OSUtils.isArm64()) {
                profierSoPath = "async-profiler/libasyncProfiler-linux-arm64.so";
            }
        }

        if (profierSoPath != null) {
            CodeSource codeSource = ProfilerCommand.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                try {
                    File bootJarPath = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                    File soFile = new File(bootJarPath.getParentFile(), profierSoPath);
                    if (soFile.exists()) {
                        libPath = soFile.getAbsolutePath();
                    }
                } catch (Throwable e) {
                    logger.error("can not find libasyncProfiler so", e);
                }
            }
        }

    }

    @Argument(argName = "action", index = 0, required = true)
    @Description("Action to execute")
    public void setAction(String action) {
        this.action = action;
    }

    @Argument(argName = "actionArg", index = 1, required = false)
    @Description("Attribute name pattern.")
    public void setActionArg(String actionArg) {
        this.actionArg = actionArg;
    }

    @Option(shortName = "i", longName = "interval")
    @Description("sampling interval in ns (default: 10'000'000, i.e. 10 ms)")
    @DefaultValue("10000000")
    public void setInterval(long interval) {
        this.interval = interval;
    }

    @Option(shortName = "j", longName = "jstackdepth")
    @Description("maximum Java stack depth (default: 2048)")
    public void setJstackdepth(int jstackdepth) {
        this.jstackdepth = jstackdepth;
    }

    @Option(shortName = "f", longName = "file")
    @Description("dump output to <filename>, if ends with html or jfr, content format can be infered")
    public void setFile(String file) {
        this.file = file;
    }

    @Option(shortName = "o", longName = "format")
    @Description("dump output content format(flat[=N]|traces[=N]|collapsed|flamegraph|tree|jfr)")
    public void setFormat(String format) {
        // only for backward compatibility
        if ("html".equals(format)) {
            format = "flamegraph";
        }
        this.format = format;
    }

    @Option(shortName = "e", longName = "event")
    @Description("which event to trace (cpu, alloc, lock, cache-misses etc.), default value is cpu")
    @DefaultValue("cpu")
    public void setEvent(String event) {
        this.event = event;
    }

    @Option(longName = "alloc")
    @Description("allocation profiling interval in bytes")
    public void setAlloc(String alloc) {
        this.alloc = alloc;
    }

    @Option(longName = "live", flag = true)
    @Description("build allocation profile from live objects only")
    public void setLive(boolean live) {
        this.live = live;
    }

    @Option(longName = "lock")
    @Description("lock profiling threshold in nanoseconds")
    public void setLock(String lock) {
        this.lock = lock;
    }

    @Option(longName = "jfrsync")
    @Description("start Java Flight Recording with the given config along with the profiler")
    public void setJfrsync(String jfrsync) {
        this.jfrsync = jfrsync;
    }

    @Option(shortName = "t", longName = "threads", flag = true)
    @Description("profile different threads separately")
    public void setThreads(boolean threads) {
        this.threads = threads;
    }

    @Option(longName = "sched", flag = true)
    @Description("group threads by scheduling policy")
    public void setSched(boolean sched) {
        this.sched = sched;
    }

    @Option(longName = "cstack")
    @Description("how to traverse C stack: fp|dwarf|lbr|no")
    public void setCstack(String cstack) {
        this.cstack = cstack;
    }

    @Option(shortName = "s", flag = true)
    @Description("use simple class names instead of FQN")
    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    @Option(shortName = "g", flag = true)
    @Description("print method signatures")
    public void setSig(boolean sig) {
        this.sig = sig;
    }

    @Option(shortName = "a", flag = true)
    @Description("annotate Java methods")
    public void setAnn(boolean ann) {
        this.ann = ann;
    }

    @Option(shortName = "l", flag = true)
    @Description("prepend library names")
    public void setLib(boolean lib) {
        this.lib = lib;
    }

    @Option(longName = "all-user", flag = true)
    @Description("include only user-mode events")
    public void setAlluser(boolean alluser) {
        this.alluser = alluser;
    }

    @Option(shortName = "d", longName = "duration")
    @Description("run profiling for <duration> seconds")
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Option(shortName = "I", longName = "include")
    @Description("include stack traces containing PATTERN, for example: 'java/*'")
    public void setInclude(List<String> includes) {
        this.includes = includes;
    }

    @Option(shortName = "X", longName = "exclude")
    @Description("exclude stack traces containing PATTERN, for example: '*Unsafe.park*'")
    public void setExclude(List<String> excludes) {
        this.excludes = excludes;
    }

    @Option(longName = "begin")
    @Description("automatically start profiling when the specified native function is executed")
    public void setBegin(String begin) {
        this.begin = begin;
    }

    @Option(longName = "end")
    @Description("automatically stop profiling when the specified native function is executed")
    public void setEnd(String end) {
        this.end = end;
    }

    @Option(longName = "ttsp", flag = true)
    @Description("time-to-safepoint profiling. "
        + "An alias for --begin SafepointSynchronize::begin --end RuntimeService::record_safepoint_synchronized")
    public void setTtsp(boolean ttsp) {
        this.ttsp = ttsp;
    }

    @Option(longName = "title")
    @Description("FlameGraph title")
    public void setTitle(String title) {
        // escape HTML special characters
        // and escape comma to avoid conflicts with JVM TI
        title = title.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace(",", "&#44;");
        this.title = title;
    }

    @Option(longName = "minwidth")
    @Description("FlameGraph minimum frame width in percent")
    public void setMinwidth(String minwidth) {
        this.minwidth = minwidth;
    }

    @Option(longName = "reverse", flag = true)
    @Description("generate stack-reversed FlameGraph / Call tree")
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    @Option(longName = "total", flag = true)
    @Description("count the total value (time, bytes, etc.) instead of samples")
    public void setTotal(boolean total) {
        this.total = total;
    }

    @Option(longName = "chunksize")
    @Description("approximate size limits for a single JFR chunk in bytes (default: 100 MB) or other units")
    public void setChunksize(String chunksize) {
        this.chunksize = chunksize;
    }

    @Option(longName = "chunktime")
    @Description("approximate time limits for a single JFR chunk in second (default: 1 hour) or other units")
    public void setChunktime(String chunktime) {
        this.chunktime = chunktime;
    }

    @Option(longName = "loop")
    @Description("run profiler in a loop (continuous profiling)")
    public void setLoop(String loop) {
        this.loop = loop;
        if (this.action.equals("collect")) {
            this.action = "start";
        }
    }

    @Option(longName = "timeout")
    @Description("automatically stop profiler at TIME (absolute or relative)")
    public void setTimeout(String timeout) {
        this.timeout = timeout;
        if (this.action.equals("collect")) {
            this.action = "start";
        }
    }


    private AsyncProfiler profilerInstance() {
        if (profiler != null) {
            return profiler;
        }

        // try to load from special path
        if (ProfilerAction.load.toString().equals(action)) {
            profiler = AsyncProfiler.getInstance(this.actionArg);
        }

        if (libPath != null) {
            // load from arthas directory
            // 尝试把lib文件复制到临时文件里，避免多次attach时出现 Native Library already loaded in another classloader
            FileOutputStream tmpLibOutputStream = null;
            FileInputStream libInputStream = null;
            try {
                File tmpLibFile = File.createTempFile(VmTool.JNI_LIBRARY_NAME, null);
                tmpLibOutputStream = new FileOutputStream(tmpLibFile);
                libInputStream = new FileInputStream(libPath);

                IOUtils.copy(libInputStream, tmpLibOutputStream);
                libPath = tmpLibFile.getAbsolutePath();
                logger.debug("copy {} to {}", libPath, tmpLibFile);
            } catch (Throwable e) {
                logger.error("try to copy lib error! libPath: {}", libPath, e);
            } finally {
                IOUtils.close(libInputStream);
                IOUtils.close(tmpLibOutputStream);
            }
            profiler = AsyncProfiler.getInstance(libPath);
        } else {
            if (OSUtils.isLinux() || OSUtils.isMac()) {
                throw new IllegalStateException("Can not find libasyncProfiler so, please check the arthas directory.");
            } else {
                throw new IllegalStateException("Current OS do not support AsyncProfiler, Only support Linux/Mac.");
            }
        }

        return profiler;
    }

    /**
     * https://github.com/async-profiler/async-profiler/blob/v2.9/profiler.sh#L154
     */
    public enum ProfilerAction {
        // start, resume, stop, dump, check, status, meminfo, list, collect,
        start, resume, stop, dump, check, status, meminfo, list, collect,
        version,

        load,
        execute,
        dumpCollapsed, dumpFlat, dumpTraces, getSamples,
        actions
    }

    private String executeArgs(ProfilerAction action) {
        StringBuilder sb = new StringBuilder();
        final char COMMA = ',';

        // start - start profiling
        // resume - start or resume profiling without resetting collected data
        // stop - stop profiling
        sb.append(action).append(COMMA);

        if (this.event != null) {
            sb.append("event=").append(this.event).append(COMMA);
        }
        if (this.alloc!= null) {
            sb.append("alloc=").append(this.alloc).append(COMMA);
        }
        if (this.live) {
            sb.append("live").append(COMMA);
        }
        if (this.lock!= null) {
            sb.append("lock=").append(this.lock).append(COMMA);
        }
        if (this.jfrsync != null) {
            this.format = "jfr";
            sb.append("jfrsync=").append(this.jfrsync).append(COMMA);
        }
        if (this.file != null) {
            sb.append("file=").append(this.file).append(COMMA);
        }
        if (this.format != null) {
            sb.append(this.format).append(COMMA);
        }
        if (this.interval != null) {
            sb.append("interval=").append(this.interval).append(COMMA);
        }
        if (this.jstackdepth != null) {
            sb.append("jstackdepth=").append(this.jstackdepth).append(COMMA);
        }
        if (this.threads) {
            sb.append("threads").append(COMMA);
        }
        if (this.sched) {
            sb.append("sched").append(COMMA);
        }
        if (this.cstack != null) {
            sb.append("cstack=").append(this.cstack).append(COMMA);
        }
        if (this.simple) {
            sb.append("simple").append(COMMA);
        }
        if (this.sig) {
            sb.append("sig").append(COMMA);
        }
        if (this.ann) {
            sb.append("ann").append(COMMA);
        }
        if (this.lib) {
            sb.append("lib").append(COMMA);
        }
        if (this.alluser) {
            sb.append("alluser").append(COMMA);
        }
        if (this.includes != null) {
            for (String include : includes) {
                sb.append("include=").append(include).append(COMMA);
            }
        }
        if (this.excludes != null) {
            for (String exclude : excludes) {
                sb.append("exclude=").append(exclude).append(COMMA);
            }
        }
        if (this.ttsp) {
            this.begin = "SafepointSynchronize::begin";
            this.end = "RuntimeService::record_safepoint_synchronized";
        }
        if (this.begin != null) {
            sb.append("begin=").append(this.begin).append(COMMA);
        }
        if (this.end != null) {
            sb.append("end=").append(this.end).append(COMMA);
        }

        if (this.title != null) {
            sb.append("title=").append(this.title).append(COMMA);
        }
        if (this.minwidth != null) {
            sb.append("minwidth=").append(this.minwidth).append(COMMA);
        }
        if (this.reverse) {
            sb.append("reverse").append(COMMA);
        }
        if (this.total) {
            sb.append("total").append(COMMA);
        }
        if (this.chunksize != null) {
            sb.append("chunksize=").append(this.chunksize).append(COMMA);
        }
        if (this.chunktime!= null) {
            sb.append("chunktime=").append(this.chunktime).append(COMMA);
        }
        if (this.loop != null) {
            sb.append("loop=").append(this.loop).append(COMMA);
        }
        if (this.timeout != null) {
            sb.append("timeout=").append(this.timeout).append(COMMA);
        }

        return sb.toString();
    }

    private static String execute(AsyncProfiler asyncProfiler, String arg)
            throws IllegalArgumentException, IOException {
        String result = asyncProfiler.execute(arg);
        if (!result.endsWith("\n")) {
            result += "\n";
        }
        return result;
    }

    @Override
    public void process(final CommandProcess process) {
        try {
            ProfilerAction profilerAction = ProfilerAction.valueOf(action);

            if (ProfilerAction.actions.equals(profilerAction)) {
                process.appendResult(new ProfilerModel(actions()));
                process.end();
                return;
            }

            final AsyncProfiler asyncProfiler = this.profilerInstance();

            if (ProfilerAction.execute.equals(profilerAction)) {
                if (actionArg == null) {
                    process.end(1, "actionArg can not be empty.");
                    return;
                }
                String result = execute(asyncProfiler, this.actionArg);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.collect.equals(profilerAction)) {
                String executeArgs = executeArgs(ProfilerAction.collect);
                String result = execute(asyncProfiler, executeArgs);
                ProfilerModel profilerModel = createProfilerModel(result);

                if (this.duration != null) {
                    final String outputFile = outputFile();
                    profilerModel.setOutputFile(outputFile);
                    profilerModel.setDuration(duration);

                    // 延时执行stop
                    ArthasBootstrap.getInstance().getScheduledExecutorService().schedule(new Runnable() {
                        @Override
                        public void run() {
                            //在异步线程执行，profiler命令已经结束，不能输出到客户端
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
                }
                process.appendResult(profilerModel);
            } else if (ProfilerAction.start.equals(profilerAction)) {
                String executeArgs = executeArgs(ProfilerAction.start);
                String result = execute(asyncProfiler, executeArgs);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.stop.equals(profilerAction)) {
                ProfilerModel profilerModel = processStop(asyncProfiler, profilerAction);
                process.appendResult(profilerModel);
            } else if (ProfilerAction.dump.equals(profilerAction)) {
                ProfilerModel profilerModel = processStop(asyncProfiler, profilerAction);
                process.appendResult(profilerModel);
            } else if (ProfilerAction.resume.equals(profilerAction)) {
                String executeArgs = executeArgs(ProfilerAction.resume);
                String result = execute(asyncProfiler, executeArgs);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.check.equals(profilerAction)) {
                String executeArgs = executeArgs(ProfilerAction.check);
                String result = execute(asyncProfiler, executeArgs);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.version.equals(profilerAction)) {
                String result = asyncProfiler.execute("version=full");
                appendExecuteResult(process, result);
            } else if (ProfilerAction.status.equals(profilerAction)
                    || ProfilerAction.meminfo.equals(profilerAction)
                    || ProfilerAction.list.equals(profilerAction)) {
                String result = asyncProfiler.execute(profilerAction.toString());
                appendExecuteResult(process, result);
            } else if (ProfilerAction.dumpCollapsed.equals(profilerAction)) {
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
                int maxMethods = 0;
                if (actionArg != null) {
                    maxMethods = Integer.valueOf(actionArg);
                }
                String result = asyncProfiler.dumpFlat(maxMethods);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.dumpTraces.equals(profilerAction)) {
                int maxTraces = 0;
                if (actionArg != null) {
                    maxTraces = Integer.valueOf(actionArg);
                }
                String result = asyncProfiler.dumpTraces(maxTraces);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.getSamples.equals(profilerAction)) {
                String result = "" + asyncProfiler.getSamples() + "\n";
                appendExecuteResult(process, result);
            }
            process.end();
        } catch (Throwable e) {
            logger.error("AsyncProfiler error", e);
            process.end(1, "AsyncProfiler error: "+e.getMessage());
        }
    }

    private ProfilerModel processStop(AsyncProfiler asyncProfiler, ProfilerAction profilerAction) throws IOException {
        String outputFile = outputFile();
        String executeArgs = executeArgs(profilerAction);
        String result = execute(asyncProfiler, executeArgs);

        ProfilerModel profilerModel = createProfilerModel(result);
        profilerModel.setOutputFile(outputFile);
        return profilerModel;
    }

    private String outputFile() throws IOException {
        if (this.file == null) {
            String fileExt = outputFileExt();
            File outputPath = ArthasBootstrap.getInstance().getOutputPath();
            if (outputPath != null) {
                this.file = new File(outputPath,
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "." + fileExt)
                                .getAbsolutePath();
            } else {
                this.file = File.createTempFile("arthas-output", "." + fileExt).getAbsolutePath();
            }
        }
        return file;
    }

    /**
     * This method should only be called when {@code this.file == null} is true.
     */
    private String outputFileExt() {
        String fileExt = "";
        if (this.format == null) {
            fileExt = "html";
        } else if (this.format.startsWith("flat") || this.format.startsWith("traces") 
                || this.format.equals("collapsed")) {
            fileExt = "txt";
        } else if (this.format.equals("flamegraph") || this.format.equals("tree")) {
            fileExt = "html";
        } else if (this.format.equals("jfr")) {
            fileExt = "jfr";
        } else {
            // illegal -o option makes async-profiler use flat
            fileExt = "txt";
        }
        return fileExt;
    }

    private void appendExecuteResult(CommandProcess process, String result) {
        ProfilerModel profilerModel = createProfilerModel(result);
        process.appendResult(profilerModel);
    }

    private ProfilerModel createProfilerModel(String result) {
        ProfilerModel profilerModel = new ProfilerModel();
        profilerModel.setAction(action);
        profilerModel.setActionArg(actionArg);
        profilerModel.setExecuteResult(result);
        return profilerModel;
    }

    private List<String> events() {
        List<String> result = new ArrayList<String>();

        String execute;
        try {
            /**
             * <pre>
               Basic events:
                  cpu
                  alloc
                  lock
                  wall
                  itimer
             * </pre>
             */
            execute = this.profilerInstance().execute("list");
        } catch (Throwable e) {
            // ignore
            return result;
        }
        String lines[] = execute.split("\\r?\\n");

        for (String line : lines) {
            if (line.startsWith(" ")) {
                result.add(line.trim());
            }
        }
        return result;
    }

    private Set<String> actions() {
        Set<String> values = new HashSet<String>();
        for (ProfilerAction action : ProfilerAction.values()) {
            values.add(action.toString());
        }
        return values;
    }

    @Override
    public void complete(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String token = tokens.get(tokens.size() - 1).value();

        if (tokens.size() >= 2) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            if (cliToken_1.isBlank()) {
                String token_2 = cliToken_2.value();
                if (token_2.equals("-e") || token_2.equals("--event")) {
                    CompletionUtils.complete(completion, events());
                    return;
                } else if (token_2.equals("-f") || token_2.equals("--format")) {
                    CompletionUtils.complete(completion, Arrays.asList("html", "jfr"));
                    return;
                }
            }
        }

        if (token.startsWith("-")) {
            super.complete(completion);
            return;
        }

        CompletionUtils.complete(completion, actions());
    }

}
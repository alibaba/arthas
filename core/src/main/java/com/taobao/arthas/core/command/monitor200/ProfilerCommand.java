package com.taobao.arthas.core.command.monitor200;

import java.io.File;
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
        + "  profiler stop --format svg   # output file format, support svg,html,jfr\n"
        + "  profiler stop --file /tmp/result.html\n"
        + "  profiler stop --threads \n"
        + "  profiler start --include 'java/*' --include 'demo/*' --exclude '*Unsafe.park*'\n"
        + "  profiler status\n"
        + "  profiler resume              # Start or resume profiling without resetting collected data.\n"
        + "  profiler getSamples          # Get the number of samples collected during the profiling session\n"
        + "  profiler dumpFlat            # Dump flat profile, i.e. the histogram of the hottest methods\n"
        + "  profiler dumpCollapsed       # Dump profile in 'collapsed stacktraces' format\n"
        + "  profiler dumpTraces          # Dump collected stack traces\n"
        + "  profiler execute 'start,framebuf=5000000'      # Execute an agent-compatible profiling command\n"
        + "  profiler execute 'stop,file=/tmp/result.svg'   # Execute an agent-compatible profiling command\n"
        + Constants.WIKI + Constants.WIKI_HOME + "profiler")
//@formatter:on
public class ProfilerCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(ProfilerCommand.class);

    private String action;
    private String actionArg;

    private String event;

    private String file;
    /**
     * output file format, default value is svg.
     */
    private String format;

    /**
     * sampling interval in ns (default: 10'000'000, i.e. 10 ms)
     */
    private Long interval;

    /**
     * size of the buffer for stack frames (default: 1'000'000)
     */
    private Long framebuf;

    /**
     * profile different threads separately
     */
    private boolean threads;

    /**
     * include only kernel-mode events
     */
    private boolean allkernel;

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

    private static String libPath;
    private static AsyncProfiler profiler = null;

    static {
        String profierSoPath = null;
        if (OSUtils.isMac()) {
            profierSoPath = "async-profiler/libasyncProfiler-mac-x64.so";
        }
        if (OSUtils.isLinux()) {
            profierSoPath = "async-profiler/libasyncProfiler-linux-x64.so";
            if (OSUtils.isArm32()) {
                profierSoPath = "async-profiler/libasyncProfiler-linux-arm.so";
            } else if (OSUtils.isArm64()) {
                profierSoPath = "async-profiler/libasyncProfiler-linux-aarch64.so";
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

    @Option(shortName = "b", longName = "framebuf")
    @Description("size of the buffer for stack frames (default: 1'000'000)")
    @DefaultValue("1000000")
    public void setFramebuf(long framebuf) {
        this.framebuf = framebuf;
    }

    @Option(shortName = "f", longName = "file")
    @Description("dump output to <filename>")
    public void setFile(String file) {
        this.file = file;
    }

    @Option(longName = "format")
    @Description("dump output file format(svg, html, jfr), default valut is svg")
    @DefaultValue("svg")
    public void setFormat(String format) {
        this.format = format;
    }

    @Option(shortName = "e", longName = "event")
    @Description("which event to trace (cpu, alloc, lock, cache-misses etc.), default value is cpu")
    @DefaultValue("cpu")
    public void setEvent(String event) {
        this.event = event;
    }

    @Option(longName = "threads", flag = true)
    @Description("profile different threads separately")
    public void setThreads(boolean threads) {
        this.threads = threads;
    }

    @Option(longName = "allkernel", flag = true)
    @Description("include only kernel-mode events")
    public void setAllkernel(boolean allkernel) {
        this.allkernel = allkernel;
    }

    @Option(longName = "alluser", flag = true)
    @Description("include only user-mode events")
    public void setAlluser(boolean alluser) {
        this.alluser = alluser;
    }

    @Option(shortName = "d", longName = "duration")
    @Description("run profiling for <duration> seconds")
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Option(longName = "include")
    @Description("include stack traces containing PATTERN, for example: 'java/*'")
    public void setInclude(List<String> includes) {
        this.includes = includes;
    }

    @Option(longName = "exclude")
    @Description("exclude stack traces containing PATTERN, for example: '*Unsafe.park*'")
    public void setExclude(List<String> excludes) {
        this.excludes = excludes;
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
     * https://github.com/jvm-profiling-tools/async-profiler/blob/v1.8.1/src/arguments.cpp#L50
     *
     */
    public enum ProfilerAction {
        execute, start, stop, resume, list, version, status, load,

        dumpCollapsed, dumpFlat, dumpTraces, getSamples,

        actions
    }

    private String executeArgs(ProfilerAction action) {
        StringBuilder sb = new StringBuilder();

        // start - start profiling
        // resume - start or resume profiling without resetting collected data
        // stop - stop profiling
        sb.append(action).append(',');

        if (this.event != null) {
            sb.append("event=").append(this.event).append(',');
        }
        if (this.file != null) {
            sb.append("file=").append(this.file).append(',');
        }
        if (this.interval != null) {
            sb.append("interval=").append(this.interval).append(',');
        }
        if (this.framebuf != null) {
            sb.append("framebuf=").append(this.framebuf).append(',');
        }
        if (this.threads) {
            sb.append("threads").append(',');
        }
        if (this.allkernel) {
            sb.append("allkernel").append(',');
        }
        if (this.alluser) {
            sb.append("alluser").append(',');
        }
        if (this.includes != null) {
            for (String include : includes) {
                sb.append("include=").append(include).append(',');
            }
        }
        if (this.excludes != null) {
            for (String exclude : excludes) {
                sb.append("exclude=").append(exclude).append(',');
            }
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
            } else if (ProfilerAction.start.equals(profilerAction)) {
                //jfr录制，必须在start的时候就指定文件路径
                if (this.file == null && "jfr".equals(format)) {
                    this.file = outputFile();
                }
                String executeArgs = executeArgs(ProfilerAction.start);
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
                                ProfilerModel model = processStop(asyncProfiler);
                                logger.info("profiler output file: " + model.getOutputFile());
                                logger.info("stop profiler successfully.");
                            } catch (Throwable e) {
                                logger.error("stop profiler failure", e);
                            }
                        }
                    }, this.duration, TimeUnit.SECONDS);
                }
                process.appendResult(profilerModel);
            } else if (ProfilerAction.stop.equals(profilerAction)) {
                ProfilerModel profilerModel = processStop(asyncProfiler);
                process.appendResult(profilerModel);
            } else if (ProfilerAction.resume.equals(profilerAction)) {
                String executeArgs = executeArgs(ProfilerAction.resume);
                String result = execute(asyncProfiler, executeArgs);
                appendExecuteResult(process, result);
            } else if (ProfilerAction.list.equals(profilerAction)) {
                String result = asyncProfiler.execute("list");
                appendExecuteResult(process, result);
            } else if (ProfilerAction.version.equals(profilerAction)) {
                String result = asyncProfiler.execute("version");
                appendExecuteResult(process, result);
            } else if (ProfilerAction.status.equals(profilerAction)) {
                String result = asyncProfiler.execute("status");
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

    private ProfilerModel processStop(AsyncProfiler asyncProfiler) throws IOException {
        String outputFile = outputFile();
        String executeArgs = executeArgs(ProfilerAction.stop);
        String result = execute(asyncProfiler, executeArgs);

        ProfilerModel profilerModel = createProfilerModel(result);
        profilerModel.setOutputFile(outputFile);
        return profilerModel;
    }

    private String outputFile() throws IOException {
        if (this.file == null) {
            File outputPath = ArthasBootstrap.getInstance().getOutputPath();
            if (outputPath != null) {
                this.file = new File(outputPath,
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "." + this.format)
                                .getAbsolutePath();
            } else {
                this.file = File.createTempFile("arthas-output", "." + this.format).getAbsolutePath();
            }
        }
        return file;
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

        if (lines != null) {
            for (String line : lines) {
                if (line.startsWith(" ")) {
                    result.add(line.trim());
                }
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
                    CompletionUtils.complete(completion, Arrays.asList("svg", "html", "jfr"));
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
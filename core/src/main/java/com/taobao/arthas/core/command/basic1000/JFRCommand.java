package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.JFRModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Java飞行记录器（JFR）命令类
 * 用于启动、停止、查看和导出JFR记录
 *
 * @author arthas
 */
@Name("jfr")
@Summary("Java Flight Recorder Command")
@Description(Constants.EXAMPLE +
        "  jfr start  # start a new JFR recording\n" +
        "  jfr start -n myRecording --duration 60s -f /tmp/myRecording.jfr \n" +
        "  jfr status                   # list all recordings\n" +
        "  jfr status -r 1              # list recording id = 1 \n" +
        "  jfr status --state running   # list recordings state = running\n" +
        "  jfr stop -r 1               # stop a JFR recording to default file\n" +
        "  jfr stop -r 1 -f /tmp/myRecording.jfr\n" +
        "  jfr dump -r 1               # copy contents of a JFR recording to default file\n" +
        "  jfr dump -r 1 -f /tmp/myRecording.jfr\n" +
        Constants.WIKI + Constants.WIKI_HOME + "jfr")
public class JFRCommand extends AnnotatedCommand {

    // 子命令名称：start、status、stop、dump
    private String cmd;
    // 录制名称
    private String name;
    // 设置文件
    private String settings;
    // JVM关闭时是否转储
    private Boolean dumpOnExit;
    // 延迟启动时间
    private String delay;
    // 录制持续时间
    private String duration;
    // 输出文件名
    private String filename;
    // 最大保留时间
    private String maxAge;
    // 最大文件大小
    private String maxSize;
    // 录制ID
    private Long recording;
    // 录制状态
    private String state;
    // 结果模型
    private JFRModel result = new JFRModel();
    // 所有活跃的录制记录，key为录制ID
    private static Map<Long, Recording> recordings = new ConcurrentHashMap<Long, Recording>();

    @Argument(index = 0, argName = "cmd", required = true)
    @Description("command name (start status stop dump)")
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Option(shortName = "n", longName = "name")
    @Description("Name that can be used to identify recording, e.g. \"My Recording\"")
    public void setName(String name) {
        this.name = name;
    }

    @Option(shortName = "s", longName = "settings")
    @Description("Settings file(s), e.g. profile or default. See JRE_HOME/lib/jfr (STRING , default)")
    public void setSettings(String settings) {
        this.settings = settings;
    }

    @Option(longName = "dumponexit")
    @Description("Dump running recording when JVM shuts down (BOOLEAN, false)")
    public void setDumpOnExit(Boolean dumpOnExit) {
        this.dumpOnExit = dumpOnExit;
    }

    @Option(shortName = "d", longName = "delay")
    @Description("Delay recording start with (s)econds, (m)inutes), (h)ours), or (d)ays, e.g. 5h. (NANOTIME, 0)")
    public void setDelay(String delay) {
        this.delay = delay;
    }

    @Option(longName = "duration")
    @Description("Duration of recording in (s)econds, (m)inutes, (h)ours, or (d)ays, e.g. 300s. (NANOTIME, 0)")
    public void setDuration(String duration) {
        this.duration = duration;
    }

    @Option(shortName = "f", longName = "filename")
    @Description("Resulting recording filename, e.g. /tmp/MyRecording.jfr.")
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Option(longName = "maxage")
    @Description("Maximum time to keep recorded data (on disk) in (s)econds, (m)inutes, (h)ours, or (d)ays, e.g. 60m, or default for no limit (NANOTIME, 0)")
    public void setMaxAge(String maxAge) {
        this.maxAge = maxAge;
    }

    @Option(longName = "maxsize")
    @Description("Maximum amount of bytes to keep (on disk) in (k)B, (M)B or (G)B, e.g. 500M, 0 for no limit (MEMORY SIZE, 250MB)")
    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    @Option(shortName = "r", longName = "recording")
    @Description("Recording number, or omit to see all recordings (LONG, -1)")
    public void setRecording(Long recording) {
        this.recording = recording;
    }

    @Option(longName = "state")
    @Description("Query recordings by sate (new, delay, running, stopped, closed)")
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 获取子命令名称
     * @return 子命令名称
     */
    public String getCmd() {
        return cmd;
    }

    /**
     * 获取录制名称
     * @return 录制名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取设置文件
     * @return 设置文件
     */
    public String getSettings() {
        return settings;
    }

    /**
     * 是否在JVM退出时转储
     * @return 是否转储
     */
    public Boolean isDumpOnExit() {
        return dumpOnExit;
    }

    /**
     * 获取延迟时间
     * @return 延迟时间
     */
    public String getDelay() {
        return delay;
    }

    /**
     * 获取持续时间
     * @return 持续时间
     */
    public String getDuration() {
        return duration;
    }

    /**
     * 获取文件名
     * @return 文件名
     */
    public String getFilename() {
        return filename;
    }

    /**
     * 获取最大保留时间
     * @return 最大保留时间
     */
    public String getMaxAge() {
        return maxAge;
    }

    /**
     * 获取最大文件大小
     * @return 最大文件大小
     */
    public String getMaxSize() {
        return maxSize;
    }

    /**
     * 获取录制ID
     * @return 录制ID
     */
    public Long getRecording() {
        return recording;
    }

    /**
     * 获取录制状态
     * @return 录制状态
     */
    public String getState() {
        return state;
    }

    @Override
    public void process(CommandProcess process) {

        // 处理start命令：启动新的JFR录制
        if ("start".equals(cmd)) {
            Configuration c = null;
            try {
                // 如果没有指定设置文件，使用默认配置
                if (getSettings() == null) {
                    setSettings("default");
                }
                // 加载JFR配置
                c = Configuration.getConfiguration(settings);
            } catch (Throwable e) {
                process.end(-1, "Could not start recording, not able to read settings");
            }
            // 创建录制对象
            Recording r = new Recording(c);

            // 如果指定了输出文件，设置目标文件
            if (getFilename() != null) {
                try {
                    r.setDestination(Paths.get(getFilename()));
                } catch (IOException e) {
                    r.close();
                    process.end(-1, "Could not start recording, not able to write to file " + getFilename() + e.getMessage());
                }
            }

            // 设置最大文件大小
            if (getMaxSize() != null) {
                try {
                    r.setMaxSize(parseSize(getMaxSize()));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
            }

            // 设置最大保留时间
            if (getMaxAge() != null) {
                try {
                    r.setMaxAge(Duration.ofNanos(parseTimespan(getMaxAge())));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
            }

            // 设置JVM退出时是否转储
            if (isDumpOnExit() != false) {
                r.setDumpOnExit(isDumpOnExit().booleanValue());
            }

            // 设置录制持续时间
            if (getDuration() != null) {
                try {
                    r.setDuration(Duration.ofNanos(parseTimespan(getDuration())));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
            }

            // 设置录制名称
            if (getName() == null) {
                r.setName("Recording-" + r.getId());
            } else {
                r.setName(getName());
            }

            // 保存录制到map中
            long id = r.getId();
            recordings.put(id, r);

            // 处理延迟启动
            if (getDelay() != null) {
                try {
                    r.scheduleStart(Duration.ofNanos(parseTimespan(getDelay())));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
                result.setJfrOutput("Recording " + r.getId() + " scheduled to start in " + getDelay());
            } else {
                // 立即启动录制
                r.start();
                result.setJfrOutput("Started recording " + r.getId() + ".");
            }

            // 如果没有指定任何限制，使用默认的250MB
            if (duration == null && maxAge == null && maxSize == null) {
                result.setJfrOutput(" No limit specified, using maxsize=250MB as default.");
                r.setMaxSize(250 * 1024L * 1024L);
            }

            // 如果指定了文件名和持续时间，提示结果将写入的位置
            if (filename != null && duration != null) {
                result.setJfrOutput(" The result will be written to:\n" + filename);
            }
        } else if ("status".equals(cmd)) {
            // 处理status命令：查看录制状态
            // 查看指定ID的录制
            if (getRecording() != null) {
                Recording r = recordings.get(getRecording());
                if (r == null) {
                    process.end(-1, "recording not exit");
                }
                printRecording(r);
            } else {// 列出所有录制
                List<Recording> recordingList;
                // 如果指定了状态，按状态筛选
                if (state != null) {
                    recordingList = findRecordingByState(state);
                } else {
                    recordingList = new ArrayList<Recording>(recordings.values());
                }
                if (recordingList.isEmpty()) {
                    process.end(-1, "No available recordings.\n Use jfr start to start a recording.\n");
                } else {
                    // 打印每个录制的信息
                    for (Recording recording : recordingList) {
                        printRecording(recording);
                    }
                }
            }
        } else if ("dump".equals(cmd)) {
            // 处理dump命令：导出录制内容
            if (recordings.isEmpty()) {
                process.end(-1, "No recordings to dump. Use jfr start to start a recording.");
            }
            if (getRecording() != null) {
                Recording r = recordings.get(getRecording());
                if (r == null) {
                    process.end(-1, "recording not exit");
                }
                // 如果没有指定输出文件，生成默认文件名
                if (getFilename() == null) {
                    try {
                        setFilename(outputFile());
                    } catch (IOException e) {
                        process.end(-1, e.getMessage());
                    }
                }

                try {
                    // 导出录制内容
                    r.dump(Paths.get(getFilename()));
                } catch (IOException e) {
                    process.end(-1, "Could not to dump. " + e.getMessage());
                }
                result.setJfrOutput("Dump recording " + r.getId() + ", The result will be written to:\n" + getFilename());
            } else {
                process.end(-1, "Failed to dump. Please input recording id");
            }

        } else if ("stop".equals(cmd)) {
            // 处理stop命令：停止录制
            if (recordings.isEmpty()) {
                process.end(-1, "No recordings to stop. Use jfr start to start a recording.");
            }
            if (getRecording() != null) {
                // 从map中移除录制
                Recording r = recordings.remove(getRecording());
                if (r == null) {
                    process.end(-1, "recording not exit");
                }
                // 检查录制状态，已经停止或关闭的不能再停止
                if ("CLOSED".equals(r.getState().toString()) || "STOPPED".equals(r.getState().toString())) {
                    process.end(-1, "Failed to stop recording, state can not be closed/stopped");
                }
                // 如果没有指定输出文件，生成默认文件名
                if (getFilename() == null) {
                    try {
                        setFilename(outputFile());
                    } catch (IOException e) {
                        process.end(-1, e.getMessage());
                    }
                }
                try {
                    // 设置输出文件
                    r.setDestination(Paths.get(getFilename()));
                } catch (IOException e) {
                    process.end(-1, "Failed to stop " + r.getName() + ". Could not set destination for " + filename + "to file" + e.getMessage());
                }

                // 停止录制
                r.stop();
                result.setJfrOutput("Stop recording " + r.getId() + ", The result will be written to:\n" + getFilename());
                // 关闭录制
                r.close();
            } else {
                process.end(-1, "Failed to stop. please input recording id");
            }
        } else {
            // 不支持的命令
            process.end(-1, "Please input correct jfr command (start status stop dump)");
        }

        // 添加结果并结束处理
        process.appendResult(result);
        process.end();
    }

    /**
     * 解析文件大小字符串
     * 支持的单位：b(字节)、k(KB)、m(MB)、g(GB)
     * @param s 大小字符串，如 "500M"
     * @return 字节数
     * @throws Exception 解析失败时抛出异常
     */
    public long parseSize(String s) throws Exception {
        s = s.toLowerCase();
        if (s.endsWith("b")) {
            return Long.parseLong(s.substring(0, s.length() - 1).trim());
        } else if (s.endsWith("k")) {
            return 1024 * Long.parseLong(s.substring(0, s.length() - 1).trim());
        } else if (s.endsWith("m")) {
            return 1024 * 1024 * Long.parseLong(s.substring(0, s.length() - 1).trim());
        } else if (s.endsWith("g")) {
            return 1024 * 1024 * 1024 * Long.parseLong(s.substring(0, s.length() - 1).trim());
        } else {
            try {
                return Long.parseLong(s);
            } catch (Exception e) {
                throw new NumberFormatException("'" + s + "' is not a valid size. Should be numeric value followed by a unit, i.e. 20M. Valid units k, M, G");
            }
        }
    }

    /**
     * 解析时间跨度字符串
     * 支持的单位：s(秒)、m(分)、h(小时)、d(天)
     * @param s 时间字符串，如 "60s"、"5h"
     * @return 纳秒数
     * @throws Exception 解析失败时抛出异常
     */
    public long parseTimespan(String s) throws Exception {
        s = s.toLowerCase();
        if (s.endsWith("s")) {
            return TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else if (s.endsWith("m")) {
            return 60 * TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else if (s.endsWith("h")) {
            return 60 * 60 * TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else if (s.endsWith("d")) {
            return 24 * 60 * 60 * TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException var2) {
                throw new NumberFormatException("'" + s + "' is not a valid timespan. Shoule be numeric value followed by a unit, i.e. 20s. Valid units s, m, h and d.");
            }
        }
    }

    /**
     * 根据状态查找录制记录
     * @param state 录制状态
     * @return 匹配的录制列表
     */
    private List<Recording> findRecordingByState(String state) {
        List<Recording> resultRecordingList = new ArrayList<Recording>();
        Collection<Recording> recordingList = recordings.values();
        for (Recording recording : recordingList) {
            // 比较录制状态（不区分大小写）
            if (recording.getState().toString().toLowerCase().equals(state)) {
                resultRecordingList.add(recording);
            }
        }
        return resultRecordingList;
    }

    /**
     * 打印录制信息
     * @param recording 录制对象
     */
    private void printRecording(Recording recording) {
        // 格式化录制基本信息
        String format = "Recording: recording=" + recording.getId() + " name=" + recording.getName() + "";
        result.setJfrOutput(format);
        // 如果有持续时间，添加持续时间信息
        Duration duration = recording.getDuration();
        if (duration != null) {
            result.setJfrOutput(" duration=" + duration.toString());
        }
        // 添加录制状态
        result.setJfrOutput(" (" + recording.getState().toString().toLowerCase() + ")\n");
    }

    /**
     * 生成输出文件名
     * 如果没有指定文件名，使用时间戳生成默认文件名
     * @return 文件名
     * @throws IOException 文件操作异常
     */
    private String outputFile() throws IOException {
        if (this.filename == null) {
            File outputPath = ArthasBootstrap.getInstance().getOutputPath();
            if (outputPath != null) {
                // 使用输出目录，生成带时间戳的文件名
                this.filename = new File(outputPath,
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".jfr")
                        .getAbsolutePath();
            } else {
                // 使用临时目录
                this.filename = File.createTempFile("arthas-output", ".jfr").getAbsolutePath();
            }
        }
        return filename;
    }

    @Override
    public void complete(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String token = tokens.get(tokens.size() - 1).value();

        // 如果是选项参数，使用默认补全
        if (token.startsWith("-")) {
            super.complete(completion);
            return;
        }
        // 补全子命令
        List<String> cmd = Arrays.asList("start", "status", "dump", "stop");
        CompletionUtils.complete(completion, cmd);
    }
}

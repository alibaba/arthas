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

    private String cmd;
    private String name;
    private String settings;
    private Boolean dumpOnExit;
    private String delay;
    private String duration;
    private String filename;
    private String maxAge;
    private String maxSize;
    private Long recording;
    private String state;
    private JFRModel result = new JFRModel();
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

    public String getCmd() {
        return cmd;
    }

    public String getName() {
        return name;
    }

    public String getSettings() {
        return settings;
    }

    public Boolean isDumpOnExit() {
        return dumpOnExit;
    }

    public String getDelay() {
        return delay;
    }

    public String getDuration() {
        return duration;
    }

    public String getFilename() {
        return filename;
    }

    public String getMaxAge() {
        return maxAge;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public Long getRecording() {
        return recording;
    }

    public String getState() {
        return state;
    }

    @Override
    public void process(CommandProcess process) {

        if ("start".equals(cmd)) {
            Configuration c = null;
            try {
                if (getSettings() == null) {
                    setSettings("default");
                }
                c = Configuration.getConfiguration(settings);
            } catch (Throwable e) {
                process.end(-1, "Could not start recording, not able to read settings");
            }
            Recording r = new Recording(c);

            if (getFilename() != null) {
                try {
                    r.setDestination(Paths.get(getFilename()));
                } catch (IOException e) {
                    r.close();
                    process.end(-1, "Could not start recording, not able to write to file " + getFilename() + e.getMessage());
                }
            }

            if (getMaxSize() != null) {
                try {
                    r.setMaxSize(parseSize(getMaxSize()));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
            }

            if (getMaxAge() != null) {
                try {
                    r.setMaxAge(Duration.ofNanos(parseTimespan(getMaxAge())));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
            }

            if (isDumpOnExit() != false) {
                r.setDumpOnExit(isDumpOnExit().booleanValue());
            }

            if (getDuration() != null) {
                try {
                    r.setDuration(Duration.ofNanos(parseTimespan(getDuration())));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
            }

            if (getName() == null) {
                r.setName("Recording-" + r.getId());
            } else {
                r.setName(getName());
            }

            long id = r.getId();
            recordings.put(id, r);

            if (getDelay() != null) {
                try {
                    r.scheduleStart(Duration.ofNanos(parseTimespan(getDelay())));
                } catch (Exception e) {
                    process.end(-1, e.getMessage());
                }
                result.setJfrOutput("Recording " + r.getId() + " scheduled to start in " + getDelay());
            } else {
                r.start();
                result.setJfrOutput("Started recording " + r.getId() + ".");
            }

            if (duration == null && maxAge == null && maxSize == null) {
                result.setJfrOutput(" No limit specified, using maxsize=250MB as default.");
                r.setMaxSize(250 * 1024L * 1024L);
            }

            if (filename != null && duration != null) {
                result.setJfrOutput(" The result will be written to:\n" + filename);
            }
        } else if ("status".equals(cmd)) {
            // list recording id = recording
            if (getRecording() != null) {
                Recording r = recordings.get(getRecording());
                if (r == null) {
                    process.end(-1, "recording not exit");
                }
                printRecording(r);
            } else {// list all recordings
                List<Recording> recordingList;
                if (state != null) {
                    recordingList = findRecordingByState(state);
                } else {
                    recordingList = new ArrayList<Recording>(recordings.values());
                }
                if (recordingList.isEmpty()) {
                    process.end(-1, "No available recordings.\n Use jfr start to start a recording.\n");
                } else {
                    for (Recording recording : recordingList) {
                        printRecording(recording);
                    }
                }
            }
        } else if ("dump".equals(cmd)) {
            if (recordings.isEmpty()) {
                process.end(-1, "No recordings to dump. Use jfr start to start a recording.");
            }
            if (getRecording() != null) {
                Recording r = recordings.get(getRecording());
                if (r == null) {
                    process.end(-1, "recording not exit");
                }
                if (getFilename() == null) {
                    try {
                        setFilename(outputFile());
                    } catch (IOException e) {
                        process.end(-1, e.getMessage());
                    }
                }

                try {
                    r.dump(Paths.get(getFilename()));
                } catch (IOException e) {
                    process.end(-1, "Could not to dump. " + e.getMessage());
                }
                result.setJfrOutput("Dump recording " + r.getId() + ", The result will be written to:\n" + getFilename());
            } else {
                process.end(-1, "Failed to dump. Please input recording id");
            }

        } else if ("stop".equals(cmd)) {
            if (recordings.isEmpty()) {
                process.end(-1, "No recordings to stop. Use jfr start to start a recording.");
            }
            if (getRecording() != null) {
                Recording r = recordings.remove(getRecording());
                if (r == null) {
                    process.end(-1, "recording not exit");
                }
                if ("CLOSED".equals(r.getState().toString()) || "STOPPED".equals(r.getState().toString())) {
                    process.end(-1, "Failed to stop recording, state can not be closed/stopped");
                }
                if (getFilename() == null) {
                    try {
                        setFilename(outputFile());
                    } catch (IOException e) {
                        process.end(-1, e.getMessage());
                    }
                }
                try {
                    r.setDestination(Paths.get(getFilename()));
                } catch (IOException e) {
                    process.end(-1, "Failed to stop" + r.getName() + ". Could not set destination for " + filename + "to file" + e.getMessage());
                }

                r.stop();
                result.setJfrOutput("Stop recording " + r.getId() + ", The result will be written to:\n" + getFilename());
                r.close();
            } else {
                process.end(-1, "Failed to stop. please input recording id");
            }
        } else {
            process.end(-1, "Please input correct jfr command (start status stop dump)");
        }

        process.appendResult(result);
        process.end();
    }

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
                throw new NumberFormatException("'" + s + "' is not a valid size. Shoule be numeric value followed by a unit, i.e. 20M. Valid units k, M, G");
            }
        }
    }

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

    private List<Recording> findRecordingByState(String state) {
        List<Recording> resultRecordingList = new ArrayList<Recording>();
        Collection<Recording> recordingList = recordings.values();
        for (Recording recording : recordingList) {
            if (recording.getState().toString().toLowerCase().equals(state)) {
                resultRecordingList.add(recording);
            }
        }
        return resultRecordingList;
    }

    private void printRecording(Recording recording) {
        String format = "Recording: recording=" + recording.getId() + " name=" + recording.getName() + "";
        result.setJfrOutput(format);
        Duration duration = recording.getDuration();
        if (duration != null) {
            result.setJfrOutput(" duration=" + duration.toString());
        }
        result.setJfrOutput(" (" + recording.getState().toString().toLowerCase() + ")\n");
    }

    private String outputFile() throws IOException {
        if (this.filename == null) {
            File outputPath = ArthasBootstrap.getInstance().getOutputPath();
            if (outputPath != null) {
                this.filename = new File(outputPath,
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".jfr")
                        .getAbsolutePath();
            } else {
                this.filename = File.createTempFile("arthas-output", ".jfr").getAbsolutePath();
            }
        }
        return filename;
    }

    @Override
    public void complete(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String token = tokens.get(tokens.size() - 1).value();

        if (token.startsWith("-")) {
            super.complete(completion);
            return;
        }
        List<String> cmd = Arrays.asList("start", "status", "dump", "stop");
        CompletionUtils.complete(completion, cmd);
    }
}

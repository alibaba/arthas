package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.JFRModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import jdk.jfr.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Name("jfr")
@Summary("Java Flight Command")
@Description(Constants.EXAMPLE +
        "  jfr start [-n <value>] [-s <value>][-c <value>] [--disk <value>] " +
        "[--delay <value>] [--dumponexit <value>] [--duration <value>] " +
        "[-f <value>] [-h] [--maxage <value>] [--maxsize <value>] \n" +
        "  jfr check [-n <value>] [-r <value>] [--state <value>] \n" +
        "  jfr stop [-n <value>] [-r <value>] [--discard <value>] [-f <value>] [-c <value>]\n" +
        "  jfr dump [-n <value>] [-r <value>] [-f <value>] [-c <value>]\n" +
        Constants.WIKI + Constants.WIKI_HOME + "jfr")
public class JFRCommand extends AnnotatedCommand {

    private String cmd;
    private String name;
    private String settings;
    private Boolean disk;
    private Boolean dumpOnExit = false;
    private String delay;
    private String duration;
    private String filename;
    private String compress;
    private String maxAge;
    private String maxSize;
    private Long recording;
    private String discard;
    private String state;
    private JFRModel result = new JFRModel();
    private static Map<Long, Recording> recordings = new HashMap<>();

    @Argument(index = 0, argName = "cmd", required = true)
    @Description("command name (start check stop dump)")
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Option(shortName = "n", longName = "name")
    @Description("Name that can be used to identify recording, e.g. \\\"My Recording\\\" (STRING, no default value)")
    public void setName(String name) {
        this.name = name;
    }

    @Option(shortName = "s", longName = "settings")
    @Description("Settings file(s), e.g. profile or default. See JRE_HOME/lib/jfr (STRING SET, no default value)")
    public void setSettings(String settings) {
        this.settings = settings;
    }

    @Option(longName = "disk")
    @Description("Recording should be persisted to disk (BOOLEAN, no default value)")
    public void setToDisk(Boolean disk) {
        this.disk = disk;
    }

    @Option(longName = "dumponexit")
    @Description("Dump running recording when JVM shuts down (BOOLEAN, no default value)")
    public void setDumpOnExit(Boolean dumpOnExit) {
        this.dumpOnExit = dumpOnExit;
    }

    @Option(longName = "delay")
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
    @Description("Resulting recording filename, e.g. \\\"C:\\Users\\user\\My Recording.jfr\\\" (STRING, no default value)")
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Option(shortName = "c", longName = "compress")
    @Description("GZip-compress the resulting recording file (BOOLEAN, false)")
    public void setCompress(String compress) {
        this.compress = compress;
    }

    @Option(longName = "maxage")
    @Description("Maximum time to keep recorded data (on disk) in (s)econds, (m)inutes, (h)ours, or (d)ays, e.g. 60m, or 0 for no limit (NANOTIME, 0)")
    public void setMaxAge(String maxAge) {
        this.maxAge = maxAge;
    }

    @Option(longName = "maxsize")
    @Description("Maximum amount of bytes to keep (on disk) in (k)B, (M)B or (G)B, e.g. 500M, or 0 for no limit (MEMORY SIZE, 0)")
    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    @Option(shortName = "r", longName = "recording")
    @Description("Recording number, or omit to see all recordings (JLONG, -1)")
    public void setRecording(Long recording) {
        this.recording = recording;
    }

    @Option(longName = "discard")
    @Description("Skip writing data to previously specified file (if any) (BOOLEAN, false)")
    public void setDiscard(String discard) {
        this.discard = discard;
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

    public Boolean isToDisk() {
        return disk;
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

    public String isCompress() {
        return compress;
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

    public String getDiscard() {
        return discard;
    }

    public String getState() {
        return state;
    }

    @Override
    public void process(CommandProcess process) {

        if (cmd.equals("start")) {
            Configuration c = null;
            try {
                if (getSettings() == null) {
                    setSettings("default");
                }
                c = Configuration.getConfiguration(settings);
            } catch (IOException | ParseException e) {
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

            if (isDumpOnExit() != false) {
                r.setDumpOnExit(isDumpOnExit().booleanValue());
            }

            if (getDuration() != null) {
                long l = parseTimespan(getDuration());
                r.setDuration(Duration.ofNanos(l));
            }

            if (getName() == null) {
                r.setName("Recording-" + r.getId());
            } else {
                r.setName(getName());
            }

            if (isToDisk() != null) {
                r.setToDisk(isToDisk().booleanValue());
            }

            long id = r.getId();
            recordings.put(id, r);

            if (getDelay() != null) {
                Duration dDelay = Duration.ofNanos(parseTimespan(getDelay()));
                r.scheduleStart(dDelay);
                result.setJfrOutput("Recording " + r.getId() + " scheduled to start in " + getDelay());
            } else {
                r.start();
                result.setJfrOutput("Started recording " + r.getId() + ".");
            }

            if (r.isToDisk() && duration == null && maxAge == null && maxSize == null) {
                result.setJfrOutput(" No limit specified, using maxsize=250MB as default.");
                r.setMaxSize(262144000L);
            }

            if (filename != null && duration != null) {
                result.setJfrOutput(" The result will be written to:\n" + filename);
            }
        } else if (cmd.equals("check")) {
            Long id = getRecording();
            if (id != null) {
                printRecording(recordings.get(id));
            } else {
                List<Recording> recordingList;
                if (state != null) {
                    recordingList = findRecordingByState(state);
                } else {
                    recordingList =  new ArrayList<Recording>(recordings.values());
                }
                if (recordingList.isEmpty()) {
                    process.end(-1, "No available recordings.\n Use jfr start to start a recording.\n");
                } else {
                    for (Recording recording : recordingList) {
                        printRecording(recording);
                    }
                }
            }
        } else if (cmd.equals("dump")) {
            if (recordings.isEmpty()) {
                process.end(-1,"No recordings to dump from. Use jfr start to start a recording.");
            }
            if (getRecording() != null) {
                Recording r = recordings.get(getRecording());
                if (getFilename() == null) {
                    setFilename("dump-" + r.getName() + "-" + r.getId() + ".jfr");
                }

                try {
                    r.dump(Paths.get(getFilename()));
                } catch (IOException e) {
                    process.end(-1,"Could not to dump. "+ e.getMessage());
                }
                result.setJfrOutput("Dump recording " + r.getId() + ", The result will be written to:\n" + getFilename());
            } else {
                process.end(-1,"Failed to dump " + getFilename() + " Please input recording id");
            }

        } else if (cmd.equals("stop")) {
            Recording r = recordings.remove(getRecording());
            if (getFilename() == null)
                setFilename("stop-" + r.getName() + "-" + r.getId() + ".jfr");

            try {
                r.setDestination(Paths.get(getFilename()));
            } catch (IOException e) {
                process.end(-1, "Failed to stop" + r.getName() +". Could not set destination for "+ filename+ "to file" + e.getMessage());
            }

            r.stop();
            result.setJfrOutput("Stop recording " + r.getId() + ", The result will be written to:\n" + getFilename());
            r.close();
        } else {
            process.end(-1, "Please input correct jfr command (start check stop dump)");
        }

        process.appendResult(result);
        process.end();
    }

    public long parseTimespan(String s) {
        if (s.endsWith("s")) {
            return TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else if (s.endsWith("m")) {
            return 60L * TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else if (s.endsWith("h")) {
            return 3600L * TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else if (s.endsWith("d")) {
            return 86400L * TimeUnit.NANOSECONDS.convert(Long.parseLong(s.substring(0, s.length() - 1).trim()), TimeUnit.SECONDS);
        } else {
            try {
                Long.parseLong(s);
            } catch (NumberFormatException var2) {
                throw new NumberFormatException("'" + s + "' is not a valid timespan. Shoule be numeric value followed by a unit, i.e. 20 ms. Valid units are ns, us, s, m, h and d.");
            }
            throw new NumberFormatException("Timespan + '" + s + "' is missing unit. Valid units are ns, us, s, m, h and d.");
        }
    }

    private List<Recording> findRecordingByState(String state) {
        List<Recording> resultRecordingList = new ArrayList<>();
        Collection<Recording> recordingList = recordings.values();
        for (Recording recording : recordingList) {
            if (recording.getState().toString().toLowerCase().equals(state))
                resultRecordingList.add(recording);
        }
        return resultRecordingList;
    }

    private void printRecording(Recording recording) {
        String format = "Recording: recording="+recording.getId()+" name="+recording.getName()+"";
        result.setJfrOutput(format);
        Duration duration = recording.getDuration();
        if (duration != null) {
            result.setJfrOutput(" duration="+ duration);
        }

        long maxSize = recording.getMaxSize();
        if (maxSize != 0L) {
            result.setJfrOutput(" maxsize=" + maxSize);
        }

        Duration maxAge = recording.getMaxAge();
        if (maxAge != null) {
            result.setJfrOutput(" maxage=" + maxAge);
        }
        result.setJfrOutput(" (" + recording.getState().toString().toLowerCase() + ")\n");
    }
}

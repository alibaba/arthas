package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.JFRModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import jdk.jfr.*;

import jdk.jfr.internal.jfc.JFC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Name("jfr")
@Summary("Java Flight Command")
@Description(Constants.EXAMPLE +
        "  jfr JFR.start [-n <value>] [-s <value>][-c <value>] [-df <value>] " +
        "[--delay <value>] [--discard <value>] [--dumponexit <value>] [--duration <value>] " +
        "[-f <value>] [-h] [--maxage <value>] [--maxsize <value>] \n" +
        "  jfr JFR.check [-n <value>] [-r <value>] [-v <value>]\n" +
        "  jfr JFR.stop [-n <value>] [-r <value>] [--discard <value>] [-f <value>] [-c <value>]\n" +
        "  jfr JFR.dump [-n <value>] [-r <value>] [-f <value>] [-c <value>]\n" +
        Constants.WIKI + Constants.WIKI_HOME + "jfr")
public class JFRCommand extends AnnotatedCommand {

    private String cmd;
    private String name;
    private String settings;
    private Boolean disk;
    private Boolean dumpOnExit;
    private String delay;
    private String duration;
    private String filename;
    private String compress;
    private String maxAge;
    private String maxSize;
    private Long recording;
    private String discard;
    private Boolean verbose;
    JFRModel result = new JFRModel();

    @Argument(index = 0, argName = "cmd", required = true)
    @Description("command name (JFR.start JFR.check JFR.stop JFR.dump)")
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

    @Option(shortName = "v", longName = "verbose")
    @Description("Print event settings for the recording(s) (BOOLEAN, false)")
    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
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

    public Boolean getVerbose() {
        return verbose;
    }

    @Override
    public void process(CommandProcess process) {

        if (cmd.equals("JFR.start")) {
            String[] configurations = new String[]{settings};
            Map<String, String> s = new HashMap();
            if (configurations == null || configurations.length == 0) {
                configurations = new String[]{"default"};
            }

            String[] var11 = configurations;
            int var12 = configurations.length;

            String recordingspecifier;
            for(int var13 = 0; var13 < var12; ++var13) {
                recordingspecifier = var11[var13];

                try {
                    s.putAll(JFC.createKnown(recordingspecifier).getSettings());
                } catch (ParseException | IOException var17) {
                   result.setJfrOutput("Could not parse setting " +configurations[0]+ new Object[]{var17});
                }
            }
            Recording r = new Recording();
            r.setSettings(s);

            if (getFilename() != null) {
                try {
                    r.setDestination(Paths.get(getFilename()));
                } catch (IOException e) {
                    r.close();
                    process.end(-1, "Could not start recording, not able to write to file " + getFilename() + e.getMessage());
                }
            }
            if (isDumpOnExit() != null) {
                r.setDumpOnExit(isDumpOnExit());
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
                r.setToDisk(isToDisk());
            }

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

        } else if (cmd.equals("JFR.check")) {
            String recordingText = null;
            if (name != null)
                recordingText = name;
            if (recording != null)
                recordingText = recording.toString();

            if (recordingText != null) {
                printRecording(findRecording(recordingText), false);
            } else {
                List<Recording> recordings = this.getRecordings();
                if (!verbose && recordings.isEmpty()) {
                    result.setJfrOutput("No available recordings.\n");
                    result.setJfrOutput("Use JFR.start to start a recording.");
                } else {
                    boolean first = true;
                    Iterator var5 = recordings.iterator();

                    while(var5.hasNext()) {
                        Recording recording = (Recording)var5.next();
                        if (!first) {
                            if (Boolean.TRUE.equals(verbose)) {

                            }
                        }
                        first = false;
                        printRecording(recording, verbose);
                    }
                }
            }


        } else if (cmd.equals("JFR.dump")) {
            // TODO: 2022/8/9
        } else if (cmd.equals("JFR.stop")) {
            // TODO: 2022/8/9
        }
        process.appendResult(result);
        process.end();
    }

    private String printOutput(Process proc) throws IOException {
        BufferedReader stdInput = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(
                new InputStreamReader(proc.getErrorStream()));
        // Read the output from the command
        String s = null;
        StringBuilder sc = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            sc.append(s+'\n');
        }
        // Read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
            sc.append(s);
        }
        return sc.toString();
    }

    private Recording findRecording(String name) {
        try {
            return this.findRecordingById(Integer.parseInt(name));
        } catch (NumberFormatException e) {
            return this.findRecordingByName(name);
        }
    }
    private Recording findRecordingById(int id) {
        Iterator iterator = FlightRecorder.getFlightRecorder().getRecordings().iterator();
        Recording r;
        do {
            if (!iterator.hasNext()) {
                result.setJfrOutput("Could not find " + id + "\n\nUse JFR.check without options to see list of all available recordings.\n");
                return null;
            }
            r = (Recording) iterator.next();
        } while (r.getId() != (long)id);
        return r;
    }

    private Recording findRecordingByName(String name) {
        Iterator iterator = FlightRecorder.getFlightRecorder().getRecordings().iterator();
        Recording r;
        do {
            if (!iterator.hasNext()) {
                result.setJfrOutput("Could not find " + name + "\n\nUse JFR.check without options to see list of all available recordings.\n");
                return null;
            }
            r = (Recording) iterator.next();
        } while (r.getName() != name);
        return r;
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

    private void printRecording(Recording recording, boolean verbose) {
        this.printGeneral(recording);
        if (verbose) {
            this.printSetttings(recording);
        }
    }

    private void printGeneral(Recording recording) {
        String format = "Recording: recording="+recording.getId()+" name="+recording.getName()+"";
        result.setJfrOutput(format);
        Duration duration = recording.getDuration();
        if (duration != null) {
            result.setJfrOutput(" duration=");
//            Utils.formatTimespan(duration, "");
        }

        long maxSize = recording.getMaxSize();
        if (maxSize != 0L) {
            result.setJfrOutput(" maxsize=");
//            result.setJfrOutput(maxSize);
        }

        Duration maxAge = recording.getMaxAge();
        if (maxAge != null) {
            result.setJfrOutput(" maxage=");
//            this.printTimespan(maxAge, "");
        }
        result.setJfrOutput(" (" + recording.getState().toString().toLowerCase() + ")\n");
    }

    private void printSetttings(Recording recording) {
        Map<String, String> settings = recording.getSettings();
        Iterator var3 = sortByEventPath(FlightRecorder.getFlightRecorder().getEventTypes()).iterator();

        while(var3.hasNext()) {
            EventType eventType = (EventType)var3.next();
            StringJoiner sj = new StringJoiner(",", "[", "]");
            sj.setEmptyValue("");
            Iterator var6 = eventType.getSettingDescriptors().iterator();

            while(var6.hasNext()) {
                SettingDescriptor s = (SettingDescriptor)var6.next();
                String settingsPath = eventType.getName() + "#" + s.getName();
                if (settings.containsKey(settingsPath)) {
                    sj.add(s.getName() + "=" + (String)settings.get(settingsPath));
                }
            }

            String settingsText = sj.toString();
            if (!settingsText.isEmpty()) {
//                this.print(" %s (%s)", new Object[]{eventType.getLabel(), eventType.getName()});
//                this.println();
//                result.setJfrOutput("   " + settingsText, new Object[0]);
            }
        }

    }

    private static List<EventType> sortByEventPath(Collection<EventType> events) {
        List<EventType> sorted = new ArrayList();
        sorted.addAll(events);
        Collections.sort(sorted, new Comparator<EventType>() {
            public int compare(EventType e1, EventType e2) {
                return e1.getName().compareTo(e2.getName());
            }
        });
        return sorted;
    }

    protected final List<Recording> getRecordings() {
        List<Recording> list = new ArrayList(FlightRecorder.getFlightRecorder().getRecordings());
        Collections.sort(list, (a, b) -> {
            return a.getName().compareTo(b.getName());
        });
        return list;
    }
}

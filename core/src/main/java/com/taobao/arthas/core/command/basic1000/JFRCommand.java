package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.JFRModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

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
    private String defaultRecording;
    private String dumpOnExit;
    private String delay;
    private String duration;
    private String filename;
    private String compress;
    private String maxAge;
    private String maxSize;
    private Long recording;
    private String discard;
    private String verbose;

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

    @Option(shortName = "df", longName = "defaultrecording")
    @Description("Starts the default recording, can only be combined with settings. (BOOLEAN, false)")
    public void setDefaultRecording(String defaultRecording) {
        this.defaultRecording = defaultRecording;
    }

    @Option(longName = "dumponexit")
    @Description("Dump running recording when JVM shuts down (BOOLEAN, no default value)")
    public void setDumpOnExit(String dumpOnExit) {
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
    public void setVerbose(String verbose) {
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

    public String isDefaultRecording() {
        return defaultRecording;
    }

    public String isDumpOnExit() {
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

    public String getVerbose() {
        return verbose;
    }

    @Override
    public void process(CommandProcess process) {
        JFRModel result = new JFRModel();
        String resultCmd = "jcmd " +  PidUtils.currentPid() + " " + getCmd();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (field.get(this) != null && !field.getName().equals("cmd") )
                    resultCmd += " " +  field.getName().toLowerCase() + "=" + field.get(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        try {
            Process p1 = Runtime.getRuntime().exec(resultCmd);
            result.setJfrOutput(printOutput(p1));
        } catch (IOException e) {
            e.printStackTrace();
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

}

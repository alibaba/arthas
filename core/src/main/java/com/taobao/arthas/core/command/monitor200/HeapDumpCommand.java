package com.taobao.arthas.core.command.monitor200;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.HeapDumpModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * HeapDump command
 * 
 * @author hengyunabc 2019-09-02
 *
 */
@Name("heapdump")
@Summary("Heap dump")
@Description("\nExamples:\n" + "  heapdump\n" + "  heapdump --live\n" + "  heapdump --live /tmp/dump.hprof\n"
                + Constants.WIKI + Constants.WIKI_HOME + "heapdump")
public class HeapDumpCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(HeapDumpCommand.class);
    private String file;

    private boolean live;

    @Argument(argName = "file", index = 0, required = false)
    @Description("Output file")
    public void setFile(String file) {
        this.file = file;
    }

    @Option(shortName = "l", longName = "live", flag = true)
    @Description("Dump only live objects; if not specified, all objects in the heap are dumped.")
    public void setLive(boolean live) {
        this.live = live;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            String dumpFile = file;
            if (dumpFile == null || dumpFile.isEmpty()) {
                String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
                File file = File.createTempFile("heapdump" + date + (live ? "-live" : ""), ".hprof");
                dumpFile = file.getAbsolutePath();
                file.delete();
            }

            process.appendResult(new MessageModel("Dumping heap to " + dumpFile + " ..."));

            run(process, dumpFile, live);

            process.appendResult(new MessageModel("Heap dump file created"));
            process.appendResult(new HeapDumpModel(dumpFile, live));
            process.end();
        } catch (Throwable t) {
            String errorMsg = "heap dump error: " + t.getMessage();
            logger.error(errorMsg, t);
            process.end(-1, errorMsg);
        }

    }

    private static void run(CommandProcess process, String file, boolean live) throws IOException {
        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                        .getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        hotSpotDiagnosticMXBean.dumpHeap(file, live);
    }

}

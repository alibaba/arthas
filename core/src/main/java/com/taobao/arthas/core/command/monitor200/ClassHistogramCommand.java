package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;

/**
 * ClassHistogram command
 * 
 * @author fornaix 2020-12-25
 *
 */
@Name("classhistogram")
@Summary("Show class histogram")
@Description(Constants.EXAMPLE +
        "  classhistogram\n" +
        "  classhistogram --live\n" +
        "  classhistogram -n 10\n" +
        "\nRequirements: JDK 8 or higher")
public class ClassHistogramCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(ClassHistogramCommand.class);

    private int numberOfLimit = -1;
    private boolean live;

    @Option(shortName = "l", longName = "live", flag = true)
    @Description("Inspect only live objects; if not specified, all objects are inspected, including unreachable objects.")
    public void setLive(boolean live) {
        this.live = live;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Maximum number of class statistics (all by default)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            String histogram = run(live, numberOfLimit);
            process.appendResult(new MessageModel(histogram));
            process.end();
        } catch (Throwable t) {
            String errorMsg = "Failed to get class histogram: " + t.getMessage();
            logger.error(errorMsg, t);
            process.end(-1, errorMsg);
        }
    }

    protected static String run(boolean live, int limit) throws MalformedObjectNameException, MBeanException,
            InstanceNotFoundException, ReflectionException, IOException {
        String[] args = live ? null : new String[]{"-all"};
        ObjectName mbeanName = new ObjectName("com.sun.management:type=DiagnosticCommand");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        // "gcClassHistogram" returns a single String
        String histogram = (String) mBeanServer.invoke(mbeanName, "gcClassHistogram", new Object[]{args},
                new String[]{String[].class.getName()});
        return truncateHistogram(histogram, limit);
    }

    private static String truncateHistogram(String histogram, int limit) throws IOException {
        if (limit < 0) {
            return histogram;
        }

        BufferedReader br = new BufferedReader(new StringReader(histogram));
        StringBuilder builder = new StringBuilder();
        // append header
        builder.append(br.readLine());
        builder.append("\n");
        builder.append(br.readLine());
        builder.append("\n");
        builder.append(br.readLine());

        int num = 0;
        String line, lastLine = null;
        while ((line = br.readLine()) != null) {
            if (num < limit) {
                builder.append("\n");
                builder.append(line);
            }
            lastLine = line;
            num++;
        }
        if (num > limit) {
            // append ellipsis
            if (num > limit + 1) {
                builder.append("\n");
                builder.append(" ...");
            }
            builder.append("\n");
            builder.append(lastLine);
        }
        return builder.toString();
    }

}

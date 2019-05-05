package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.TokenUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.Descriptor;
import javax.management.DescriptorRead;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static com.taobao.text.ui.Element.label;
import static javax.management.MBeanOperationInfo.ACTION;
import static javax.management.MBeanOperationInfo.ACTION_INFO;
import static javax.management.MBeanOperationInfo.INFO;
import static javax.management.MBeanOperationInfo.UNKNOWN;

/**
 * Date: 2019/4/18
 *
 * @author xuzhiyi
 */
@Name("mbean")
@Summary("Display the mbean information")
@Description("\nExamples:\n" +
             "  mbean\n" +
             "  mbean -m java.lang:type=Threading\n" +
             "  mbean java.lang:type=Threading\n" +
             "  mbean java.lang:type=Threading *Count\n" +
             "  mbean -E java.lang:type=Threading PeakThreadCount|ThreadCount|DaemonThreadCount\n" +
             "  mbean -i 1000 java.lang:type=Threading *Count\n" +
             Constants.WIKI + Constants.WIKI_HOME + "mbean")
public class MBeanCommand extends AnnotatedCommand {

    private static final Logger logger = LogUtil.getArthasLogger();

    private String name;
    private String attribute;
    private boolean isRegEx = false;
    private long interval = 0;
    private boolean metaData;
    private int numOfExecutions = 100;
    private Timer timer;
    private long count = 0;

    @Argument(argName = "name-pattern", index = 0, required = false)
    @Description("ObjectName pattern, see javax.management.ObjectName for more detail.")
    public void setNamePattern(String name) {
        this.name = name;
    }

    @Argument(argName = "attribute-pattern", index = 1, required = false)
    @Description("Attribute name pattern.")
    public void setAttributePattern(String attribute) {
        this.attribute = attribute;
    }

    @Option(shortName = "i", longName = "interval")
    @Description("The interval (in ms) between two executions.")
    public void setInterval(long interval) {
        this.interval = interval;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match attribute name (wildcard matching by default).")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "m", longName = "metadata", flag = false)
    @Description("Show metadata of mbean.")
    public void setMetaData(boolean metaData) {
        this.metaData = metaData;
    }

    @Option(shortName = "n", longName = "number-of-execution")
    @Description("The number of times this command will be executed.")
    public void setNumOfExecutions(int numOfExecutions) {
        this.numOfExecutions = numOfExecutions;
    }

    public String getName() {
        return name;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public boolean isMetaData() {
        return metaData;
    }

    public long getInterval() {
        return interval;
    }

    public int getNumOfExecutions() {
        return numOfExecutions;
    }

    @Override
    public void process(CommandProcess process) {
        if (StringUtils.isEmpty(getName())) {
            listMBean(process);
        } else if (isMetaData()) {
            listMetaData(process);
        } else {
            listAttribute(process);
        }
    }

    private void listMBean(CommandProcess process) {
        Set<ObjectName> objectNames = queryObjectNames();
        for (ObjectName objectName : objectNames) {
            process.write(objectName.toString());
            process.write("\n");
        }
        process.end();
    }

    private void listAttribute(final CommandProcess process) {
        Session session = process.session();
        timer = new Timer("Timer-for-arthas-mbean-" + session.getSessionId(), true);

        // ctrl-C support
        process.interruptHandler(new MBeanInterruptHandler(process, timer));

        // 通过handle回调，在suspend和end时停止timer，resume时重启timer
        Handler<Void> stopHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                stop();
            }
        };

        Handler<Void> restartHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                restart(process);
            }
        };
        process.suspendHandler(stopHandler);
        process.resumeHandler(restartHandler);
        process.endHandler(stopHandler);
        // q exit support
        process.stdinHandler(new QExitHandler(process));

        // start the timer
        if (getInterval() > 0) {
            timer.scheduleAtFixedRate(new MBeanTimerTask(process), 0, getInterval());
        } else {
            timer.schedule(new MBeanTimerTask(process), 0);
        }
    }

    public synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public synchronized void restart(CommandProcess process) {
        if (timer == null) {
            Session session = process.session();
            timer = new Timer("Timer-for-arthas-mbean-" + session.getSessionId(), true);
            timer.scheduleAtFixedRate(new MBeanTimerTask(process), 0, getInterval());
        }
    }

    private void listMetaData(CommandProcess process) {
        Set<ObjectName> objectNames = queryObjectNames();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            TableElement table = createTable();
            for (ObjectName objectName : objectNames) {
                MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(objectName);
                drawMetaInfo(mBeanInfo, objectName, table);
                drawAttributeInfo(mBeanInfo.getAttributes(), table);
                drawOperationInfo(mBeanInfo.getOperations(), table);
                drawNotificationInfo(mBeanInfo.getNotifications(), table);
            }
            process.write(RenderUtil.render(table, process.width()));
        } catch (Throwable e) {
            logger.warn("listMetaData error", e);
        } finally {
            process.end();
        }
    }

    @Override
    public void complete(Completion completion) {
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);
        if (argumentIndex == 1) {
            if (!completeBeanName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 2) {
            if (!completeAttributeName(completion)) {
                super.complete(completion);
            }
            return;
        }
        super.complete(completion);
    }

    private boolean completeBeanName(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String lastToken = TokenUtils.getLast(tokens).value();

        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        if (lastToken.startsWith("-") || lastToken.startsWith("--")) {
            return false;
        }

        Set<ObjectName> objectNames = queryObjectNames();
        Set<String> names = new HashSet<String>();

        if (objectNames == null) {
            return false;
        }
        for (ObjectName objectName : objectNames) {
            String name = objectName.toString();
            if (name.startsWith(lastToken)) {
                int index = name.indexOf('.', lastToken.length());
                if (index > 0) {
                    names.add(name.substring(0, index + 1));
                    continue;
                }
                index = name.indexOf(':', lastToken.length());
                if (index > 0) {
                    names.add(name.substring(0, index + 1));
                    continue;
                }
                names.add(name);
            }
        }
        String next = names.iterator().next();
        if (names.size() == 1 && (next.endsWith(".") || next.endsWith(":"))) {
            completion.complete(next.substring(lastToken.length()), false);
            return true;
        } else {
            return CompletionUtils.complete(completion, names);
        }
    }

    private boolean completeAttributeName(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String lastToken = TokenUtils.getLast(tokens).value();

        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        String beanName = TokenUtils.retrievePreviousArg(tokens, lastToken);
        Set<ObjectName> objectNames = null;
        try {
            objectNames = platformMBeanServer.queryNames(new ObjectName(beanName), null);
        } catch (MalformedObjectNameException e) {
            logger.warn("queryNames error", e);
        }
        if (objectNames == null || objectNames.size() == 0) {
            return false;
        }
        try {
            MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(objectNames.iterator().next());
            List<String> attributeNames = new ArrayList<String>();
            MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
            for (MBeanAttributeInfo attribute : attributes) {
                if (StringUtils.isBlank(lastToken)) {
                    attributeNames.add(attribute.getName());
                } else if (attribute.getName().startsWith(lastToken)) {
                    attributeNames.add(attribute.getName());
                }
            }
            return CompletionUtils.complete(completion, attributeNames);
        } catch (Throwable e) {
            logger.warn("getMBeanInfo error", e);
        }
        return false;
    }

    private Set<ObjectName> queryObjectNames() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objectNames = new HashSet<ObjectName>();
        try {
            if (StringUtils.isEmpty(name)) {
                name = "*:*";
            }
            objectNames = platformMBeanServer.queryNames(new ObjectName(name), null);
        } catch (MalformedObjectNameException e) {
            logger.warn("queryObjectNames error", e);
        }
        return objectNames;
    }

    private Matcher<String> getAttributeMatcher() {
        if (StringUtils.isEmpty(attribute)) {
            attribute = isRegEx ? ".*" : "*";
        }
        return isRegEx ? new RegexMatcher(attribute) : new WildcardMatcher(attribute);
    }

    private void drawMetaInfo(MBeanInfo mBeanInfo, ObjectName objectName, TableElement table) {
        table.row(new LabelElement("MBeanInfo").style(Decoration.bold.fg(Color.red)));
        table.row(new LabelElement("Info:").style(Decoration.bold.fg(Color.yellow)));
        table.row("ObjectName", objectName.toString());
        table.row("ClassName", mBeanInfo.getClassName());
        table.row("Description", mBeanInfo.getDescription());
        drawDescriptorInfo("Info Descriptor:", mBeanInfo, table);
        MBeanConstructorInfo[] constructors = mBeanInfo.getConstructors();
        if (constructors.length > 0) {
            for (int i = 0; i < constructors.length; i++) {
                table.row(new LabelElement("Constructor-" + i).style(Decoration.bold.fg(Color.yellow)));
                table.row("Name", constructors[i].getName());
                table.row("Description", constructors[i].getDescription());
            }
        }
    }

    private void drawAttributeInfo(MBeanAttributeInfo[] attributes, TableElement table) {
        for (MBeanAttributeInfo attribute : attributes) {
            table.row(new LabelElement("MBeanAttributeInfo").style(Decoration.bold.fg(Color.red)));
            table.row(new LabelElement("Attribute:").style(Decoration.bold.fg(Color.yellow)));
            table.row("Name", attribute.getName());
            table.row("Description", attribute.getDescription());
            table.row("Readable", String.valueOf(attribute.isReadable()));
            table.row("Writable", String.valueOf(attribute.isWritable()));
            table.row("Is", String.valueOf(attribute.isIs()));
            table.row("Type", attribute.getType());
            drawDescriptorInfo("Attribute Descriptor:", attribute, table);
        }
    }

    private void drawOperationInfo(MBeanOperationInfo[] operations, TableElement table) {
        for (MBeanOperationInfo operation : operations) {
            table.row(new LabelElement("MBeanOperationInfo").style(Decoration.bold.fg(Color.red)));
            table.row(new LabelElement("Operation:").style(Decoration.bold.fg(Color.yellow)));
            table.row("Name", operation.getName());
            table.row("Description", operation.getDescription());
            String impact = "";
            switch (operation.getImpact()) {
                case ACTION:
                    impact = "action";
                    break;
                case ACTION_INFO:
                    impact = "action/info";
                    break;
                case INFO:
                    impact = "info";
                    break;
                case UNKNOWN:
                    impact = "unknown";
                    break;
            }
            table.row("Impact", impact);
            table.row("ReturnType", operation.getReturnType());
            MBeanParameterInfo[] signature = operation.getSignature();
            if (signature.length > 0) {
                for (int i = 0; i < signature.length; i++) {
                    table.row(new LabelElement("Parameter-" + i).style(Decoration.bold.fg(Color.yellow)));
                    table.row("Name", signature[i].getName());
                    table.row("Type", signature[i].getType());
                    table.row("Description", signature[i].getDescription());
                }
            }
            drawDescriptorInfo("Operation Descriptor:", operation, table);
        }
    }

    private void drawNotificationInfo(MBeanNotificationInfo[] notificationInfos, TableElement table) {
        for (MBeanNotificationInfo notificationInfo : notificationInfos) {
            table.row(new LabelElement("MBeanNotificationInfo").style(Decoration.bold.fg(Color.red)));
            table.row(new LabelElement("Notification:").style(Decoration.bold.fg(Color.yellow)));
            table.row("Name", notificationInfo.getName());
            table.row("Description", notificationInfo.getDescription());
            table.row("NotifTypes", Arrays.toString(notificationInfo.getNotifTypes()));
            drawDescriptorInfo("Notification Descriptor:", notificationInfo, table);
        }
    }

    private void drawDescriptorInfo(String title, DescriptorRead descriptorRead, TableElement table) {
        Descriptor descriptor = descriptorRead.getDescriptor();
        String[] fieldNames = descriptor.getFieldNames();
        if (fieldNames.length > 0) {
            table.row(new LabelElement(title).style(Decoration.bold.fg(Color.yellow)));
            for (String fieldName : fieldNames) {
                Object fieldValue = descriptor.getFieldValue(fieldName);
                table.row(fieldName, fieldValue.toString());
            }
        }
    }

    private static TableElement createTable() {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                  label("VALUE").style(Decoration.bold.bold()));
        return table;
    }

    public class MBeanInterruptHandler extends CommandInterruptHandler {

        private volatile Timer timer;

        public MBeanInterruptHandler(CommandProcess process, Timer timer) {
            super(process);
            this.timer = timer;
        }

        @Override
        public void handle(Void event) {
            timer.cancel();
            super.handle(event);
        }
    }

    private class MBeanTimerTask extends TimerTask {

        private CommandProcess process;

        public MBeanTimerTask(CommandProcess process) {
            this.process = process;
        }

        @Override
        public void run() {
            if (count >= getNumOfExecutions()) {
                // stop the timer
                timer.cancel();
                timer.purge();
                process.write("Process ends after " + getNumOfExecutions() + " time(s).\n");
                process.end();
                return;
            }

            try {
                MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
                Set<ObjectName> objectNames = queryObjectNames();
                TableElement table = createTable();
                for (ObjectName objectName : objectNames) {
                    MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(objectName);
                    MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
                    for (MBeanAttributeInfo attribute : attributes) {
                        String attributeName = attribute.getName();
                        if (!getAttributeMatcher().matching(attributeName)) {
                            continue;
                        }
                        String value;
                        if (!attribute.isReadable()) {
                            value = RenderUtil.render(new LabelElement("Unavailable").style(Decoration.bold_off.fg(Color.red)));
                        } else {
                            Object attributeObj = platformMBeanServer.getAttribute(objectName, attributeName);
                            value = String.valueOf(attributeObj);
                        }
                        table.row(attributeName, value);
                    }
                    process.write(RenderUtil.render(table, process.width()));
                    process.write("\n");
                }
            } catch (Throwable e) {
                logger.warn("mbean error", e);
            }

            count++;
            process.times().incrementAndGet();

            if (getInterval() <= 0) {
                stop();
                process.end();
            }
        }
    }
}
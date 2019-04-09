package com.taobao.arthas.core.command.monitor200;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.NetUtils;
import com.taobao.arthas.core.util.NetUtils.Response;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.arthas.core.util.metrics.SumRateCounter;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.renderers.ThreadRenderer;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author hengyunabc 2015年11月19日 上午11:57:21
 */
@Name("dashboard")
@Summary("Overview of target jvm's thread, memory, gc, vm, tomcat info.")
@Description(Constants.EXAMPLE +
        "  dashboard\n" +
        "  dashboard -n 10\n" +
        "  dashboard -i 2000\n" +
        Constants.WIKI + Constants.WIKI_HOME + "dashboard")
public class DashboardCommand extends AnnotatedCommand {

    private static final Logger logger = LogUtil.getArthasLogger();

    private SumRateCounter tomcatRequestCounter = new SumRateCounter();
    private SumRateCounter tomcatErrorCounter = new SumRateCounter();
    private SumRateCounter tomcatReceivedBytesCounter = new SumRateCounter();
    private SumRateCounter tomcatSentBytesCounter = new SumRateCounter();

    private int numOfExecutions = Integer.MAX_VALUE;

    private boolean batchMode;

    private long interval = 5000;

    private volatile long count = 0;
    private volatile Timer timer;

    @Option(shortName = "n", longName = "number-of-execution")
    @Description("The number of times this command will be executed.")
    public void setNumOfExecutions(int numOfExecutions) {
        this.numOfExecutions = numOfExecutions;
    }

    @Option(shortName = "b", longName = "batch")
    @Description("Execute this command in batch mode.")
    public void setBatchMode(boolean batchMode) {
        this.batchMode = batchMode;
    }

    @Option(shortName = "i", longName = "interval")
    @Description("The interval (in ms) between two executions, default is 5000 ms.")
    public void setInterval(long interval) {
        this.interval = interval;
    }


    @Override
    public void process(final CommandProcess process) {

        Session session = process.session();
        timer = new Timer("Timer-for-arthas-dashboard-" + session.getSessionId(), true);

        // ctrl-C support
        process.interruptHandler(new DashboardInterruptHandler(process, timer));

        /*
         * 通过handle回调，在suspend和end时停止timer，resume时重启timer
         */
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
        timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval());
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
            timer = new Timer("Timer-for-arthas-dashboard-" + session.getSessionId(), true);
            timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval());
        }
    }

    public int getNumOfExecutions() {
        return numOfExecutions;
    }

    public boolean isBatchMode() {
        return batchMode;
    }

    public long getInterval() {
        return interval;
    }

    private static String beautifyName(String name) {
        return name.replace(' ', '_').toLowerCase();
    }

    private static void addBufferPoolMemoryInfo(TableElement table) {
        try {
            @SuppressWarnings("rawtypes")
            Class bufferPoolMXBeanClass = Class.forName("java.lang.management.BufferPoolMXBean");
            @SuppressWarnings("unchecked")
            List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(bufferPoolMXBeanClass);
            for (BufferPoolMXBean mbean : bufferPoolMXBeans) {
                long used = mbean.getMemoryUsed();
                long total = mbean.getTotalCapacity();
                new MemoryEntry(mbean.getName(), used, total, Long.MIN_VALUE).addTableRow(table);
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    private static void addRuntimeInfo(TableElement table) {
        table.row("os.name", System.getProperty("os.name"));
        table.row("os.version", System.getProperty("os.version"));
        table.row("java.version", System.getProperty("java.version"));
        table.row("java.home", System.getProperty("java.home"));
        table.row("systemload.average",
                String.format("%.2f", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()));
        table.row("processors", "" + Runtime.getRuntime().availableProcessors());
        table.row("uptime", "" + ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + "s");
    }

    private static void addMemoryInfo(TableElement table) {
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        new MemoryEntry("heap", heapMemoryUsage).addTableRow(table, Decoration.bold.bold());
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = poolMXBean.getUsage();
                String poolName = beautifyName(poolMXBean.getName());
                new MemoryEntry(poolName, usage).addTableRow(table);
            }
        }

        new MemoryEntry("nonheap", nonHeapMemoryUsage).addTableRow(table, Decoration.bold.bold());
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.NON_HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = poolMXBean.getUsage();
                String poolName = beautifyName(poolMXBean.getName());
                new MemoryEntry(poolName, usage).addTableRow(table);
            }
        }

        addBufferPoolMemoryInfo(table);
    }

    private static void addGcInfo(TableElement table) {
        List<GarbageCollectorMXBean> garbageCollectorMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMxBeans) {
            String name = garbageCollectorMXBean.getName();
            table.add(new RowElement().style(Decoration.bold.bold()).add("gc." + beautifyName(name) + ".count",
                    "" + garbageCollectorMXBean.getCollectionCount()));
            table.row("gc." + beautifyName(name) + ".time(ms)", "" + garbageCollectorMXBean.getCollectionTime());
        }
    }

    private static String formatBytes(long size) {
        int unit = 1;
        String unitStr = "B";
        if (size / 1024 > 0) {
            unit = 1024;
            unitStr = "K";
        } else if (size / 1024 / 1024 > 0) {
            unit = 1024 * 1024;
            unitStr = "M";
        }

        return String.format("%d%s", size / unit, unitStr);
    }

    private void addTomcatInfo(TableElement table) {

        String threadPoolPath = "http://localhost:8006/connector/threadpool";
        String connectorStatPath = "http://localhost:8006/connector/stats";
        Response connectorStatResponse = NetUtils.request(connectorStatPath);
        if (connectorStatResponse.isSuccess()) {
            List<JSONObject> connectorStats = JSON.parseArray(connectorStatResponse.getContent(), JSONObject.class);
            for (JSONObject stat : connectorStats) {
                String name = stat.getString("name").replace("\"", "");
                long bytesReceived = stat.getLongValue("bytesReceived");
                long bytesSent = stat.getLongValue("bytesSent");
                long processingTime = stat.getLongValue("processingTime");
                long requestCount = stat.getLongValue("requestCount");
                long errorCount = stat.getLongValue("errorCount");

                tomcatRequestCounter.update(requestCount);
                tomcatErrorCounter.update(errorCount);
                tomcatReceivedBytesCounter.update(bytesReceived);
                tomcatSentBytesCounter.update(bytesSent);

                table.add(new RowElement().style(Decoration.bold.bold()).add("connector", name));
                table.row("QPS", String.format("%.2f", tomcatRequestCounter.rate()));
                table.row("RT(ms)", String.format("%.2f", processingTime / (double) requestCount));
                table.row("error/s", String.format("%.2f", tomcatErrorCounter.rate()));
                table.row("received/s", formatBytes((long) tomcatReceivedBytesCounter.rate()));
                table.row("sent/s", formatBytes((long) tomcatSentBytesCounter.rate()));
            }
        }

        Response threadPoolResponse = NetUtils.request(threadPoolPath);
        if (threadPoolResponse.isSuccess()) {
            List<JSONObject> threadPoolInfos = JSON.parseArray(threadPoolResponse.getContent(), JSONObject.class);
            for (JSONObject info : threadPoolInfos) {
                String name = info.getString("name").replace("\"", "");
                long busy = info.getLongValue("threadBusy");
                long total = info.getLongValue("threadCount");
                table.add(new RowElement().style(Decoration.bold.bold()).add("threadpool", name));
                table.row("busy", "" + busy);
                table.row("total", "" + total);
            }
        }
    }

    static String drawThreadInfo(int width, int height) {
        Map<String, Thread> threads = ThreadUtil.getThreads();
        return RenderUtil.render(threads.values().iterator(), new ThreadRenderer(), width, height);
    }

    static String drawMemoryInfoAndGcInfo(int width, int height) {
        TableElement table = new TableElement(1, 1);

        TableElement memoryInfoTable = new TableElement(3, 1, 1, 1, 1).rightCellPadding(1);
        memoryInfoTable.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Memory",
                "used", "total", "max", "usage"));

        addMemoryInfo(memoryInfoTable);

        TableElement gcInfoTable = new TableElement(1, 1).rightCellPadding(1);
        gcInfoTable.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("GC", ""));
        addGcInfo(gcInfoTable);

        table.row(memoryInfoTable, gcInfoTable);
        return RenderUtil.render(table, width, height);
    }

    String drawRuntineInfoAndTomcatInfo(int width, int height) {
        TableElement table = new TableElement(1, 1);

        TableElement runtimeInfoTable = new TableElement(1, 1).rightCellPadding(1);
        runtimeInfoTable
                .add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Runtime", ""));

        addRuntimeInfo(runtimeInfoTable);

        TableElement tomcatInfoTable = new TableElement(1, 1).rightCellPadding(1);

        try {
            // 如果请求tomcat信息失败，则不显示tomcat信息
            if (NetUtils.request("http://localhost:8006").isSuccess()) {
                tomcatInfoTable
                        .add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Tomcat", ""));
                addTomcatInfo(tomcatInfoTable);
            }
        } catch (Throwable t) {
            logger.error(null, "get Tomcat Info error!", t);
        }

        table.row(runtimeInfoTable, tomcatInfoTable);
        return RenderUtil.render(table, width, height);
    }

    static class MemoryEntry {
        String name;
        long used;
        long total;
        long max;

        int unit;
        String unitStr;

        public MemoryEntry(String name, long used, long total, long max) {
            this.name = name;
            this.used = used;
            this.total = total;
            this.max = max;

            unitStr = "K";
            unit = 1024;
            if (used / 1024 / 1024 > 0) {
                unitStr = "M";
                unit = 1024 * 1024;
            }
        }

        public MemoryEntry(String name, MemoryUsage usage) {
            this(name, usage.getUsed(), usage.getCommitted(), usage.getMax());
        }

        private String format(long value) {
            String valueStr = "-";
            if (value == -1) {
                return "-1";
            }
            if (value != Long.MIN_VALUE) {
                valueStr = value / unit + unitStr;
            }
            return valueStr;
        }

        public void addTableRow(TableElement table) {
            double usage = used / (double) (max == -1 || max == Long.MIN_VALUE ? total : max) * 100;

            table.row(name, format(used), format(total), format(max), String.format("%.2f%%", usage));
        }

        public void addTableRow(TableElement table, Style.Composite style) {
            double usage = used / (double) (max == -1 || max == Long.MIN_VALUE ? total : max) * 100;

            table.add(new RowElement().style(style).add(name, format(used), format(total), format(max),
                    String.format("%.2f%%", usage)));
        }
    }

    private class DashboardTimerTask extends TimerTask {
        private CommandProcess process;

        public DashboardTimerTask(CommandProcess process) {
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

            int width = process.width();
            int height = process.height();

            // 上半部分放thread top。下半部分再切分为田字格，其中上面两格放memory, gc的信息。下面两格放tomcat,
            // runtime的信息
            int totalHeight = height - 1;
            int threadTopHeight = totalHeight / 2;
            int lowerHalf = totalHeight - threadTopHeight;

            int runtimeInfoHeight = lowerHalf / 2;
            int heapInfoHeight = lowerHalf - runtimeInfoHeight;

            String threadInfo = drawThreadInfo(width, threadTopHeight);
            String memoryAndGc = drawMemoryInfoAndGcInfo(width, runtimeInfoHeight);
            String runTimeAndTomcat = drawRuntineInfoAndTomcatInfo(width, heapInfoHeight);

            process.write(threadInfo + memoryAndGc + runTimeAndTomcat);

            count++;
            process.times().incrementAndGet();
        }
    }
}

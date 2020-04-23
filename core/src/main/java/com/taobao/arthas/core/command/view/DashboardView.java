package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.Overflow;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.lang.management.MemoryUsage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author gongdewei 2020/4/22
 */
public class DashboardView extends ResultView<DashboardModel> {

    /** . */
    private static final EnumMap<Thread.State, Color> colorMapping = new EnumMap<Thread.State, Color>(Thread.State.class);

    static {
        colorMapping.put(Thread.State.NEW, Color.cyan);
        colorMapping.put(Thread.State.RUNNABLE, Color.green);
        colorMapping.put(Thread.State.BLOCKED, Color.red);
        colorMapping.put(Thread.State.WAITING, Color.yellow);
        colorMapping.put(Thread.State.TIMED_WAITING, Color.magenta);
        colorMapping.put(Thread.State.TERMINATED, Color.blue);
    }

    @Override
    public void draw(CommandProcess process, DashboardModel result) {
        int width = process.width();
        int height = process.height();

        // 上半部分放thread top。下半部分再切分为田字格，其中上面两格放memory, gc的信息。下面两格放tomcat,
        // runtime的信息
        int totalHeight = height - 1;
        int threadTopHeight = totalHeight / 2;
        int lowerHalf = totalHeight - threadTopHeight;

        int runtimeInfoHeight = lowerHalf / 2;
        int heapInfoHeight = lowerHalf - runtimeInfoHeight;

        String threadInfo = drawThreadInfo(result.getThreads(), width, threadTopHeight);
        String memoryAndGc = drawMemoryInfoAndGcInfo(result.getMemoryInfo(), result.getGcInfos(), width, runtimeInfoHeight);
        String runTimeAndTomcat = drawRuntimeInfoAndTomcatInfo(result.getRuntimeInfo(), result.getTomcatInfo(), width, heapInfoHeight);

        process.write(threadInfo + memoryAndGc + runTimeAndTomcat);
    }

    static String drawThreadInfo(List<ThreadVO> threads, int width, int height) {
        TableElement table = new TableElement(1,3,2,1,1,1,1,1,1).overflow(Overflow.HIDDEN).rightCellPadding(1);

        // Header
        table.add(
                new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add(
                        "ID",
                        "NAME",
                        "GROUP",
                        "PRIORITY",
                        "STATE",
                        "%CPU",
                        "TIME",
                        "INTERRUPTED",
                        "DAEMON"
                )
        );

        for (ThreadVO thread : threads) {
            Color color = colorMapping.get(thread.getState());
            long seconds = thread.getTime();
            long min = seconds / 60;
            String time = min + ":" + (seconds % 60);
            long cpu = thread.getCpu();

            LabelElement daemonLabel = new LabelElement(thread.isDaemon());
            if (!thread.isDaemon()) {
                daemonLabel.setStyle(Style.style(Color.magenta));
            }
            table.row(
                    new LabelElement(thread.getId()),
                    new LabelElement(thread.getName()),
                    new LabelElement(thread.getGroup()),
                    new LabelElement(thread.getPriority()),
                    new LabelElement(thread.getState()).style(color.fg()),
                    new LabelElement(cpu),
                    new LabelElement(time),
                    new LabelElement(thread.isInterrupted()),
                    daemonLabel
            );
        }
        return RenderUtil.render(table, width, height);
    }

    static String drawMemoryInfoAndGcInfo(Map<String, List<MemoryEntryVO>> memoryInfo, List<GcInfoVO> gcInfos, int width, int height) {
        TableElement table = new TableElement(1, 1);

        TableElement memoryInfoTable = new TableElement(3, 1, 1, 1, 1).rightCellPadding(1);
        memoryInfoTable.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Memory",
                "used", "total", "max", "usage"));

        drawMemoryInfo(memoryInfoTable, memoryInfo);

        TableElement gcInfoTable = new TableElement(1, 1).rightCellPadding(1);
        gcInfoTable.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("GC", ""));
        drawGcInfo(gcInfoTable, gcInfos);

        table.row(memoryInfoTable, gcInfoTable);
        return RenderUtil.render(table, width, height);
    }

    private static void drawMemoryInfo(TableElement table, Map<String, List<MemoryEntryVO>> memoryInfo) {
        List<MemoryEntryVO> heapMemoryEntries = memoryInfo.get(MemoryEntryVO.TYPE_HEAP);
        //heap memory
        for (MemoryEntryVO memoryEntryVO : heapMemoryEntries) {
            if (MemoryEntryVO.TYPE_HEAP.equals(memoryEntryVO.getName())) {
                new MemoryEntry(memoryEntryVO).addTableRow(table, Decoration.bold.bold());
            } else {
                new MemoryEntry(memoryEntryVO).addTableRow(table);
            }
        }

        //non-heap memory
        List<MemoryEntryVO> nonheapMemoryEntries = memoryInfo.get(MemoryEntryVO.TYPE_NON_HEAP);
        for (MemoryEntryVO memoryEntryVO : nonheapMemoryEntries) {
            if (MemoryEntryVO.TYPE_NON_HEAP.equals(memoryEntryVO.getName())) {
                new MemoryEntry(memoryEntryVO).addTableRow(table, Decoration.bold.bold());
            } else {
                new MemoryEntry(memoryEntryVO).addTableRow(table);
            }
        }

        //buffer-pool
        List<MemoryEntryVO> bufferPoolMemoryEntries = memoryInfo.get(MemoryEntryVO.TYPE_BUFFER_POOL);
        for (MemoryEntryVO memoryEntryVO : bufferPoolMemoryEntries) {
            new MemoryEntry(memoryEntryVO).addTableRow(table);
        }
    }

    private static void drawGcInfo(TableElement table, List<GcInfoVO> gcInfos) {
        for (GcInfoVO gcInfo : gcInfos) {
            table.add(new RowElement().style(Decoration.bold.bold()).add("gc." + gcInfo.getName() + ".count",
                    "" + gcInfo.getCollectionCount()));
            table.row("gc." + gcInfo.getName() + ".time(ms)", "" + gcInfo.getCollectionTime());
        }
    }

    String drawRuntimeInfoAndTomcatInfo(RuntimeInfoVO runtimeInfo, TomcatInfoVO tomcatInfo, int width, int height) {
        TableElement resultTable = new TableElement(1, 1);
        //runtime
        TableElement runtimeInfoTable = drawRuntimeInfo(runtimeInfo);
        //tomcat
        TableElement tomcatInfoTable = drawTomcatInfo(tomcatInfo);

        if (tomcatInfoTable != null) {
            resultTable.row(runtimeInfoTable, tomcatInfoTable);
        } else {
            resultTable = runtimeInfoTable;
        }
        return RenderUtil.render(resultTable, width, height);
    }

    private static TableElement drawRuntimeInfo(RuntimeInfoVO runtimeInfo) {
        TableElement table = new TableElement(1, 1).rightCellPadding(1);
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Runtime", ""));

        table.row("os.name", runtimeInfo.getOsName());
        table.row("os.version", runtimeInfo.getOsVersion());
        table.row("java.version", runtimeInfo.getJavaVersion());
        table.row("java.home", runtimeInfo.getJavaHome());
        table.row("systemload.average", String.format("%.2f", runtimeInfo.getSystemLoadAverage()));
        table.row("processors", "" + runtimeInfo.getProcessors());
        table.row("uptime", "" + runtimeInfo.getUptime() + "s");
        return table;
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

    private TableElement drawTomcatInfo(TomcatInfoVO tomcatInfo) {
        if (tomcatInfo == null) {
            return null;
        }

        //header
        TableElement table = new TableElement(1, 1).rightCellPadding(1);
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Tomcat", ""));

        if (tomcatInfo.getConnectorStats() != null) {
            for (TomcatInfoVO.ConnectorStats connectorStat : tomcatInfo.getConnectorStats()) {
                table.add(new RowElement().style(Decoration.bold.bold()).add("connector", connectorStat.getName()));
                table.row("QPS", String.format("%.2f", connectorStat.getQps()));
                table.row("RT(ms)", String.format("%.2f", connectorStat.getRt()));
                table.row("error/s", String.format("%.2f", connectorStat.getError()));
                table.row("received/s", formatBytes(connectorStat.getReceived()));
                table.row("sent/s", formatBytes(connectorStat.getSent()));
            }
        }

        if (tomcatInfo.getThreadPools() != null) {
            for (TomcatInfoVO.ThreadPool threadPool : tomcatInfo.getThreadPools()) {
                table.add(new RowElement().style(Decoration.bold.bold()).add("threadpool", threadPool.getName()));
                table.row("busy", "" + threadPool.getBusy());
                table.row("total", "" + threadPool.getTotal());
            }
        }
        return table;
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

        public MemoryEntry(MemoryEntryVO memoryEntryVO) {
            this(memoryEntryVO.getName(), memoryEntryVO.getUsed(), memoryEntryVO.getTotal(), memoryEntryVO.getMax());
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
}

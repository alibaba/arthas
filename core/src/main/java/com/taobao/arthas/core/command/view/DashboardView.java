package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * View of 'dashboard' command
 *
 * @author gongdewei 2020/4/22
 */
public class DashboardView extends ResultView<DashboardModel> {

    @Override
    public void draw(CommandProcess process, DashboardModel result) {
        int width = process.width();
        int height = process.height();

        // 上半部分放thread top。下半部分再切分为田字格，其中上面两格放memory, gc的信息。下面两格放tomcat,
        // runtime的信息
        int totalHeight = height - 1;
        int threadTopHeight;
        if (totalHeight <= 24) {
            //总高度较小时取1/2
            threadTopHeight = totalHeight / 2;
        } else {
            //总高度较大时取1/3，但不少于上面的值(24/2=12)
            threadTopHeight = totalHeight / 3;
            if (threadTopHeight < 12) {
                threadTopHeight = 12;
            }
        }
        int lowerHalf = totalHeight - threadTopHeight;

        //Memory至少保留8行, 显示metaspace信息
        int memoryInfoHeight = lowerHalf / 2;
        if (memoryInfoHeight < 8) {
            memoryInfoHeight = Math.min(8, lowerHalf);
        }

        //runtime
        TableElement runtimeInfoTable = drawRuntimeInfo(result.getRuntimeInfo());
        //tomcat
        TableElement tomcatInfoTable = drawTomcatInfo(result.getTomcatInfo());
        int runtimeInfoHeight = Math.max(runtimeInfoTable.getRows().size(), tomcatInfoTable == null ? 0 : tomcatInfoTable.getRows().size());
        if (runtimeInfoHeight < lowerHalf - memoryInfoHeight) {
            //如果runtimeInfo高度有剩余，则增大MemoryInfo的高度
            memoryInfoHeight = lowerHalf - runtimeInfoHeight;
        } else {
            runtimeInfoHeight = lowerHalf - memoryInfoHeight;
        }

        //如果MemoryInfo高度有剩余，则增大ThreadHeight
        int maxMemoryInfoHeight = getMemoryInfoHeight(result.getMemoryInfo());
        memoryInfoHeight = Math.min(memoryInfoHeight, maxMemoryInfoHeight);
        threadTopHeight = totalHeight - memoryInfoHeight - runtimeInfoHeight;

        String threadInfo = ViewRenderUtil.drawThreadInfo(result.getThreads(), width, threadTopHeight);
        String memoryAndGc = drawMemoryInfoAndGcInfo(result.getMemoryInfo(), result.getGcInfos(), width, memoryInfoHeight);
        String runTimeAndTomcat = drawRuntimeInfoAndTomcatInfo(runtimeInfoTable, tomcatInfoTable, width, runtimeInfoHeight);

        process.write(threadInfo + memoryAndGc + runTimeAndTomcat);
    }

    static String drawMemoryInfoAndGcInfo(Map<String, List<MemoryEntryVO>> memoryInfo, List<GcInfoVO> gcInfos, int width, int height) {
        TableElement table = new TableElement(1, 1);
        TableElement memoryInfoTable = MemoryView.drawMemoryInfo(memoryInfo);
        TableElement gcInfoTable = drawGcInfo(gcInfos);
        table.row(memoryInfoTable, gcInfoTable);
        return RenderUtil.render(table, width, height);
    }

    private static int getMemoryInfoHeight(Map<String, List<MemoryEntryVO>> memoryInfo) {
        int height = 1;
        for (List<MemoryEntryVO> memoryEntryVOS : memoryInfo.values()) {
            height += memoryEntryVOS.size();
        }
        return height;
    }

    private static TableElement drawGcInfo(List<GcInfoVO> gcInfos) {
        TableElement table = new TableElement(1, 1).rightCellPadding(1);
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("GC", ""));
        for (GcInfoVO gcInfo : gcInfos) {
            table.add(new RowElement().style(Decoration.bold.bold()).add("gc." + gcInfo.getName() + ".count",
                    "" + gcInfo.getCollectionCount()));
            table.row("gc." + gcInfo.getName() + ".time(ms)", "" + gcInfo.getCollectionTime());
        }
        return table;
    }

    String drawRuntimeInfoAndTomcatInfo(TableElement runtimeInfoTable, TableElement tomcatInfoTable, int width, int height) {
        if (height <= 0) {
            return "";
        }
        TableElement resultTable = new TableElement(1, 1);
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
        table.row("timestamp/uptime", new Date(runtimeInfo.getTimestamp()).toString() + "/" + runtimeInfo.getUptime() + "s");
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
}

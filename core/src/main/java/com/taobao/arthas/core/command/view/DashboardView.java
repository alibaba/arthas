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
 * Dashboard命令的结果视图类
 *
 * 负责将Dashboard命令的结果渲染并输出到命令行界面。
 * Dashboard命令显示Java应用程序的实时运行信息，包括：
 * 1. 线程信息（Thread）：显示线程CPU使用率Top N
 * 2. 内存信息（Memory）：显示堆内存、非堆内存等使用情况
 * 3. GC信息（Garbage Collection）：显示GC次数和耗时
 * 4. Runtime信息：显示操作系统、Java版本、系统负载等
 * 5. Tomcat信息（如果存在）：显示连接器状态和线程池状态
 *
 * 界面布局策略：
 * - 上半部分：线程Top信息
 * - 下半部分分为田字格：
 *   - 上面两格：内存信息和GC信息
 *   - 下面两格：Runtime信息和Tomcat信息
 *
 * View of 'dashboard' command
 *
 * @author gongdewei 2020/4/22
 */
public class DashboardView extends ResultView<DashboardModel> {

    /**
     * 绘制Dashboard命令的执行结果
     *
     * 根据终端窗口的大小，智能布局各个信息模块，确保所有信息都能清晰显示。
     * 核心逻辑是动态计算各个模块的高度，合理分配屏幕空间。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param result Dashboard命令的执行结果模型，包含各种运行时信息
     */
    @Override
    public void draw(CommandProcess process, DashboardModel result) {
        // 获取终端窗口的宽度和高度
        int width = process.width();
        int height = process.height();

        // 上半部分放thread top。下半部分再切分为田字格，其中上面两格放memory, gc的信息。下面两格放tomcat,
        // runtime的信息

        // 总高度减去1，留出空间
        int totalHeight = height - 1;
        int threadTopHeight;

        // 根据总高度动态计算线程信息区域的高度
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

        // 下半部分的高度
        int lowerHalf = totalHeight - threadTopHeight;

        //Memory至少保留8行, 显示metaspace信息
        // 初始分配给内存信息的高度为下半部分的一半
        int memoryInfoHeight = lowerHalf / 2;
        if (memoryInfoHeight < 8) {
            // 确保内存信息至少有8行，但不超过下半部分的总高度
            memoryInfoHeight = Math.min(8, lowerHalf);
        }

        //runtime
        // 绘制Runtime信息表格
        TableElement runtimeInfoTable = drawRuntimeInfo(result.getRuntimeInfo());
        //tomcat
        // 绘制Tomcat信息表格
        TableElement tomcatInfoTable = drawTomcatInfo(result.getTomcatInfo());

        // 计算Runtime和Tomcat信息区域的高度，取两者的最大行数
        int runtimeInfoHeight = Math.max(runtimeInfoTable.getRows().size(), tomcatInfoTable == null ? 0 : tomcatInfoTable.getRows().size());

        if (runtimeInfoHeight < lowerHalf - memoryInfoHeight) {
            //如果runtimeInfo高度有剩余，则增大MemoryInfo的高度
            memoryInfoHeight = lowerHalf - runtimeInfoHeight;
        } else {
            // 否则，调整runtimeInfo的高度
            runtimeInfoHeight = lowerHalf - memoryInfoHeight;
        }

        //如果MemoryInfo高度有剩余，则增大ThreadHeight
        // 计算内存信息的实际最大高度
        int maxMemoryInfoHeight = getMemoryInfoHeight(result.getMemoryInfo());
        // 限制内存信息高度不超过实际需要的高度
        memoryInfoHeight = Math.min(memoryInfoHeight, maxMemoryInfoHeight);
        // 重新计算线程信息的高度，确保总高度正确
        threadTopHeight = totalHeight - memoryInfoHeight - runtimeInfoHeight;

        // 绘制各个信息模块
        String threadInfo = ViewRenderUtil.drawThreadInfo(result.getThreads(), width, threadTopHeight);
        String memoryAndGc = drawMemoryInfoAndGcInfo(result.getMemoryInfo(), result.getGcInfos(), width, memoryInfoHeight);
        String runTimeAndTomcat = drawRuntimeInfoAndTomcatInfo(runtimeInfoTable, tomcatInfoTable, width, runtimeInfoHeight);

        // 将所有模块拼接后输出
        process.write(threadInfo + memoryAndGc + runTimeAndTomcat);
    }

    /**
     * 绘制内存信息和GC信息
     *
     * 创建一个表格，左侧显示内存信息，右侧显示GC信息。
     * 将两个信息模块并排显示，充分利用屏幕宽度。
     *
     * @param memoryInfo 内存信息映射表，键为内存区名称，值为内存条目列表
     * @param gcInfos GC信息列表
     * @param width 终端窗口宽度
     * @param height 分配给该区域的高度
     * @return 渲染后的字符串
     */
    static String drawMemoryInfoAndGcInfo(Map<String, List<MemoryEntryVO>> memoryInfo, List<GcInfoVO> gcInfos, int width, int height) {
        // 创建一个1行2列的表格
        TableElement table = new TableElement(1, 1);
        // 绘制内存信息表格
        TableElement memoryInfoTable = MemoryView.drawMemoryInfo(memoryInfo);
        // 绘制GC信息表格
        TableElement gcInfoTable = drawGcInfo(gcInfos);
        // 将两个表格添加到同一行
        table.row(memoryInfoTable, gcInfoTable);
        // 渲染表格
        return RenderUtil.render(table, width, height);
    }

    /**
     * 计算内存信息的实际高度
     *
     * 遍历所有内存区，计算总行数。
     * 每个内存区占用一行标题，加上该内存区中的所有内存条目。
     *
     * @param memoryInfo 内存信息映射表
     * @return 内存信息的总行数
     */
    private static int getMemoryInfoHeight(Map<String, List<MemoryEntryVO>> memoryInfo) {
        // 初始高度为1（可能用于标题或其他）
        int height = 1;
        // 遍历所有内存区
        for (List<MemoryEntryVO> memoryEntryVOS : memoryInfo.values()) {
            // 累加每个内存区的条目数量
            height += memoryEntryVOS.size();
        }
        return height;
    }

    /**
     * 绘制GC信息表格
     *
     * 创建一个表格，显示各个垃圾收集器的统计信息，包括GC次数和GC耗时。
     *
     * @param gcInfos GC信息列表
     * @return GC信息表格元素
     */
    private static TableElement drawGcInfo(List<GcInfoVO> gcInfos) {
        // 创建表格，设置右边距为1
        TableElement table = new TableElement(1, 1).rightCellPadding(1);
        // 添加标题行，使用黑底白字加粗样式
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("GC", ""));

        // 遍历所有GC信息
        for (GcInfoVO gcInfo : gcInfos) {
            // 添加GC收集次数行，使用加粗样式
            table.add(new RowElement().style(Decoration.bold.bold()).add("gc." + gcInfo.getName() + ".count",
                    "" + gcInfo.getCollectionCount()));
            // 添加GC收集时间行
            table.row("gc." + gcInfo.getName() + ".time(ms)", "" + gcInfo.getCollectionTime());
        }
        return table;
    }

    /**
     * 绘制Runtime信息和Tomcat信息
     *
     * 将Runtime信息和Tomcat信息并排显示在表格中。
     * 如果Tomcat信息不存在，则只显示Runtime信息。
     *
     * @param runtimeInfoTable Runtime信息表格
     * @param tomcatInfoTable Tomcat信息表格（可能为null）
     * @param width 终端窗口宽度
     * @param height 分配给该区域的高度
     * @return 渲染后的字符串，如果高度<=0则返回空字符串
     */
    String drawRuntimeInfoAndTomcatInfo(TableElement runtimeInfoTable, TableElement tomcatInfoTable, int width, int height) {
        // 如果高度不足，返回空字符串
        if (height <= 0) {
            return "";
        }
        // 创建一个1行2列的表格
        TableElement resultTable = new TableElement(1, 1);

        if (tomcatInfoTable != null) {
            // 如果Tomcat信息存在，将Runtime和Tomcat信息并排显示
            resultTable.row(runtimeInfoTable, tomcatInfoTable);
        } else {
            // 如果Tomcat信息不存在，只显示Runtime信息
            resultTable = runtimeInfoTable;
        }
        // 渲染表格
        return RenderUtil.render(resultTable, width, height);
    }

    /**
     * 绘制Runtime信息表格
     *
     * 创建一个表格，显示Java运行时的基本信息，包括操作系统、Java版本、系统负载等。
     *
     * @param runtimeInfo Runtime信息对象
     * @return Runtime信息表格元素
     */
    private static TableElement drawRuntimeInfo(RuntimeInfoVO runtimeInfo) {
        // 创建表格，设置右边距为1
        TableElement table = new TableElement(1, 1).rightCellPadding(1);
        // 添加标题行，使用黑底白字加粗样式
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Runtime", ""));

        // 添加操作系统名称
        table.row("os.name", runtimeInfo.getOsName());
        // 添加操作系统版本
        table.row("os.version", runtimeInfo.getOsVersion());
        // 添加Java版本
        table.row("java.version", runtimeInfo.getJavaVersion());
        // 添加Java安装目录
        table.row("java.home", runtimeInfo.getJavaHome());
        // 添加系统平均负载（保留两位小数）
        table.row("systemload.average", String.format("%.2f", runtimeInfo.getSystemLoadAverage()));
        // 添加处理器数量
        table.row("processors", "" + runtimeInfo.getProcessors());
        // 添加时间戳和运行时间（秒）
        table.row("timestamp/uptime", new Date(runtimeInfo.getTimestamp()).toString() + "/" + runtimeInfo.getUptime() + "s");
        return table;
    }

    /**
     * 格式化字节数
     *
     * 将字节数转换为合适的单位（B、K、M），便于阅读。
     * 转换规则：
     * - 小于1024字节：使用B（字节）
     * - 小于1024*1024字节：使用K（千字节）
     * - 大于等于1024*1024字节：使用M（兆字节）
     *
     * 注意：当前实现只检查size/1024>0，这意味着只要size>=1024就会使用K单位，
     * 可能需要更精确的单位选择逻辑（例如size >= 1024*1024才使用M）。
     *
     * @param size 字节数
     * @return 格式化后的字符串，包含数值和单位
     */
    private static String formatBytes(long size) {
        int unit = 1;
        String unitStr = "B";
        if (size / 1024 > 0) {
            // 如果大小大于1024字节，使用KB单位
            unit = 1024;
            unitStr = "K";
        } else if (size / 1024 / 1024 > 0) {
            // 如果大小大于1024*1024字节，使用MB单位
            unit = 1024 * 1024;
            unitStr = "M";
        }

        // 返回格式化后的字符串
        return String.format("%d%s", size / unit, unitStr);
    }

    /**
     * 绘制Tomcat信息表格
     *
     * 创建一个表格，显示Tomcat服务器的状态信息，包括：
     * 1. 连接器统计信息（Connector Stats）：QPS、响应时间、错误率、接收/发送字节数
     * 2. 线程池信息（Thread Pool）：繁忙线程数、总线程数
     *
     * @param tomcatInfo Tomcat信息对象
     * @return Tomcat信息表格元素，如果tomcatInfo为null则返回null
     */
    private TableElement drawTomcatInfo(TomcatInfoVO tomcatInfo) {
        // 如果Tomcat信息不存在，返回null
        if (tomcatInfo == null) {
            return null;
        }

        //header
        // 创建表格，设置右边距为1
        TableElement table = new TableElement(1, 1).rightCellPadding(1);
        // 添加标题行，使用黑底白字加粗样式
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Tomcat", ""));

        // 如果有连接器统计信息，则遍历并显示
        if (tomcatInfo.getConnectorStats() != null) {
            for (TomcatInfoVO.ConnectorStats connectorStat : tomcatInfo.getConnectorStats()) {
                // 添加连接器名称行，使用加粗样式
                table.add(new RowElement().style(Decoration.bold.bold()).add("connector", connectorStat.getName()));
                // 添加每秒查询数（QPS）
                table.row("QPS", String.format("%.2f", connectorStat.getQps()));
                // 添加响应时间（毫秒）
                table.row("RT(ms)", String.format("%.2f", connectorStat.getRt()));
                // 添加每秒错误数
                table.row("error/s", String.format("%.2f", connectorStat.getError()));
                // 添加每秒接收字节数（格式化为易读单位）
                table.row("received/s", formatBytes(connectorStat.getReceived()));
                // 添加每秒发送字节数（格式化为易读单位）
                table.row("sent/s", formatBytes(connectorStat.getSent()));
            }
        }

        // 如果有线程池信息，则遍历并显示
        if (tomcatInfo.getThreadPools() != null) {
            for (TomcatInfoVO.ThreadPool threadPool : tomcatInfo.getThreadPools()) {
                // 添加线程池名称行，使用加粗样式
                table.add(new RowElement().style(Decoration.bold.bold()).add("threadpool", threadPool.getName()));
                // 添加繁忙线程数
                table.row("busy", "" + threadPool.getBusy());
                // 添加总线程数
                table.row("total", "" + threadPool.getTotal());
            }
        }
        return table;
    }
}

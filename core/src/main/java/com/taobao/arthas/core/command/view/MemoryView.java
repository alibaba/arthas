package com.taobao.arthas.core.command.view;

import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;

import com.taobao.arthas.core.command.model.MemoryEntryVO;
import com.taobao.arthas.core.command.model.MemoryModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

/**
 * memory命令的视图类
 * 负责将JVM内存信息渲染为表格形式展示给用户
 *
 * @author hengyunabc 2022-03-01
 */
public class MemoryView extends ResultView<MemoryModel> {

    /**
     * 绘制memory命令的执行结果
     *
     * @param process 命令处理进程，用于输出结果
     * @param result memory命令的执行结果模型
     */
    @Override
    public void draw(CommandProcess process, MemoryModel result) {
        // 绘制内存信息表格
        TableElement table = drawMemoryInfo(result.getMemoryInfo());
        // 渲染并输出表格
        process.write(RenderUtil.render(table, process.width()));
    }

    /**
     * 绘制内存信息表格
     * 包括堆内存、非堆内存和缓冲池的详细信息
     *
     * @param memoryInfo 内存信息映射表，key为内存类型，value为该类型的内存条目列表
     * @return 渲染后的表格元素
     */
    static TableElement drawMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfo) {
        // 创建表格，5列（名称、已用、总计、最大、使用率），设置右内边距
        TableElement table = new TableElement(3, 1, 1, 1, 1).rightCellPadding(1);
        // 添加表头行，黑色字体、白色背景、加粗显示
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Memory",
                "used", "total", "max", "usage"));
        // 获取堆内存条目列表
        List<MemoryEntryVO> heapMemoryEntries = memoryInfo.get(MemoryEntryVO.TYPE_HEAP);
        // 处理堆内存数据
        for (MemoryEntryVO memoryEntryVO : heapMemoryEntries) {
            // 如果是堆内存总览行，则使用加粗样式
            if (MemoryEntryVO.TYPE_HEAP.equals(memoryEntryVO.getName())) {
                new MemoryEntry(memoryEntryVO).addTableRow(table, Decoration.bold.bold());
            } else {
                // 其他堆内存区域使用普通样式
                new MemoryEntry(memoryEntryVO).addTableRow(table);
            }
        }

        // 处理非堆内存数据
        List<MemoryEntryVO> nonheapMemoryEntries = memoryInfo.get(MemoryEntryVO.TYPE_NON_HEAP);
        for (MemoryEntryVO memoryEntryVO : nonheapMemoryEntries) {
            // 如果是非堆内存总览行，则使用加粗样式
            if (MemoryEntryVO.TYPE_NON_HEAP.equals(memoryEntryVO.getName())) {
                new MemoryEntry(memoryEntryVO).addTableRow(table, Decoration.bold.bold());
            } else {
                // 其他非堆内存区域使用普通样式
                new MemoryEntry(memoryEntryVO).addTableRow(table);
            }
        }

        // 处理缓冲池数据
        List<MemoryEntryVO> bufferPoolMemoryEntries = memoryInfo.get(MemoryEntryVO.TYPE_BUFFER_POOL);
        if (bufferPoolMemoryEntries != null) {
            for (MemoryEntryVO memoryEntryVO : bufferPoolMemoryEntries) {
                new MemoryEntry(memoryEntryVO).addTableRow(table);
            }
        }
        return table;
    }

    /**
     * 内存条目内部类
     * 用于封装单个内存区域的信息并提供格式化功能
     */
    static class MemoryEntry {
        /** 内存区域名称 */
        String name;
        /** 已使用的内存量（字节） */
        long used;
        /** 已提交的内存量（字节） */
        long total;
        /** 最大可用内存量（字节） */
        long max;

        /** 显示单位（1024或1048576，对应K或M） */
        int unit;
        /** 显示单位字符串（"K"或"M"） */
        String unitStr;

        /**
         * 构造函数：使用具体的内存数值
         *
         * @param name 内存区域名称
         * @param used 已使用的内存量（字节）
         * @param total 已提交的内存量（字节）
         * @param max 最大可用内存量（字节）
         */
        public MemoryEntry(String name, long used, long total, long max) {
            this.name = name;
            this.used = used;
            this.total = total;
            this.max = max;

            // 默认使用KB作为显示单位
            unitStr = "K";
            unit = 1024;
            // 如果已用内存超过1MB，则改用MB作为显示单位
            if (used / 1024 / 1024 > 0) {
                unitStr = "M";
                unit = 1024 * 1024;
            }
        }

        /**
         * 构造函数：使用MemoryUsage对象
         *
         * @param name 内存区域名称
         * @param usage 内存使用情况对象
         */
        public MemoryEntry(String name, MemoryUsage usage) {
            this(name, usage.getUsed(), usage.getCommitted(), usage.getMax());
        }

        /**
         * 构造函数：使用MemoryEntryVO对象
         *
         * @param memoryEntryVO 内存条目值对象
         */
        public MemoryEntry(MemoryEntryVO memoryEntryVO) {
            this(memoryEntryVO.getName(), memoryEntryVO.getUsed(), memoryEntryVO.getTotal(), memoryEntryVO.getMax());
        }

        /**
         * 格式化内存值为可读的字符串
         * 将字节数转换为KB或MB单位
         *
         * @param value 要格式化的内存值（字节）
         * @return 格式化后的字符串，如"1024K"、"512M"等
         */
        private String format(long value) {
            String valueStr = "-";
            // 值为-1时直接返回"-1"
            if (value == -1) {
                return "-1";
            }
            // 值不为Long.MIN_VALUE时进行格式化
            if (value != Long.MIN_VALUE) {
                valueStr = value / unit + unitStr;
            }
            return valueStr;
        }

        /**
         * 添加内存条目行到表格（使用默认样式）
         * 计算使用率并格式化所有数值
         *
         * @param table 表格元素
         */
        public void addTableRow(TableElement table) {
            // 计算内存使用率：如果最大值为-1或Long.MIN_VALUE，则使用总量作为分母
            double usage = used / (double) (max == -1 || max == Long.MIN_VALUE ? total : max) * 100;
            // 处理NaN和Infinite情况
            if (Double.isNaN(usage) || Double.isInfinite(usage)) {
                usage = 0;
            }
            // 添加表格行：名称、已用、总计、最大、使用率
            table.row(name, format(used), format(total), format(max), String.format("%.2f%%", usage));
        }

        /**
         * 添加内存条目行到表格（使用指定样式）
         * 计算使用率并格式化所有数值
         *
         * @param table 表格元素
         * @param style 样式对象
         */
        public void addTableRow(TableElement table, Style.Composite style) {
            // 计算内存使用率：如果最大值为-1或Long.MIN_VALUE，则使用总量作为分母
            double usage = used / (double) (max == -1 || max == Long.MIN_VALUE ? total : max) * 100;
            // 处理NaN和Infinite情况
            if (Double.isNaN(usage) || Double.isInfinite(usage)) {
                usage = 0;
            }
            // 添加带样式的表格行
            table.add(new RowElement().style(style).add(name, format(used), format(total), format(max),
                    String.format("%.2f%%", usage)));
        }
    }
}

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
 * View of 'memory' command
 *
 * @author hengyunabc 2022-03-01
 */
public class MemoryView extends ResultView<MemoryModel> {

    @Override
    public void draw(CommandProcess process, MemoryModel result) {
        TableElement table = drawMemoryInfo(result.getMemoryInfo());
        process.write(RenderUtil.render(table, process.width()));
    }

    static TableElement drawMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfo) {
        TableElement table = new TableElement(3, 1, 1, 1, 1).rightCellPadding(1);
        table.add(new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add("Memory",
                "used", "total", "max", "usage"));
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
        if (bufferPoolMemoryEntries != null) {
            for (MemoryEntryVO memoryEntryVO : bufferPoolMemoryEntries) {
                new MemoryEntry(memoryEntryVO).addTableRow(table);
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
            if (Double.isNaN(usage) || Double.isInfinite(usage)) {
                usage = 0;
            }
            table.row(name, format(used), format(total), format(max), String.format("%.2f%%", usage));
        }

        public void addTableRow(TableElement table, Style.Composite style) {
            double usage = used / (double) (max == -1 || max == Long.MIN_VALUE ? total : max) * 100;
            if (Double.isNaN(usage) || Double.isInfinite(usage)) {
                usage = 0;
            }
            table.add(new RowElement().style(style).add(name, format(used), format(total), format(max),
                    String.format("%.2f%%", usage)));
        }
    }
}

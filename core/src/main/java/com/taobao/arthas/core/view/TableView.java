package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.StringUtils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.String.format;

/**
 * 表格控件
 * Created by vlinux on 15/5/7.
 */
public class TableView implements View {

    /**
     * 上边框
     */
    public static final int BORDER_TOP = 1;

    /**
     * 下边框
     */
    public static final int BORDER_BOTTOM = 1 << 1;

    // 各个列的定义
    private final ColumnDefine[] columnDefineArray;

    // 是否渲染边框
    private boolean hasBorder;

    // 边框
    private int borders = BORDER_TOP | BORDER_BOTTOM;

    // 内填充
    private int padding;

    public TableView(ColumnDefine[] columnDefineArray) {
        this.columnDefineArray = null == columnDefineArray
                ? new ColumnDefine[0]
                : columnDefineArray;
    }

    public TableView(int columnNum) {
        this.columnDefineArray = new ColumnDefine[columnNum];
        for (int index = 0; index < this.columnDefineArray.length; index++) {
            columnDefineArray[index] = new ColumnDefine();
        }
    }

    private boolean isAnyBorder(int... borders) {
        if (null == borders) {
            return false;
        }
        for (int b : borders) {
            if ((this.borders & b) == b) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取表格边框设置
     *
     * @return 边框位
     */
    public int borders() {
        return borders;
    }

    /**
     * 设置表格边框
     *
     * @param border 边框位
     * @return this
     */
    public TableView borders(int border) {
        this.borders = border;
        return this;
    }

    @Override
    public String draw() {
        final StringBuilder tableSB = new StringBuilder();

        // init width cache
        final int[] widthCacheArray = new int[getColumnCount()];
        for (int index = 0; index < widthCacheArray.length; index++) {
            widthCacheArray[index] = abs(columnDefineArray[index].getWidth());
        }

        final int tableHigh = getTableHigh();
        for (int rowIndex = 0; rowIndex < tableHigh; rowIndex++) {

            final boolean isFirstRow = rowIndex == 0;
            final boolean isLastRow = rowIndex == tableHigh - 1;

            // 打印首分隔行
            if (isFirstRow
                    && hasBorder()
                    && isAnyBorder(BORDER_TOP)) {
                tableSB.append(drawSeparationLine(widthCacheArray)).append("\n");
            }

            // 打印内部分割行
            if (!isFirstRow
                    && hasBorder()) {
                tableSB.append(drawSeparationLine(widthCacheArray)).append("\n");
            }

            // 绘一行
            tableSB.append(drawRow(widthCacheArray, rowIndex));


            // 打印结尾分隔行
            if (isLastRow
                    && hasBorder()
                    && isAnyBorder(BORDER_BOTTOM)) {
                // 打印分割行
                tableSB.append(drawSeparationLine(widthCacheArray)).append("\n");
            }

        }


        return tableSB.toString();
    }


    private String drawRow(int[] widthCacheArray, int rowIndex) {

        final StringBuilder rowSB = new StringBuilder();
        final Scanner[] scannerArray = new Scanner[getColumnCount()];
        try {
            boolean hasNext;
            do {

                hasNext = false;
                final StringBuilder segmentSB = new StringBuilder();

                for (int colIndex = 0; colIndex < getColumnCount(); colIndex++) {


                    final String borderChar = hasBorder() ? "|" : Constants.EMPTY_STRING;
                    final int width = widthCacheArray[colIndex];
                    final boolean isLastColOfRow = colIndex == widthCacheArray.length - 1;


                    if (null == scannerArray[colIndex]) {
                        scannerArray[colIndex] = new Scanner(
                                new StringReader(StringUtils.wrap(getData(rowIndex, columnDefineArray[colIndex]), width)));
                    }
                    final Scanner scanner = scannerArray[colIndex];

                    final String data;
                    if (scanner.hasNext()) {
                        data = scanner.nextLine();
                        hasNext = true;
                    } else {
                        data = Constants.EMPTY_STRING;
                    }

                    if (width > 0) {
                        final ColumnDefine columnDefine = columnDefineArray[colIndex];
                        final String dataFormat = getDataFormat(columnDefine, width);
                        final String paddingChar = StringUtils.repeat(" ", padding);
                        segmentSB.append(format(borderChar + paddingChar + dataFormat + paddingChar, data));
                    }

                    if (isLastColOfRow) {
                        segmentSB.append(borderChar).append("\n");
                    }

                }

                if (hasNext) {
                    rowSB.append(segmentSB);
                }

            } while (hasNext);

            return rowSB.toString();
        } finally {
            for (Scanner scanner : scannerArray) {
                if (null != scanner) {
                    scanner.close();
                }
            }
        }

    }

    private String getData(int rowIndex, ColumnDefine columnDefine) {
        return columnDefine.getHigh() <= rowIndex
                ? Constants.EMPTY_STRING
                : columnDefine.dataList.get(rowIndex);
    }

    private String getDataFormat(ColumnDefine columnDefine, int width) {
        switch (columnDefine.align) {
            case RIGHT: {
                return "%" + width + "s";
            }
            case LEFT:
            default: {
                return "%-" + width + "s";
            }
        }
    }

    /*
     * 获取表格高度
     */
    private int getTableHigh() {
        int tableHigh = 0;
        for (ColumnDefine columnDefine : columnDefineArray) {
            tableHigh = max(tableHigh, columnDefine.getHigh());
        }
        return tableHigh;
    }

    /*
     * 打印分隔行
     */
    private String drawSeparationLine(int[] widthCacheArray) {
        final StringBuilder separationLineSB = new StringBuilder();
        for (int width : widthCacheArray) {
            if (width > 0) {
                separationLineSB.append("+").append(StringUtils.repeat("-", width + 2 * padding));
            }
        }
        return separationLineSB
                .append("+")
                .toString();
    }

    /**
     * 添加数据行
     *
     * @param columnDataArray 数据数组
     */
    public TableView addRow(Object... columnDataArray) {
        if (null == columnDataArray) {
            return this;
        }

        for (int index = 0; index < columnDefineArray.length; index++) {
            final ColumnDefine columnDefine = columnDefineArray[index];
            if (index < columnDataArray.length
                    && null != columnDataArray[index]) {
                columnDefine.dataList.add(StringUtils.replace(columnDataArray[index].toString(), "\t", "    "));
            } else {
                columnDefine.dataList.add(Constants.EMPTY_STRING);
            }
        }

        return this;
    }


    /**
     * 对齐方向
     */
    public enum Align {
        LEFT,
        RIGHT
    }

    /**
     * 列定义
     */
    public static class ColumnDefine {

        private final int width;
        private final boolean isAutoResize;
        private final Align align;
        private final List<String> dataList = new ArrayList<String>();

        public ColumnDefine(int width, boolean isAutoResize, Align align) {
            this.width = width;
            this.isAutoResize = isAutoResize;
            this.align = align;
        }

        public ColumnDefine(Align align) {
            this(0, true, align);
        }

        public ColumnDefine() {
            this(Align.LEFT);
        }

        /**
         * 获取当前列的宽度
         *
         * @return 宽度
         */
        public int getWidth() {

            if (!isAutoResize) {
                return width;
            }

            int maxWidth = 0;
            for (String data : dataList) {
                final Scanner scanner = new Scanner(new StringReader(data));
                try {
                    while (scanner.hasNext()) {
                        maxWidth = max(StringUtils.length(scanner.nextLine()), maxWidth);
                    }
                } finally {
                    scanner.close();
                }
            }

            return maxWidth;
        }

        /**
         * 获取当前列的高度
         *
         * @return 高度
         */
        public int getHigh() {
            return dataList.size();
        }

    }

    /**
     * 设置是否画边框
     *
     * @param hasBorder true / false
     */
    public TableView hasBorder(boolean hasBorder) {
        this.hasBorder = hasBorder;
        return this;
    }

    /**
     * 是否画边框
     *
     * @return true / false
     */
    public boolean hasBorder() {
        return hasBorder;
    }

    /**
     * 设置内边距大小
     *
     * @param padding 内边距
     */
    public TableView padding(int padding) {
        this.padding = padding;
        return this;
    }

    /**
     * 获取表格列总数
     *
     * @return 表格列总数
     */
    public int getColumnCount() {
        return columnDefineArray.length;
    }

}

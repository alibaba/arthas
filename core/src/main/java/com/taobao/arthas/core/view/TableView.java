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
 * 表格视图
 * 用于在控制台中渲染表格数据
 * Created by vlinux on 15/5/7.
 */
public class TableView implements View {

    /**
     * 上边框标记
     */
    public static final int BORDER_TOP = 1;

    /**
     * 下边框标记
     */
    public static final int BORDER_BOTTOM = 1 << 1;

    /** 各个列的定义 */
    private final ColumnDefine[] columnDefineArray;

    /** 是否渲染边框 */
    private boolean hasBorder;

    /** 边框设置（位掩码） */
    private int borders = BORDER_TOP | BORDER_BOTTOM;

    /** 内填充大小 */
    private int padding;

    /**
     * 构造函数
     *
     * @param columnDefineArray 列定义数组
     */
    public TableView(ColumnDefine[] columnDefineArray) {
        this.columnDefineArray = null == columnDefineArray
                ? new ColumnDefine[0]
                : columnDefineArray;
    }

    /**
     * 构造函数
     *
     * @param columnNum 列数量
     */
    public TableView(int columnNum) {
        this.columnDefineArray = new ColumnDefine[columnNum];
        for (int index = 0; index < this.columnDefineArray.length; index++) {
            columnDefineArray[index] = new ColumnDefine();
        }
    }

    /**
     * 检查是否设置了指定的边框
     *
     * @param borders 要检查的边框标记
     * @return 如果设置了任意一个指定的边框则返回true
     */
    private boolean isAnyBorder(int... borders) {
        if (null == borders) {
            return false;
        }
        for (int b : borders) {
            // 使用位运算检查是否设置了指定的边框
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

    /**
     * 绘制表格
     *
     * @return 表格的字符串表示
     */
    @Override
    public String draw() {
        final StringBuilder tableSB = new StringBuilder();

        // 初始化宽度缓存
        final int[] widthCacheArray = new int[getColumnCount()];
        for (int index = 0; index < widthCacheArray.length; index++) {
            widthCacheArray[index] = abs(columnDefineArray[index].getWidth());
        }

        final int tableHigh = getTableHigh();
        for (int rowIndex = 0; rowIndex < tableHigh; rowIndex++) {

            final boolean isFirstRow = rowIndex == 0;
            final boolean isLastRow = rowIndex == tableHigh - 1;

            // 打印首分隔行（上边框）
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

            // 绘制一行数据
            tableSB.append(drawRow(widthCacheArray, rowIndex));


            // 打印结尾分隔行（下边框）
            if (isLastRow
                    && hasBorder()
                    && isAnyBorder(BORDER_BOTTOM)) {
                // 打印分割行
                tableSB.append(drawSeparationLine(widthCacheArray)).append("\n");
            }

        }


        return tableSB.toString();
    }


    /**
     * 绘制表格的一行
     *
     * @param widthCacheArray 列宽度缓存数组
     * @param rowIndex        行索引
     * @return 该行的字符串表示
     */
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


                    // 如果还没有为该列创建扫描器，则创建一个
                    if (null == scannerArray[colIndex]) {
                        scannerArray[colIndex] = new Scanner(
                                new StringReader(StringUtils.wrap(getData(rowIndex, columnDefineArray[colIndex]), width)));
                    }
                    final Scanner scanner = scannerArray[colIndex];

                    // 读取下一行数据
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
                        // 格式化数据并添加到行字符串
                        segmentSB.append(format(borderChar + paddingChar + dataFormat + paddingChar, data));
                    }

                    // 如果是最后一列，添加边框和换行符
                    if (isLastColOfRow) {
                        segmentSB.append(borderChar).append("\n");
                    }

                }

                // 如果还有数据，将该段添加到行字符串
                if (hasNext) {
                    rowSB.append(segmentSB);
                }

            } while (hasNext);

            return rowSB.toString();
        } finally {
            // 关闭所有扫描器
            for (Scanner scanner : scannerArray) {
                if (null != scanner) {
                    scanner.close();
                }
            }
        }

    }

    /**
     * 获取指定行的数据
     *
     * @param rowIndex     行索引
     * @param columnDefine 列定义
     * @return 该行的数据，如果行索引超出范围则返回空字符串
     */
    private String getData(int rowIndex, ColumnDefine columnDefine) {
        return columnDefine.getHigh() <= rowIndex
                ? Constants.EMPTY_STRING
                : columnDefine.dataList.get(rowIndex);
    }

    /**
     * 获取数据格式化字符串
     *
     * @param columnDefine 列定义
     * @param width        列宽
     * @return 格式化字符串
     */
    private String getDataFormat(ColumnDefine columnDefine, int width) {
        switch (columnDefine.align) {
            case RIGHT: {
                // 右对齐格式
                return "%" + width + "s";
            }
            case LEFT:
            default: {
                // 左对齐格式
                return "%-" + width + "s";
            }
        }
    }

    /**
     * 获取表格高度
     * 表格高度由所有列中高度最大的列决定
     *
     * @return 表格高度
     */
    private int getTableHigh() {
        int tableHigh = 0;
        for (ColumnDefine columnDefine : columnDefineArray) {
            tableHigh = max(tableHigh, columnDefine.getHigh());
        }
        return tableHigh;
    }

    /**
     * 绘制分隔行
     *
     * @param widthCacheArray 列宽度数组
     * @return 分隔行的字符串表示
     */
    private String drawSeparationLine(int[] widthCacheArray) {
        final StringBuilder separationLineSB = new StringBuilder();
        for (int width : widthCacheArray) {
            if (width > 0) {
                // 为每列绘制分隔符，宽度为列宽加上两倍的内边距
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
    /**
     * 添加数据行
     *
     * @param columnDataArray 数据数组
     * @return this，支持链式调用
     */
    public TableView addRow(Object... columnDataArray) {
        if (null == columnDataArray) {
            return this;
        }

        for (int index = 0; index < columnDefineArray.length; index++) {
            final ColumnDefine columnDefine = columnDefineArray[index];
            if (index < columnDataArray.length
                    && null != columnDataArray[index]) {
                // 将制表符替换为4个空格
                columnDefine.dataList.add(StringUtils.replace(columnDataArray[index].toString(), "\t", "    "));
            } else {
                // 如果数据不足，添加空字符串
                columnDefine.dataList.add(Constants.EMPTY_STRING);
            }
        }

        return this;
    }


    /**
     * 对齐方向枚举
     */
    public enum Align {
        /** 左对齐 */
        LEFT,
        /** 右对齐 */
        RIGHT
    }

    /**
     * 列定义类
     * 定义表格中每一列的属性
     */
    public static class ColumnDefine {

        /** 列宽 */
        private final int width;
        /** 是否自动调整大小 */
        private final boolean isAutoResize;
        /** 对齐方式 */
        private final Align align;
        /** 数据列表 */
        private final List<String> dataList = new ArrayList<String>();

        /**
         * 构造函数
         *
         * @param width        列宽
         * @param isAutoResize 是否自动调整大小
         * @param align        对齐方式
         */
        public ColumnDefine(int width, boolean isAutoResize, Align align) {
            this.width = width;
            this.isAutoResize = isAutoResize;
            this.align = align;
        }

        /**
         * 构造函数（自动调整大小）
         *
         * @param align 对齐方式
         */
        public ColumnDefine(Align align) {
            this(0, true, align);
        }

        /**
         * 默认构造函数（左对齐，自动调整大小）
         */
        public ColumnDefine() {
            this(Align.LEFT);
        }

        /**
         * 获取当前列的宽度
         *
         * @return 宽度
         */
        /**
         * 获取当前列的宽度
         * 如果设置为自动调整大小，则会根据数据内容计算最大宽度
         *
         * @return 列宽
         */
        public int getWidth() {

            if (!isAutoResize) {
                // 如果不是自动调整大小，直接返回设置的宽度
                return width;
            }

            // 计算所有数据中最大的宽度
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
        /**
         * 获取当前列的高度（数据行数）
         *
         * @return 列高
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
     * 是否绘制边框
     *
     * @return true表示绘制边框，false表示不绘制
     */
    public boolean hasBorder() {
        return hasBorder;
    }

    /**
     * 设置内边距大小
     *
     * @param padding 内边距大小
     * @return this，支持链式调用
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

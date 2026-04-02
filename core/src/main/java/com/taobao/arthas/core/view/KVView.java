package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.StringUtils;

import java.util.Scanner;

/**
 * 键值对（Key-Value）排版视图控件
 * 用于以表格形式展示键值对数据，支持自定义列定义
 * Created by vlinux on 15/5/9.
 */
public class KVView implements View {

    // 内部使用的表格视图，用于实际渲染键值对
    private final TableView tableView;

    /**
     * 默认构造函数
     * 创建一个三列的键值视图：键列、分隔符列（右对齐）、值列
     */
    public KVView() {
        this.tableView = new TableView(new TableView.ColumnDefine[]{
                // 第一列：键列，右对齐
                new TableView.ColumnDefine(TableView.Align.RIGHT),
                // 第二列：分隔符列，右对齐
                new TableView.ColumnDefine(TableView.Align.RIGHT),
                // 第三列：值列，左对齐
                new TableView.ColumnDefine(TableView.Align.LEFT)
        })
                // 不显示边框
                .hasBorder(false)
                // 内边距为0
                .padding(0);
    }

    /**
     * 带列定义的构造函数
     * 允许自定义键列和值列的列定义
     * @param keyColumnDefine 键列的列定义
     * @param valueColumnDefine 值列的列定义
     */
    public KVView(TableView.ColumnDefine keyColumnDefine, TableView.ColumnDefine valueColumnDefine) {
        this.tableView = new TableView(new TableView.ColumnDefine[]{
                // 第一列：使用自定义的键列定义
                keyColumnDefine,
                // 第二列：分隔符列，右对齐
                new TableView.ColumnDefine(TableView.Align.RIGHT),
                // 第三列：使用自定义的值列定义
                valueColumnDefine
        })
                // 不显示边框
                .hasBorder(false)
                // 内边距为0
                .padding(0);
    }

    /**
     * 添加一个键值对
     * @param key 键
     * @param value 值
     * @return this，支持链式调用
     */
    public KVView add(final Object key, final Object value) {
        // 添加一行，包含键、分隔符和值
        tableView.addRow(key, " : ", value);
        return this;
    }

    /**
     * 绘制键值视图
     * @return 格式化后的键值对字符串，已清理行尾多余空格
     */
    @Override
    public String draw() {
        // 先通过表格视图绘制内容
        String content = tableView.draw();
        StringBuilder sb = new StringBuilder();
        // 使用Scanner逐行处理内容
        Scanner scanner = new Scanner(content);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line != null) {
                // 清理一行后面多余的空格
                line = StringUtils.stripEnd(line, " ");
            }
            // 将处理后的行添加到结果中
            sb.append(line).append('\n');
        }
        scanner.close();
        return sb.toString();
    }
}

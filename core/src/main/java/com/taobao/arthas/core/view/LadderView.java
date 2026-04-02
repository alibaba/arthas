package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶梯缩进视图控件
 * 用于以阶梯缩进的形式展示层级关系的数据
 * 第一个条目不缩进，后续条目逐级缩进并显示分隔符
 * Created by vlinux on 15/5/8.
 */
public class LadderView implements View {

    // 分隔符：用于表示层级关系的符号
    private static final String LADDER_CHAR = "`-";

    // 缩进符：用于缩进的空格字符
    private static final String STEP_CHAR = " ";

    // 缩进长度：每级缩进的空格数
    private static final int INDENT_STEP = 2;

    // 存储所有要显示的条目
    private final List<String> items = new ArrayList<String>();


    /**
     * 绘制阶梯视图
     * @return 格式化的阶梯缩进字符串
     */
    @Override
    public String draw() {
        final StringBuilder ladderSB = new StringBuilder();
        // 深度计数器，用于控制缩进级别
        int deep = 0;
        // 遍历所有条目
        for (String item : items) {

            // 第一个条目不需要分隔符，直接输出
            if (deep == 0) {
                ladderSB
                        .append(item)
                        .append("\n");
            }

            // 其他的需要添加缩进和分隔符
            else {
                // 根据深度计算缩进空格数
                ladderSB
                        .append(StringUtils.repeat(STEP_CHAR, deep * INDENT_STEP))
                        // 添加分隔符
                        .append(LADDER_CHAR)
                        // 添加条目内容
                        .append(item)
                        .append("\n");
            }

            // 深度递增
            deep++;

        }
        return ladderSB.toString();
    }

    /**
     * 添加一个项目到阶梯视图
     *
     * @param item 要添加的项目字符串
     * @return this，支持链式调用
     */
    public LadderView addItem(String item) {
        items.add(item);
        return this;
    }

}

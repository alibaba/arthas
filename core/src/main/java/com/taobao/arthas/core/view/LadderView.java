package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶梯缩进控件
 * Created by vlinux on 15/5/8.
 */
public class LadderView implements View {

    // 分隔符
    private static final String LADDER_CHAR = "`-";

    // 缩进符
    private static final String STEP_CHAR = " ";

    // 缩进长度
    private static final int INDENT_STEP = 2;

    private final List<String> items = new ArrayList<String>();


    @Override
    public String draw() {
        final StringBuilder ladderSB = new StringBuilder();
        int deep = 0;
        for (String item : items) {

            // 第一个条目不需要分隔符
            if (deep == 0) {
                ladderSB
                        .append(item)
                        .append("\n");
            }

            // 其他的需要添加分隔符
            else {
                ladderSB
                        .append(StringUtils.repeat(STEP_CHAR, deep * INDENT_STEP))
                        .append(LADDER_CHAR)
                        .append(item)
                        .append("\n");
            }

            deep++;

        }
        return ladderSB.toString();
    }

    /**
     * 添加一个项目
     *
     * @param item 项目
     * @return this
     */
    public LadderView addItem(String item) {
        items.add(item);
        return this;
    }

}

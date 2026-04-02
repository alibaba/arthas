package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 行影响视图
 * 用于显示命令操作影响的行数或对象数量
 * 这是一个简单的视图，直接输出影响数量信息
 *
 * @author gongdewei 2020/4/8
 */
public class RowAffectView extends ResultView<RowAffectModel> {

    /**
     * 渲染行影响信息
     * 将影响数量格式化为字符串并输出
     *
     * @param process 命令处理进程，用于写入输出
     * @param result 行影响模型，包含影响数量信息
     */
    @Override
    public void draw(CommandProcess process, RowAffectModel result) {
        // 调用 affect() 方法获取影响数量的字符串表示，并输出
        // 例如："Affect(row-cnt:1) cost in 2 ms."
        process.write(result.affect() + "\n");
    }
}

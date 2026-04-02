package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.EchoModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 回显视图
 * 用于渲染echo命令的执行结果，简单地将内容输出到终端
 * 该视图用于测试和调试场景，直接显示命令中指定的内容
 *
 * @author gongdewei 2020/5/11
 */
public class EchoView extends ResultView<EchoModel> {
    /**
     * 绘制命令执行结果
     * 将模型中的内容直接输出到终端，不进行任何格式化处理
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 命令执行结果模型，包含要显示的内容
     */
    @Override
    public void draw(CommandProcess process, EchoModel result) {
        // 直接写入内容并换行
        process.write(result.getContent()).write("\n");
    }
}

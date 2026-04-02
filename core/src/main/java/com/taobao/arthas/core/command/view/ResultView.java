package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 用于 telnet term/tty 的命令结果视图
 * 注意：结果视图是一个可重用且无状态的实例
 *
 * @author gongdewei 2020/3/27
 * @param <T> 结果模型的类型，必须继承自 ResultModel
 */
public abstract class ResultView<T extends ResultModel> {

    /**
     * 将格式化的数据输出到 term/tty
     * 这是一个抽象方法，由子类实现具体的渲染逻辑
     *
     * @param process 命令处理进程，用于写入输出
     * @param result 要渲染的结果模型对象
     */
    public abstract void draw(CommandProcess process, T result);

    /**
     * 写入字符串并追加一个换行符
     * 这是一个辅助方法，用于简化输出操作
     *
     * @param process 命令处理进程，用于写入输出
     * @param str 要写入的字符串
     */
    protected void writeln(CommandProcess process, String str) {
        // 写入字符串
        process.write(str).write("\n");
    }
}

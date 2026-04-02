package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.term.Term;

/**
 * Term输出处理器
 * <p>
 * 该处理器将输出数据写入终端（Term）对象，用于在命令行界面显示输出结果。
 * 继承自StdoutHandler，是输出到终端的标准处理器。
 * </p>
 *
 * @author gehui 2017年7月26日 上午11:20:00
 */
public class TermHandler extends StdoutHandler {
    /** 终端对象，用于处理命令行输入输出 */
    private Term term;

    /**
     * 构造函数
     *
     * @param term 终端对象
     */
    public TermHandler(Term term) {
        this.term = term;
    }

    /**
     * 将数据写入终端
     * <p>
     * 接收输入数据并写入终端，然后返回原始数据。
     * 这样数据既显示在终端上，又可以在管道中继续传递。
     * </p>
     *
     * @param data 要输出的数据
     * @return 原始数据（不变）
     */
    @Override
    public String apply(String data) {
        term.write(data);
        return data;
    }
}
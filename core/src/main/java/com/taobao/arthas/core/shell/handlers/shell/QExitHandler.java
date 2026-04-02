package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * Q退出处理器
 * <p>
 * 用于处理用户输入的"q"命令，当用户输入"q"时退出当前的命令进程。
 * 这个处理器通常用于交互式提示场景，允许用户通过输入"q"快速退出。
 * </p>
 *
 * @author hengyunabc 2019-02-09
 *
 */
public class QExitHandler implements Handler<String> {
    /**
     * 当前的命令进程对象
     * 用于在用户输入"q"时结束该进程
     */
    private CommandProcess process;

    /**
     * 构造函数
     *
     * @param process 命令进程对象，将被此处理器管理
     */
    public QExitHandler(CommandProcess process) {
        this.process = process;
    }

    /**
     * 处理事件
     * <p>
     * 检查用户输入的事件是否为"q"（不区分大小写），
     * 如果是，则结束当前的命令进程。
     * </p>
     *
     * @param event 用户输入的事件字符串
     */
    @Override
    public void handle(String event) {
        // 判断输入是否为"q"（不区分大小写）
        if ("q".equalsIgnoreCase(event)) {
            // 结束当前命令进程
            process.end();
        }
    }
}

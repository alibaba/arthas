package com.taobao.arthas.core.shell.handlers.command;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * 命令中断处理器
 *
 * 当命令执行被中断时（例如用户按下Ctrl+C），该处理器负责清理资源并释放会话锁。
 * 这是Arthas命令执行框架中的重要组成部分，确保命令被中断后能够正确释放相关资源。
 *
 * @author ralf0131 2017-01-09 13:23.
 */
public class CommandInterruptHandler implements Handler<Void> {

    /**
     * 当前正在执行的命令进程
     * 用于在中断时结束命令并释放会话锁
     */
    private CommandProcess process;

    /**
     * 构造函数
     *
     * @param process 需要被监控的命令进程对象
     */
    public CommandInterruptHandler(CommandProcess process) {
        this.process = process;
    }

    /**
     * 处理命令中断事件
     *
     * 当命令执行被中断时调用此方法，执行以下操作：
     * 1. 结束命令进程的执行
     * 2. 释放会话锁，允许其他命令执行
     *
     * @param event 中断事件，Void类型表示无具体事件数据
     */
    @Override
    public void handle(Void event) {
        // 结束命令进程
        process.end();
        // 释放会话锁，解锁会话以便后续命令可以执行
        process.session().unLock();
    }
}

package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;

import java.util.Timer;

/**
 * Dashboard命令的中断处理器
 * 用于处理用户中断操作（如Ctrl+C），在终止前停止定时器
 *
 * @author ralf0131 2017-01-09 13:37.
 */
public class DashboardInterruptHandler extends CommandInterruptHandler {

    // 定时器引用，使用volatile保证多线程可见性
    private volatile Timer timer;

    /**
     * 构造函数
     *
     * @param process 命令进程
     * @param timer 需要管理的定时器
     */
    public DashboardInterruptHandler(CommandProcess process, Timer timer) {
        super(process);
        this.timer = timer;
    }

    /**
     * 处理中断事件
     * 当用户按下Ctrl+C时，首先停止定时器，然后调用父类的中断处理逻辑
     *
     * @param event 中断事件
     */
    @Override
    public void handle(Void event) {
        // 取消定时器，停止周期性任务
        timer.cancel();
        // 调用父类的中断处理方法，执行标准的中断流程
        super.handle(event);
    }
}

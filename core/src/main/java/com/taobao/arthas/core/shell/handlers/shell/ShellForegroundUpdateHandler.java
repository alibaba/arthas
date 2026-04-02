package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.system.Job;

/**
 * Shell前台更新处理器
 * <p>
 * 这个处理器用于处理前台任务更新的场景。
 * 当一个任务需要更新到前台或者前台任务状态发生变化时，
 * 这个处理器会被调用来处理相应的逻辑。
 * </p>
 * <p>
 * 主要用途：
 * - 当没有前台任务时，重新读取用户输入
 * - 管理前台任务的切换和状态更新
 * </p>
 *
 * @author beiwei30 on 23/11/2016.
 */
public class ShellForegroundUpdateHandler implements Handler<Job> {
    /**
     * Shell实现对象
     * 用于访问和管理shell的功能，包括读取用户输入等
     */
    private ShellImpl shell;

    /**
     * 构造函数
     *
     * @param shell Shell实现对象，将被此处理器使用
     */
    public ShellForegroundUpdateHandler(ShellImpl shell) {
        this.shell = shell;
    }

    /**
     * 处理前台任务更新事件
     * <p>
     * 当接收到前台任务更新事件时，检查任务对象是否为null：
     * - 如果为null，表示当前没有前台任务，此时重新开始读取用户输入
     * - 如果不为null，则表示有前台任务在运行，不需要特别处理
     * </p>
     *
     * @param job 任务对象，可能为null
     */
    @Override
    public void handle(Job job) {
        // 如果任务为null，说明当前没有前台任务
        if (job == null) {
            // 重新开始读取用户输入，等待下一个命令
            shell.readline();
        }
    }
}

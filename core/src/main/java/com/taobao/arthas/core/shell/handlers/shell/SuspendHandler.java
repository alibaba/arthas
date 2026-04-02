package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;

/**
 * 任务挂起处理器
 *
 * 该类实现了 SignalHandler 接口，用于处理任务挂起信号（通常是 Ctrl+Z）。
 * 当用户按下挂起快捷键时，该处理器会暂停当前正在前台运行的任务。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class SuspendHandler implements SignalHandler {

    /**
     * Shell 实例的引用
     * 用于获取当前终端、前台任务等信息
     */
    private ShellImpl shell;

    /**
     * 构造函数
     *
     * @param shell Shell 实例，用于访问终端和任务管理功能
     */
    public SuspendHandler(ShellImpl shell) {
        this.shell = shell;
    }

    /**
     * 处理挂起信号
     *
     * 当用户触发挂起信号时（如按下 Ctrl+Z），该方法会被调用。
     * 它会执行以下操作：
     * 1. 获取当前的前台任务
     * 2. 如果存在前台任务，则：
     *    a. 在终端显示任务状态变更信息（从运行中变为已停止）
     *    b. 调用任务的 suspend() 方法挂起任务
     * 3. 返回 true 表示信号已被处理
     *
     * @param key 触发挂起的按键码，用于识别是哪个按键触发了挂起信号
     * @return 始终返回 true，表示信号已被成功处理
     */
    @Override
    public boolean deliver(int key) {
        // 获取当前终端实例
        Term term = shell.term();

        // 获取当前正在前台运行的任务
        Job job = shell.getForegroundJob();

        // 如果存在前台任务
        if (job != null) {
            // 在终端显示任务状态变更信息
            // statusLine 方法会生成状态行，显示任务已从运行中变为已停止
            term.echo(shell.statusLine(job, ExecStatus.STOPPED));

            // 调用任务的 suspend 方法，实际挂起任务
            // 这会暂停任务的执行，并将其状态更新为 STOPPED
            job.suspend();
        }

        // 返回 true 表示信号已被成功处理
        return true;
    }
}

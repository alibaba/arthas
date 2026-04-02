package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.term.SignalHandler;

/**
 * 中断信号处理器
 *
 * 该类实现了SignalHandler接口,用于处理Shell中的中断信号(通常是Ctrl+C)。
 * 当用户按下中断键时,该处理器会中断当前正在运行的前台作业,
 * 如果没有前台作业,则返回true表示信号已被处理。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class InterruptHandler implements SignalHandler {

    /**
     * Shell实例引用
     * 用于获取和操作当前正在运行的前台作业
     */
    private ShellImpl shell;

    /**
     * 构造函数
     *
     * @param shell Shell实例,用于访问和控制前台作业
     */
    public InterruptHandler(ShellImpl shell) {
        this.shell = shell;
    }

    /**
     * 传递并处理中断信号
     *
     * 当用户按下中断键(如Ctrl+C)时,该方法会被调用。
     * 处理逻辑:
     * 1. 如果存在前台作业,则中断该作业并返回中断结果
     * 2. 如果没有前台作业,返回true表示信号已被处理
     *
     * @param key 按下的键值,用于识别是哪个键触发了信号
     * @return true表示信号已被处理,false表示信号未被处理
     */
    @Override
    public boolean deliver(int key) {
        // 检查是否有正在运行的前台作业
        if (shell.getForegroundJob() != null) {
            // 中断前台作业并返回中断结果
            return shell.getForegroundJob().interrupt();
        }
        // 没有前台作业时,返回true表示信号已被处理
        return true;
    }
}

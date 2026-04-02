package com.taobao.arthas.core.command.model;

/**
 * WebUI 的命令输入状态枚举
 * <p>
 * 定义了 Web 用户界面中命令输入框的三种状态：
 * 允许输入新命令、允许中断正在运行的任务、禁用输入和中断
 * </p>
 *
 * @author gongdewei 2020/4/14
 */
public enum InputStatus {
    /**
     * 允许输入状态
     * 表示当前可以输入新的命令
     */
    ALLOW_INPUT,

    /**
     * 允许中断状态
     * 表示当前有任务正在运行，允许用户中断正在执行的任务
     */
    ALLOW_INTERRUPT,

    /**
     * 禁用状态
     * 表示当前既不允许输入新命令，也不允许中断任务
     */
    DISABLED
}

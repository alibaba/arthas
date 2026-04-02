package com.taobao.arthas.core.shell.system;

/**
 * 作业执行状态枚举
 * 定义了作业（Job）在其生命周期中的各种可能状态
 *
 * 作业状态转换规则：
 * READY -> RUNNING: 作业开始执行
 * RUNNING -> STOPPED: 作业被暂停
 * STOPPED -> RUNNING: 暂停的作业恢复执行
 * READY/RUNNING/STOPPED -> TERMINATED: 作业终止（完成或被取消）
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public enum ExecStatus {

    /**
     * 就绪状态
     * 作业已创建并准备就绪，可以开始执行或已终止
     * 这是作业的初始状态
     */
    READY,

    /**
     * 运行状态
     * 作业正在执行中，可以被暂停或终止
     */
    RUNNING,

    /**
     * 停止状态
     * 作业已暂停执行，可以恢复运行或终止
     * 处于此状态的作业保留执行上下文，可以继续执行
     */
    STOPPED,

    /**
     * 终止状态
     * 作业已终止，这是最终状态
     * 作业执行完成、被取消或出错后都会进入此状态
     * 终止后的作业不能恢复执行
     */
    TERMINATED


}

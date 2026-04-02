package com.taobao.arthas.core.shell.system;

import java.util.Date;

import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;

/**
 * 任务（Job）接口
 *
 * 表示在JobController中执行的一个任务，可以包含一个或多个进程
 * 任务的生命周期可以通过run、resume、suspend和interrupt方法来控制
 *
 * 支持前台和后台运行模式，可以动态切换
 * 每个任务都有唯一ID、执行状态和对应的命令行
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Job {

    /**
     * 获取任务ID
     *
     * @return 任务ID，在同一个JobController中唯一
     */
    int id();

    /**
     * 获取任务的执行状态
     *
     * @return 任务执行状态枚举值
     */
    ExecStatus status();

    /**
     * 获取任务的执行命令行
     *
     * 即启动该任务的shell命令行内容
     *
     * @return 命令行字符串
     */
    String line();


    /**
     * 运行任务（默认前台运行）
     *
     * 在运行任务之前必须先设置Tty
     *
     * @return this对象，支持链式调用
     */
    Job run();

    /**
     * 运行任务，可以指定前台或后台运行
     *
     * 在运行任务之前必须先设置Tty
     *
     * @param foreground 是否在前台运行，true为前台，false为后台
     * @return this对象，支持链式调用
     */
    Job run(boolean foreground);

    /**
     * 尝试中断任务
     *
     * 发送中断信号给正在运行的任务
     *
     * @return 如果任务实际被中断了返回true，否则返回false
     */
    boolean interrupt();

    /**
     * 恢复任务运行到前台
     *
     * 将暂停或后台运行的任务恢复到前台运行
     *
     * @return this对象，支持链式调用
     */
    Job resume();

    /**
     * 判断任务是否在后台运行
     *
     * @return 如果任务正在后台运行返回true，否则返回false
     */
    boolean isRunInBackground();

    /**
     * 将任务发送到后台运行
     *
     * @return this对象，支持链式调用
     */
    Job toBackground();

    /**
     * 将任务调到前台运行
     *
     * @return this对象，支持链式调用
     */
    Job toForeground();

    /**
     * 恢复任务运行
     *
     * 可以指定恢复到前台还是后台
     *
     * @param foreground true表示恢复到前台，false表示恢复到后台
     * @return this对象，支持链式调用
     */
    Job resume(boolean foreground);

    /**
     * 暂停任务
     *
     * 将正在运行的任务暂停
     *
     * @return this对象，支持链式调用
     */
    Job suspend();

    /**
     * 终止任务
     *
     * 强制结束任务的执行，释放相关资源
     */
    void terminate();

    /**
     * 获取任务中的第一个进程
     *
     * 一个任务可能包含多个进程，这里返回第一个
     *
     * @return 任务中的第一个进程对象
     */
    Process process();

    /**
     * 获取任务的超时时间
     *
     * 如果任务设置了超时，返回超时的日期时间
     *
     * @return 任务超时的日期，如果未设置超时则返回null
     */
    Date timeoutDate();

    /**
     * 设置任务的超时时间
     *
     * @param date 任务超时的日期时间
     */
    void setTimeoutDate(Date date);

    /**
     * 获取任务所属的会话
     *
     * 每个任务都属于一个特定的会话，会话中保存了任务运行时的上下文信息
     *
     * @return 任务所属的会话对象
     */
    Session getSession();
}

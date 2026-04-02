package com.taobao.arthas.core.shell.system;

import java.util.Date;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;

/**
 * 由 Shell 管理的进程接口。
 *
 * <p>此接口定义了 Arthas Shell 中进程的基本行为和生命周期管理。
 * 进程可以是用户执行的命令、任务或其他可执行单元。每个进程都有：
 * <ul>
 *   <li>执行状态（运行中、已终止等）</li>
 *   <li>退出码（终止时）</li>
 *   <li>关联的 TTY（终端）</li>
 *   <li>关联的会话（Session）</li>
 *   <li>前后台切换能力</li>
 *   <li>暂停、恢复、中断等控制能力</li>
 * </ul>
 *
 * <p>进程支持多种控制操作：
 * <ul>
 *   <li>run() - 启动进程执行</li>
 *   <li>interrupt() - 中断进程（发送中断信号）</li>
 *   <li>suspend() - 暂停进程</li>
 *   <li>resume() - 恢复进程</li>
 *   <li>terminate() - 终止进程</li>
 *   <li>toBackground/toForeground() - 在前后台之间切换</li>
 * </ul>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Process {
    /**
     * 获取当前进程的执行状态。
     *
     * @return 当前的进程状态，如 RUNNING、TERMINATED 等
     */
    ExecStatus status();

    /**
     * 获取进程的退出码。
     *
     * @return 当进程状态为 {@link ExecStatus#TERMINATED} 时返回退出码，
     *         如果进程还在运行则返回 {@code null}
     */
    Integer exitCode();

    /**
     * 设置进程的 TTY（终端）。
     * TTY 用于进程与用户之间的交互，包括输入输出处理。
     *
     * @param tty 要设置的终端对象
     * @return 此对象（支持链式调用）
     */
    Process setTty(Tty tty);

    /**
     * 获取进程的 TTY（终端）。
     *
     * @return 进程关联的终端对象
     */
    Tty getTty();

    /**
     * 设置进程的会话。
     * 会话管理进程的执行环境和上下文信息。
     *
     * @param session 要设置的会话对象
     * @return 此对象（支持链式调用）
     */
    Process setSession(Session session);

    /**
     * 获取进程的会话。
     *
     * @return 进程关联的会话对象
     */
    Session getSession();

    /**
     * 设置进程终止时的回调处理器。
     * 当进程终止时，将调用此处理器并传入退出码。
     *
     * @param handler 进程终止时调用的处理器，接收进程的退出码作为参数
     * @return 此对象（支持链式调用）
     */
    Process terminatedHandler(Handler<Integer> handler);

    /**
     * 运行进程。
     * 在前台启动进程的执行。
     */
    void run();

    /**
     * 运行进程并指定前台或后台模式。
     *
     * @param foreground 如果为 true，进程在前台运行；如果为 false，进程在后台运行
     */
    void run(boolean foreground);

    /**
     * 尝试中断进程。
     * 向进程发送中断信号（类似于 Ctrl+C）。
     *
     * @return 如果进程捕获了信号则返回 true
     */
    boolean interrupt();

    /**
     * 尝试中断进程并在完成后调用处理器。
     * 向进程发送中断信号，操作完成后调用指定的处理器。
     *
     * @param completionHandler 中断操作完成后调用的处理器
     * @return 如果进程捕获了信号则返回 true
     */
    boolean interrupt(Handler<Void> completionHandler);

    /**
     * 恢复进程。
     * 恢复之前暂停的进程，默认在前台运行。
     */
    void resume();

    /**
     * 恢复进程并指定前台或后台模式。
     *
     * @param foreground 如果为 true，在前台恢复；如果为 false，在后台恢复
     */
    void resume(boolean foreground);

    /**
     * 恢复进程并在完成后调用处理器。
     *
     * @param completionHandler 恢复操作完成后调用的处理器
     */
    void resume(Handler<Void> completionHandler);

    /**
     * 恢复进程并指定模式，完成后调用处理器。
     *
     * @param foreground 如果为 true，在前台恢复；如果为 false，在后台恢复
     * @param completionHandler 恢复操作完成后调用的处理器
     */
    void resume(boolean foreground, Handler<Void> completionHandler);

    /**
     * 暂停进程。
     * 将正在运行的进程挂起，暂停其执行。
     * 注意：方法名是 suspend，但注释写的是 Resume，这是原文档的笔误。
     */
    void suspend();

    /**
     * 暂停进程并在完成后调用处理器。
     *
     * @param completionHandler 暂停操作完成后调用的处理器
     */
    void suspend(Handler<Void> completionHandler);

    /**
     * 终止进程。
     * 强制结束进程的执行，释放相关资源。
     */
    void terminate();

    /**
     * 终止进程并在完成后调用处理器。
     *
     * @param completionHandler 终止操作完成后调用的处理器
     */
    void terminate(Handler<Void> completionHandler);

    /**
     * 将进程切换到后台运行。
     * 进程将在后台继续执行，不会阻塞当前终端。
     */
    void toBackground();

    /**
     * 将进程切换到后台运行并在完成后调用处理器。
     *
     * @param completionHandler 切换操作完成后调用的处理器
     */
    void toBackground(Handler<Void> completionHandler);

    /**
     * 将进程切换到前台运行。
     * 进程将占用当前终端，用户可以与其交互。
     */
    void toForeground();

    /**
     * 将进程切换到前台运行并在完成后调用处理器。
     *
     * @param completionHandler 切换操作完成后调用的处理器
     */
    void toForeground(Handler<Void> completionHandler);

    /**
     * 获取进程的执行次数。
     * 表示进程被执行或重启的次数。
     *
     * @return 进程的执行次数
     */
    int times();

    /**
     * 获取进程的开始时间。
     *
     * @return 进程开始执行的时间戳
     */
    Date startTime();

    /**
     * 获取缓存文件的位置。
     * 进程可能会将一些数据缓存到文件中，此方法返回缓存文件的路径。
     *
     * @return 缓存文件的位置路径
     */
    String cacheLocation();

    /**
     * 设置作业 ID。
     * 用于标识和管理后台作业。
     *
     * @param jobId 要设置的作业 ID
     */
    void setJobId(int jobId);
}

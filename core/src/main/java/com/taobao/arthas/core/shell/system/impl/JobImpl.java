package com.taobao.arthas.core.shell.system.impl;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.Process;

/**
 * Job接口的实现类，用于管理作业的生命周期
 *
 * Job表示一个可以在前台或后台运行的命令执行任务
 * 它负责管理进程的状态转换、前后台切换、超时控制等
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-05-14
 * @author gongdewei 2020-03-23
 */
public class JobImpl implements Job {

    /** 作业的唯一标识ID */
    final int id;

    /** 作业控制器，负责管理所有作业的创建和销毁 */
    final JobControllerImpl controller;

    /** 作业对应的进程对象 */
    final Process process;

    /** 作业的命令行文本 */
    final String line;

    /** 作业所属的会话 */
    private volatile Session session;

    /** 作业的实际执行状态（仅用于内部测试） */
    private volatile ExecStatus actualStatus; // Used internally for testing only

    /** 作业最后一次停止的时间戳 */
    volatile long lastStopped; // When the job was last stopped

    /** 作业事件监听器，用于处理作业的生命周期事件 */
    volatile JobListener jobHandler;

    /** 状态更新处理器，当作业状态变化时被调用 */
    volatile Handler<ExecStatus> statusUpdateHandler;

    /** 作业的超时日期 */
    volatile Date timeoutDate;

    /** 终止操作的Future对象，用于异步等待作业完成 */
    final Future<Void> terminateFuture;

    /** 标识作业是否在后台运行 */
    final AtomicBoolean runInBackground;

    // 前台更新处理器（已废弃）
    //final Handler<Job> foregroundUpdatedHandler;

    /**
     * 构造函数，创建一个新的Job实例
     *
     * @param id 作业的唯一标识ID
     * @param controller 作业控制器
     * @param process 作业对应的进程对象
     * @param line 命令行文本
     * @param runInBackground 是否在后台运行
     * @param session 所属的会话
     * @param jobHandler 作业事件监听器
     */
    JobImpl(int id, final JobControllerImpl controller, Process process, String line, boolean runInBackground,
            Session session, JobListener jobHandler) {
        this.id = id;
        this.controller = controller;
        this.process = process;
        this.line = line;
        this.session = session;
        this.terminateFuture = Future.future();
        this.runInBackground = new AtomicBoolean(runInBackground);
        this.jobHandler = jobHandler;
        // 验证JobListener不能为空
        if (jobHandler == null) {
            throw new IllegalArgumentException("JobListener is required");
        }
        // 注册进程终止处理器
        //this.foregroundUpdatedHandler = new ShellForegroundUpdateHandler(shell);
        process.terminatedHandler(new TerminatedHandler(controller));
    }

    /**
     * 获取作业的实际执行状态
     *
     * @return 当前执行状态
     */
    public ExecStatus actualStatus() {
        return actualStatus;
    }

    /**
     * 中断当前作业的执行
     *
     * @return 是否成功中断
     */
    @Override
    public boolean interrupt() {
        return process.interrupt();
    }

    /**
     * 恢复作业执行（默认在前台恢复）
     *
     * @return 当前Job实例
     */
    @Override
    public Job resume() {
        return resume(true);
    }

    /**
     * 获取作业的超时时间
     *
     * @return 超时日期
     */
    @Override
    public Date timeoutDate() {
        return timeoutDate;
    }

    /**
     * 设置作业的超时时间
     *
     * @param date 超时日期
     */
    @Override
    public void setTimeoutDate(Date date) {
        this.timeoutDate = date;
    }

    /**
     * 获取作业所属的会话
     *
     * @return 会话对象
     */
    @Override
    public Session getSession() {
        return session;
    }

    /**
     * 恢复作业执行
     *
     * @param foreground true表示在前台恢复，false表示在后台恢复
     * @return 当前Job实例
     */
    @Override
    public Job resume(boolean foreground) {
        try {
            // 尝试恢复进程执行
            process.resume(foreground, new ResumeHandler());
        } catch (IllegalStateException ignore) {
            // 如果状态不允许恢复，则忽略异常
        }

        // 更新后台运行标志
        runInBackground.set(!foreground);

//        if (foreground) {
//            if (foregroundUpdatedHandler != null) {
//                foregroundUpdatedHandler.handle(this);
//            }
//        }
        // 通知状态更新
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(process.status());
        }

        // 如果作业正在运行，触发相应的前台或后台事件
        if (this.status() == ExecStatus.RUNNING) {
            if (foreground) {
                jobHandler.onForeground(this);
            } else {
                jobHandler.onBackground(this);
            }
        }
        return this;
    }

    /**
     * 挂起作业的执行
     *
     * @return 当前Job实例
     */
    @Override
    public Job suspend() {
        try {
            // 尝试挂起进程
            process.suspend(new SuspendHandler());
        } catch (IllegalStateException ignore) {
            // 如果状态不允许挂起，直接返回
            return this;
        }
//        if (!runInBackground.get() && foregroundUpdatedHandler != null) {
//            foregroundUpdatedHandler.handle(null);
//        }
        // 通知状态更新
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(process.status());
        }

//        shell.setForegroundJob(null);
        // 触发挂起事件
        jobHandler.onSuspend(this);
        return this;
    }

    /**
     * 终止作业的执行
     * 从控制器中移除该作业
     */
    @Override
    public void terminate() {
        try {
            // 终止进程
            process.terminate();
        } catch (IllegalStateException ignore) {
            // 进程可能已经自行终止，忽略异常
            // Process already terminated, likely by itself
        } finally {
            // 从控制器中移除该作业
            controller.removeJob(this.id);
        }
    }

    /**
     * 获取作业关联的进程对象
     *
     * @return 进程对象
     */
    @Override
    public Process process() {
        return process;
    }

    /**
     * 获取作业的当前状态
     *
     * @return 执行状态
     */
    public ExecStatus status() {
        return process.status();
    }

    /**
     * 获取作业的命令行文本
     *
     * @return 命令行文本
     */
    public String line() {
        return line;
    }

    /**
     * 判断作业是否在后台运行
     *
     * @return true表示在后台运行，false表示在前台运行
     */
    @Override
    public boolean isRunInBackground() {
        return runInBackground.get();
    }

    /**
     * 将作业切换到后台运行
     *
     * @return 当前Job实例
     */
    @Override
    public Job toBackground() {
        if (!this.runInBackground.get()) {
            // 当前在前台模式运行
            if (runInBackground.compareAndSet(false, true)) {
                // 使用CAS原子操作切换到后台
                process.toBackground();
                // 通知状态更新
                if (statusUpdateHandler != null) {
                    statusUpdateHandler.handle(process.status());
                }
                // 触发后台事件
                jobHandler.onBackground(this);
            }
        }

//        shell.setForegroundJob(null);
//        jobHandler.onBackground(this);
        return this;
    }

    /**
     * 将作业切换到前台运行
     *
     * @return 当前Job实例
     */
    @Override
    public Job toForeground() {
        if (this.runInBackground.get()) {
            // 当前在后台模式运行
            if (runInBackground.compareAndSet(true, false)) {
                // 使用CAS原子操作切换到前台
//                if (foregroundUpdatedHandler != null) {
//                    foregroundUpdatedHandler.handle(this);
//                }
                process.toForeground();
                // 通知状态更新
                if (statusUpdateHandler != null) {
                    statusUpdateHandler.handle(process.status());
                }

//                shell.setForegroundJob(this);
                // 触发前台事件
                jobHandler.onForeground(this);
            }
        }

        return this;
    }

    /**
     * 获取作业的ID
     *
     * @return 作业ID
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * 运行作业（根据当前后台标志决定前台或后台）
     *
     * @return 当前Job实例
     */
    @Override
    public Job run() {
        return run(!runInBackground.get());
    }

    /**
     * 运行作业
     *
     * @param foreground true表示在前台运行，false表示在后台运行
     * @return 当前Job实例
     */
    @Override
    public Job run(boolean foreground) {
//        if (foreground && foregroundUpdatedHandler != null) {
//            foregroundUpdatedHandler.handle(this);
//        }

        // 设置实际状态为运行中
        actualStatus = ExecStatus.RUNNING;
        // 通知状态更新
        if (statusUpdateHandler != null) {
            statusUpdateHandler.handle(ExecStatus.RUNNING);
        }
        // 进程的tty在JobControllerImpl.createCommandProcess中设置
        //process.setTty(shell.term());
        // 设置进程所属的会话
        process.setSession(this.session);
        // 启动进程
        process.run(foreground);

//        if (!foreground && foregroundUpdatedHandler != null) {
//            foregroundUpdatedHandler.handle(null);
//        }
//
//        if (foreground) {
//            shell.setForegroundJob(this);
//        } else {
//            shell.setForegroundJob(null);
//        }
        // 如果作业正在运行，触发相应的前台或后台事件
        if (this.status() == ExecStatus.RUNNING) {
            if (foreground) {
                jobHandler.onForeground(this);
            } else {
                jobHandler.onBackground(this);
            }
        }
        return this;
    }

    /**
     * 进程终止处理器
     * 当进程终止时，从控制器中移除作业并通知相关监听器
     */
    private class TerminatedHandler implements Handler<Integer> {

        /** 作业控制器引用 */
        private final JobControllerImpl controller;

        /**
         * 构造函数
         *
         * @param controller 作业控制器
         */
        public TerminatedHandler(JobControllerImpl controller) {
            this.controller = controller;
        }

        /**
         * 处理进程终止事件
         *
         * @param exitCode 进程退出码
         */
        @Override
        public void handle(Integer exitCode) {
//            if (!runInBackground.get() && actualStatus.equals(ExecStatus.RUNNING)) {
                // 只有前台在运行的任务，才需要调用foregroundUpdateHandler
//                if (foregroundUpdatedHandler != null) {
//                    foregroundUpdatedHandler.handle(null);
//                }
//            }
            // 触发终止事件
            jobHandler.onTerminated(JobImpl.this);
            // 从控制器中移除作业
            controller.removeJob(JobImpl.this.id);
            // 通知状态更新
            if (statusUpdateHandler != null) {
                statusUpdateHandler.handle(ExecStatus.TERMINATED);
            }
            // 完成终止Future
            terminateFuture.complete();

            // 保存命令历史（已移至JobControllerImpl.ShellJobHandler.onTerminated）
//            Term term = shell.term();
//            if (term instanceof TermImpl) {
//                List<int[]> history = ((TermImpl) term).getReadline().getHistory();
//                FileUtils.saveCommandHistory(history, new File(Constants.CMD_HISTORY_FILE));
//            }
        }
    }

    /**
     * 进程恢复处理器
     * 当进程恢复执行时更新实际状态
     */
    private class ResumeHandler implements Handler<Void> {

        /**
         * 处理进程恢复事件
         *
         * @param event 事件对象（未使用）
         */
        @Override
        public void handle(Void event) {
            // 更新实际状态为运行中
            actualStatus = ExecStatus.RUNNING;
        }
    }

    /**
     * 进程挂起处理器
     * 当进程挂起时更新实际状态
     */
    private class SuspendHandler implements Handler<Void> {

        /**
         * 处理进程挂起事件
         *
         * @param event 事件对象（未使用）
         */
        @Override
        public void handle(Void event) {
            // 更新实际状态为已停止
            actualStatus = ExecStatus.STOPPED;
        }
    }
}

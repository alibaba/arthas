package com.taobao.arthas.core.shell.system.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.command.basic1000.HelpCommand;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.impl.TermResultDistributorImpl;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.internal.CloseFunction;
import com.taobao.arthas.core.shell.command.internal.StatisticsFunction;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.ProcessAware;
import com.taobao.arthas.core.shell.term.Tty;
import com.taobao.middleware.cli.CLIException;
import com.taobao.middleware.cli.CommandLine;
import io.termd.core.function.Function;

import java.lang.instrument.ClassFileTransformer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process接口的实现类，用于管理命令进程的执行
 *
 * Process代表一个正在执行的命令进程，负责管理进程的状态、输入输出、
 * 前后台切换、生命周期事件等
 *
 * @author beiwei30 on 10/11/2016.
 * @author gongdewei 2020-03-26
 */
public class ProcessImpl implements Process {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(ProcessImpl.class);

    /** 命令上下文，包含命令的CLI配置等信息 */
    private Command commandContext;

    /** 命令处理器，负责执行具体的命令逻辑 */
    private Handler<CommandProcess> handler;

    /** 命令参数列表（Token形式） */
    private List<CliToken> args;

    /** 终端设备，用于输入输出 */
    private Tty tty;

    /** 进程所属的会话 */
    private Session session;

    /** 中断处理器，当进程被中断时调用 */
    private Handler<Void> interruptHandler;

    /** 挂起处理器，当进程被挂起时调用 */
    private Handler<Void> suspendHandler;

    /** 恢复处理器，当进程被恢复时调用 */
    private Handler<Void> resumeHandler;

    /** 结束处理器，当进程结束时调用 */
    private Handler<Void> endHandler;

    /** 后台处理器，当进程切换到后台时调用 */
    private Handler<Void> backgroundHandler;

    /** 前台处理器，当进程切换到前台时调用 */
    private Handler<Void> foregroundHandler;

    /** 终止处理器，当进程终止时调用 */
    private Handler<Integer> terminatedHandler;

    /** 标识进程是否在前台运行 */
    private boolean foreground;

    /** 进程的执行状态（线程安全） */
    private volatile ExecStatus processStatus;

    /** 标识进程实际是否在前台 */
    private boolean processForeground;

    /** 标准输入处理器 */
    private Handler<String> stdinHandler;

    /** 终端大小调整处理器 */
    private Handler<Void> resizeHandler;

    /** 进程退出码 */
    private Integer exitCode;

    /** 命令进程实现对象 */
    private CommandProcessImpl process;

    /** 进程开始时间 */
    private Date startTime;

    /** 进程输出处理器 */
    private ProcessOutput processOutput;

    /** 作业ID */
    private int jobId;

    /** 结果分发器，用于分发命令执行结果 */
    private ResultDistributor resultDistributor;

    /**
     * 构造函数，创建一个新的Process实例
     *
     * @param commandContext 命令上下文
     * @param args 命令参数列表
     * @param handler 命令处理器
     * @param processOutput 进程输出处理器
     * @param resultDistributor 结果分发器
     */
    public ProcessImpl(Command commandContext, List<CliToken> args, Handler<CommandProcess> handler,
                       ProcessOutput processOutput, ResultDistributor resultDistributor) {
        this.commandContext = commandContext;
        this.handler = handler;
        this.args = args;
        this.resultDistributor = resultDistributor;
        // 初始化进程状态为就绪
        this.processStatus = ExecStatus.READY;
        this.processOutput = processOutput;
    }

    /**
     * 获取进程的退出码
     *
     * @return 退出码，如果进程未结束则返回null
     */
    @Override
    public Integer exitCode() {
        return exitCode;
    }

    /**
     * 获取进程的当前状态
     *
     * @return 进程状态
     */
    @Override
    public ExecStatus status() {
        return processStatus;
    }

    /**
     * 设置终端设备
     *
     * @param tty 终端对象
     * @return 当前Process实例
     */
    @Override
    public synchronized Process setTty(Tty tty) {
        this.tty = tty;
        return this;
    }

    /**
     * 获取终端设备
     *
     * @return 终端对象
     */
    @Override
    public synchronized Tty getTty() {
        return tty;
    }

    /**
     * 设置作业ID
     *
     * @param jobId 作业ID
     */
    @Override
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    /**
     * 设置进程所属的会话
     *
     * @param session 会话对象
     * @return 当前Process实例
     */
    @Override
    public synchronized Process setSession(Session session) {
        this.session = session;
        return this;
    }

    /**
     * 获取进程所属的会话
     *
     * @return 会话对象
     */
    @Override
    public synchronized Session getSession() {
        return session;
    }

    /**
     * 获取进程的执行次数
     *
     * @return 执行次数
     */
    @Override
    public int times() {
        return process.times().get();
    }

    /**
     * 获取进程的开始时间
     *
     * @return 开始时间
     */
    public Date startTime() {
        return startTime;
    }

    /**
     * 获取缓存位置
     *
     * @return 缓存文件路径，如果没有缓存则返回null
     */
    @Override
    public String cacheLocation() {
        if (processOutput != null) {
            return processOutput.cacheLocation;
        }
        return null;
    }

    /**
     * 设置进程终止处理器
     *
     * @param handler 终止处理器
     * @return 当前Process实例
     */
    @Override
    public Process terminatedHandler(Handler<Integer> handler) {
        terminatedHandler = handler;
        return this;
    }

    /**
     * 中断进程执行
     *
     * @return 是否成功中断
     */
    @Override
    public boolean interrupt() {
        return interrupt(null);
    }

    /**
     * 中断进程执行
     *
     * @param completionHandler 完成处理器
     * @return 是否成功中断
     */
    @Override
    public boolean interrupt(final Handler<Void> completionHandler) {
        // 只有运行中、已停止或已终止的进程才能被中断
        if (processStatus == ExecStatus.RUNNING || processStatus == ExecStatus.STOPPED || processStatus == ExecStatus.TERMINATED) {
            final Handler<Void> handler = interruptHandler;
            try {
                // 调用中断处理器
                if (handler != null) {
                    handler.handle(null);
                }
            } finally {
                // 调用完成处理器
                if (completionHandler != null) {
                    completionHandler.handle(null);
                }
            }
            return handler != null;
        } else {
            throw new IllegalStateException("Cannot interrupt process in " + processStatus + " state");
        }
    }

    /**
     * 恢复进程执行（默认在前台恢复）
     */
    @Override
    public void resume() {
        resume(true);
    }

    /**
     * 恢复进程执行
     *
     * @param foreground true表示在前台恢复，false表示在后台恢复
     */
    @Override
    public void resume(boolean foreground) {
        resume(foreground, null);
    }

    /**
     * 恢复进程执行（默认在前台恢复）
     *
     * @param completionHandler 完成处理器
     */
    @Override
    public void resume(Handler<Void> completionHandler) {
        resume(true, completionHandler);
    }

    /**
     * 恢复进程执行
     *
     * @param fg true表示在前台恢复，false表示在后台恢复
     * @param completionHandler 完成处理器
     */
    @Override
    public synchronized void resume(boolean fg, Handler<Void> completionHandler) {
        // 只有已停止的进程才能被恢复
        if (processStatus == ExecStatus.STOPPED) {
            // 更新状态为运行中
            updateStatus(ExecStatus.RUNNING, null, fg, resumeHandler, terminatedHandler, completionHandler);
            if (process != null) {
                process.resume();
            }
        } else {
            throw new IllegalStateException("Cannot resume process in " + processStatus + " state");
        }
    }

    /**
     * 挂起进程
     */
    @Override
    public void suspend() {
        suspend(null);
    }

    /**
     * 挂起进程
     *
     * @param completionHandler 完成处理器
     */
    @Override
    public synchronized void suspend(Handler<Void> completionHandler) {
        // 只有运行中的进程才能被挂起
        if (processStatus == ExecStatus.RUNNING) {
            // 更新状态为已停止
            updateStatus(ExecStatus.STOPPED, null, false, suspendHandler, terminatedHandler, completionHandler);
            if (process != null) {
                process.suspend();
            }
        } else {
            throw new IllegalStateException("Cannot suspend process in " + processStatus + " state");
        }
    }

    /**
     * 将进程切换到后台
     */
    @Override
    public void toBackground() {
        toBackground(null);
    }

    /**
     * 将进程切换到后台
     *
     * @param completionHandler 完成处理器
     */
    @Override
    public void toBackground(Handler<Void> completionHandler) {
        // 只有运行中的进程才能切换到后台
        if (processStatus == ExecStatus.RUNNING) {
            // 只有在前台的进程才需要切换
            if (processForeground) {
                updateStatus(ExecStatus.RUNNING, null, false, backgroundHandler, terminatedHandler, completionHandler);
            }
        } else {
            throw new IllegalStateException("Cannot set to background a process in " + processStatus + " state");
        }
    }

    /**
     * 将进程切换到前台
     */
    @Override
    public void toForeground() {
        toForeground(null);
    }

    /**
     * 将进程切换到前台
     *
     * @param completionHandler 完成处理器
     */
    @Override
    public void toForeground(Handler<Void> completionHandler) {
        // 只有运行中的进程才能切换到前台
        if (processStatus == ExecStatus.RUNNING) {
            // 只有在后台的进程才需要切换
            if (!processForeground) {
                updateStatus(ExecStatus.RUNNING, null, true, foregroundHandler, terminatedHandler, completionHandler);
            }
        } else {
            throw new IllegalStateException("Cannot set to foreground a process in " + processStatus + " state");
        }
    }

    /**
     * 终止进程
     */
    @Override
    public void terminate() {
        terminate(null);
    }

    /**
     * 终止进程
     *
     * @param completionHandler 完成处理器
     */
    @Override
    public void terminate(Handler<Void> completionHandler) {
        if (!terminate(-10, completionHandler, null)) {
            throw new IllegalStateException("Cannot terminate terminated process");
        }
    }

    /**
     * 内部终止方法
     *
     * @param exitCode 退出码
     * @param completionHandler 完成处理器
     * @param message 附加消息
     * @return 是否成功终止
     */
    private synchronized boolean terminate(int exitCode, Handler<Void> completionHandler, String message) {
        // 只有未终止的进程才能被终止
        if (processStatus != ExecStatus.TERMINATED) {
            // 添加状态消息
            this.appendResult(new StatusModel(exitCode, message));
            if (process != null) {
                processOutput.close();
            }
            // 更新状态为已终止
            updateStatus(ExecStatus.TERMINATED, exitCode, false, endHandler, terminatedHandler, completionHandler);
            if (process != null) {
                // 注销进程
                process.unregister();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 添加结果到结果分发器
     *
     * @param result 结果对象
     */
    private void appendResult(ResultModel result) {
        result.setJobId(jobId);
        if (resultDistributor != null) {
            resultDistributor.appendResult(result);
        }
    }

    /**
     * 更新进程状态
     *
     * @param statusUpdate 新的状态
     * @param exitCodeUpdate 退出码
     * @param foregroundUpdate 是否前台运行
     * @param handler 状态处理器
     * @param terminatedHandler 终止处理器
     * @param completionHandler 完成处理器
     */
    private void updateStatus(ExecStatus statusUpdate, Integer exitCodeUpdate, boolean foregroundUpdate,
                              Handler<Void> handler, Handler<Integer> terminatedHandler,
                              Handler<Void> completionHandler) {
        // 更新进程状态
        processStatus = statusUpdate;
        exitCode = exitCodeUpdate;

        // 处理前台/后台切换
        if (!foregroundUpdate) {
            // 切换到后台
            if (processForeground) {
                processForeground = false;
                // 取消终端输入处理
                if (stdinHandler != null) {
                    tty.stdinHandler(null);
                }
                // 取消终端大小调整处理
                if (resizeHandler != null) {
                    tty.resizehandler(null);
                }
            }
        } else {
            // 切换到前台
            if (!processForeground) {
                processForeground = true;
                // 恢复终端输入处理
                if (stdinHandler != null) {
                    tty.stdinHandler(stdinHandler);
                }
                // 恢复终端大小调整处理
                if (resizeHandler != null) {
                    tty.resizehandler(resizeHandler);
                }
            }
        }

        foreground = foregroundUpdate;
        try {
            // 调用状态处理器
            if (handler != null) {
                handler.handle(null);
            }
        } finally {
            // 调用完成处理器
            if (completionHandler != null) {
                completionHandler.handle(null);
            }
            // 如果状态为已终止，调用终止处理器
            if (terminatedHandler != null && statusUpdate == ExecStatus.TERMINATED) {
                terminatedHandler.handle(exitCodeUpdate);
            }
        }
    }

    /**
     * 运行进程（默认在前台运行）
     */
    @Override
    public void run() {
        run(true);
    }

    /**
     * 运行进程
     *
     * @param fg true表示在前台运行，false表示在后台运行
     */
    @Override
    public synchronized void run(boolean fg) {
        // 只有就绪状态的进程才能运行
        if (processStatus != ExecStatus.READY) {
            throw new IllegalStateException("Cannot run proces in " + processStatus + " state");
        }

        // 更新进程状态
        processStatus = ExecStatus.RUNNING;
        processForeground = fg;
        foreground = fg;
        startTime = new Date();

        // 创建本地副本，避免并发问题
        // Make a local copy
        final Tty tty = this.tty;
        if (tty == null) {
            throw new IllegalStateException("Cannot execute process without a TTY set");
        }

        // 创建命令进程实现对象
        process = new CommandProcessImpl(this, tty);
        // 如果结果分发器未设置，使用默认的终端结果分发器
        if (resultDistributor == null) {
            resultDistributor = new TermResultDistributorImpl(process, ArthasBootstrap.getInstance().getResultViewResolver());
        }

        // 提取文本类型的参数
        final List<String> args2 = new LinkedList<String>();
        for (CliToken arg : args) {
            if (arg.isText()) {
                args2.add(arg.value());
            }
        }

        // 解析命令行参数
        CommandLine cl = null;
        try {
            if (commandContext.cli() != null) {
                // 检查是否请求帮助信息
                if (commandContext.cli().parse(args2, false).isAskingForHelp()) {
                    appendResult(new HelpCommand().createHelpDetailModel(commandContext));
                    terminate();
                    return;
                }

                // 解析命令行
                cl = commandContext.cli().parse(args2);
                process.setArgs2(args2);
                process.setCommandLine(cl);
            }
        } catch (CLIException e) {
            // 解析失败，终止进程
            terminate(-10, null, e.getMessage());
            return;
        }

        // 如果有缓存位置，输出提示信息
        if (cacheLocation() != null) {
            process.echoTips("job id  : " + this.jobId + "\n");
            process.echoTips("cache location  : " + cacheLocation() + "\n");
        }
        // 创建命令处理任务并提交执行
        Runnable task = new CommandProcessTask(process);
        ArthasBootstrap.getInstance().execute(task);
    }

    /**
     * 命令处理任务
     * 封装命令的执行逻辑，提交到线程池中异步执行
     */
    private class CommandProcessTask implements Runnable {

        /** 命令进程对象 */
        private CommandProcess process;

        /**
         * 构造函数
         *
         * @param process 命令进程对象
         */
        public CommandProcessTask(CommandProcess process) {
            this.process = process;
        }

        /**
         * 执行命令处理任务
         */
        @Override
        public void run() {
            try {
                // 调用命令处理器执行命令
                handler.handle(process);
            } catch (Throwable t) {
                // 捕获所有异常，记录日志并结束进程
                logger.error("Error during processing the command:", t);
                process.end(1, "Error during processing the command: " + t.getClass().getName() + ", message:" + t.getMessage()
                        + ", please check $HOME/logs/arthas/arthas.log for more details." );
            }
        }
    }

    /**
     * 命令进程实现类
     * 实现CommandProcess接口，提供命令执行过程中的各种操作
     */
    private class CommandProcessImpl implements CommandProcess {

        /** 关联的进程对象 */
        private final Process process;

        /** 终端设备 */
        private final Tty tty;

        /** 命令参数列表（字符串形式） */
        private List<String> args2;

        /** 命令行对象，包含解析后的参数和选项 */
        private CommandLine commandLine;

        /** 执行次数计数器 */
        private AtomicInteger times = new AtomicInteger();

        /** 增强监听器，用于监听方法的增强事件 */
        private AdviceListener listener = null;

        /** 类文件转换器，用于字节码增强 */
        private ClassFileTransformer transformer;

        /**
         * 构造函数
         *
         * @param process 进程对象
         * @param tty 终端设备
         */
        public CommandProcessImpl(Process process, Tty tty) {
            this.process = process;
            this.tty = tty;
        }

        /**
         * 获取命令参数的Token列表
         *
         * @return Token列表
         */
        @Override
        public List<CliToken> argsTokens() {
            return args;
        }

        /**
         * 获取命令参数列表
         *
         * @return 参数列表
         */
        @Override
        public List<String> args() {
            return args2;
        }

        /**
         * 获取终端类型
         *
         * @return 终端类型
         */
        @Override
        public String type() {
            return tty.type();
        }

        /**
         * 判断是否在前台运行
         *
         * @return true表示前台运行
         */
        @Override
        public boolean isForeground() {
            return foreground;
        }

        /**
         * 获取终端宽度
         *
         * @return 终端宽度（字符数）
         */
        @Override
        public int width() {
            return tty.width();
        }

        /**
         * 获取终端高度
         *
         * @return 终端高度（字符数）
         */
        @Override
        public int height() {
            return tty.height();
        }

        /**
         * 获取命令行对象
         *
         * @return 命令行对象
         */
        @Override
        public CommandLine commandLine() {
            return commandLine;
        }

        /**
         * 获取会话对象
         *
         * @return 会话对象
         */
        @Override
        public Session session() {
            return session;
        }

        /**
         * 获取执行次数计数器
         *
         * @return 计数器
         */
        @Override
        public AtomicInteger times() {
            return times;
        }

        /**
         * 设置命令参数列表
         *
         * @param args2 参数列表
         */
        public void setArgs2(List<String> args2) {
            this.args2 = args2;
        }

        /**
         * 设置命令行对象
         *
         * @param commandLine 命令行对象
         */
        public void setCommandLine(CommandLine commandLine) {
            this.commandLine = commandLine;
        }

        /**
         * 设置标准输入处理器
         *
         * @param handler 输入处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess stdinHandler(Handler<String> handler) {
            stdinHandler = handler;
            // 只有在前台运行时才设置输入处理器
            if (processForeground && stdinHandler != null) {
                tty.stdinHandler(stdinHandler);
            }
            return this;
        }

        /**
         * 写入数据到标准输出
         *
         * @param data 要写入的数据
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess write(String data) {
            // 只有运行中的进程才能写入输出
            if (processStatus != ExecStatus.RUNNING) {
                throw new IllegalStateException(
                        "Cannot write to standard output when " + status().name().toLowerCase());
            }
            processOutput.write(data);
            return this;
        }

        /**
         * 输出提示信息到终端
         *
         * @param tips 提示信息
         */
        @Override
        public void echoTips(String tips) {
            processOutput.term.write(tips);
        }

        /**
         * 获取缓存位置
         *
         * @return 缓存文件路径
         */
        @Override
        public String cacheLocation() {
            return ProcessImpl.this.cacheLocation();
        }

        /**
         * 设置终端大小调整处理器
         *
         * @param handler 调整处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess resizehandler(Handler<Void> handler) {
            resizeHandler = handler;
            tty.resizehandler(resizeHandler);
            return this;
        }

        /**
         * 设置中断处理器
         *
         * @param handler 中断处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess interruptHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                interruptHandler = handler;
            }
            return this;
        }

        /**
         * 设置挂起处理器
         *
         * @param handler 挂起处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess suspendHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                suspendHandler = handler;
            }
            return this;
        }

        /**
         * 设置恢复处理器
         *
         * @param handler 恢复处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess resumeHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                resumeHandler = handler;
            }
            return this;
        }

        /**
         * 设置结束处理器
         *
         * @param handler 结束处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess endHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                endHandler = handler;
            }
            return this;
        }

        /**
         * 设置后台处理器
         *
         * @param handler 后台处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess backgroundHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                backgroundHandler = handler;
            }
            return this;
        }

        /**
         * 设置前台处理器
         *
         * @param handler 前台处理器
         * @return 当前CommandProcess实例
         */
        @Override
        public CommandProcess foregroundHandler(Handler<Void> handler) {
            synchronized (ProcessImpl.this) {
                foregroundHandler = handler;
            }
            return this;
        }

        /**
         * 注册增强监听器和类文件转换器
         * 用于字节码增强功能，如watch、trace等命令
         *
         * @param adviceListener 增强监听器
         * @param transformer 类文件转换器
         */
        @Override
        public void register(AdviceListener adviceListener, ClassFileTransformer transformer) {
            // 如果监听器是ProcessAware类型，设置其进程关联
            if (adviceListener instanceof ProcessAware) {
                ProcessAware processAware = (ProcessAware) adviceListener;
                // listener 有可能是其它 command 创建的
                if(processAware.getProcess() == null) {
                    processAware.setProcess(this.process);
                }
            }
            // 保存监听器引用
            this.listener = adviceListener;
            // 注册到AdviceWeaver
            AdviceWeaver.reg(listener);

            // 保存转换器引用
            this.transformer = transformer;
        }

        /**
         * 注销增强监听器和类文件转换器
         * 清理字节码增强相关的资源
         */
        @Override
        public void unregister() {
            // 移除类文件转换器
            if (transformer != null) {
                ArthasBootstrap.getInstance().getTransformerManager().removeTransformer(transformer);
            }

            // 注销增强监听器
            if (listener instanceof ProcessAware) {
                // listener有可能其它 command 创建的，所以不能unRge
                if (this.process.equals(((ProcessAware) listener).getProcess())) {
                    AdviceWeaver.unReg(listener);
                }
            } else {
                AdviceWeaver.unReg(listener);
            }
        }

        /**
         * 恢复进程执行
         */
        @Override
        public void resume() {
//            if (suspendedListener != null) {
//                AdviceWeaver.resume(suspendedListener);
//                suspendedListener = null;
//            }
        }

        /**
         * 挂起进程执行
         */
        @Override
        public void suspend() {
//            if (this.enhanceLock >= 0) {
//                suspendedListener = AdviceWeaver.suspend(enhanceLock);
//            }
        }

        /**
         * 结束进程（退出码为0）
         */
        @Override
        public void end() {
            end(0);
        }

        /**
         * 结束进程
         *
         * @param statusCode 退出码
         */
        @Override
        public void end(int statusCode) {
            end(statusCode, null);
        }

        /**
         * 结束进程
         *
         * @param statusCode 退出码
         * @param message 附加消息
         */
        @Override
        public void end(int statusCode, String message) {
            terminate(statusCode, null, message);
        }

        /**
         * 判断进程是否正在运行
         *
         * @return true表示正在运行
         */
        @Override
        public boolean isRunning() {
            return processStatus == ExecStatus.RUNNING;
        }

        /**
         * 添加结果到结果分发器
         *
         * @param result 结果对象
         */
        @Override
        public void appendResult(ResultModel result) {
            // 只有运行中的进程才能添加结果
            if (processStatus != ExecStatus.RUNNING) {
                throw new IllegalStateException(
                        "Cannot write to standard output when " + status().name().toLowerCase());
            }
            ProcessImpl.this.appendResult(result);
        }
    }

    /**
     * 进程输出处理器
     * 负责处理命令的标准输出，支持处理器链模式
     */
    static class ProcessOutput {

        /** 标准输出处理器链 */
        private List<Function<String, String>> stdoutHandlerChain;

        /** 统计处理器，用于收集命令执行的统计信息 */
        private StatisticsFunction statisticsHandler = null;

        /** 刷新处理器链，在关闭时执行 */
        private List<Function<String, String>> flushHandlerChain = null;

        /** 缓存位置 */
        private String cacheLocation;

        /** 终端设备 */
        private Tty term;

        /**
         * 构造函数
         *
         * @param stdoutHandlerChain 标准输出处理器链
         * @param cacheLocation 缓存位置
         * @param term 终端设备
         */
        public ProcessOutput(List<Function<String, String>> stdoutHandlerChain, String cacheLocation, Tty term) {
            // this.stdoutHandlerChain = stdoutHandlerChain;

            // 查找统计处理器的位置
            int i = 0;
            for (; i < stdoutHandlerChain.size(); i++) {
                if (stdoutHandlerChain.get(i) instanceof StatisticsFunction) {
                    break;
                }
            }
            // 将处理器链分为三部分：统计前、统计、统计后
            if (i < stdoutHandlerChain.size()) {
                this.stdoutHandlerChain = stdoutHandlerChain.subList(0, i + 1);
                this.statisticsHandler = (StatisticsFunction) stdoutHandlerChain.get(i);
                if (i < stdoutHandlerChain.size() - 1) {
                    flushHandlerChain = stdoutHandlerChain.subList(i + 1, stdoutHandlerChain.size());
                }
            } else {
                // 没有统计处理器，全部作为stdoutHandlerChain
                this.stdoutHandlerChain = stdoutHandlerChain;
            }

            this.cacheLocation = cacheLocation;
            this.term = term;
        }

        /**
         * 写入数据到处理器链
         * 数据会依次通过所有处理器进行处理
         *
         * @param data 要处理的数据
         */
        private void write(String data) {
            if (stdoutHandlerChain != null) {
                // 使用for循环而非foreach，减少内存碎片
                // hotspot, reduce memory fragment (foreach/iterator)
                int size = stdoutHandlerChain.size();
                for (int i = 0; i < size; i++) {
                    Function<String, String> function = stdoutHandlerChain.get(i);
                    data = function.apply(data);
                }
            }
        }

        /**
         * 关闭处理器链
         * 执行刷新处理器链和清理操作
         */
        private void close() {
            // 如果有统计处理器和刷新处理器链，执行刷新操作
            if (statisticsHandler != null && flushHandlerChain != null) {
                String data = statisticsHandler.result();

                // 依次执行刷新处理器
                for (Function<String, String> function : flushHandlerChain) {
                    data = function.apply(data);
                    if (function instanceof StatisticsFunction) {
                        data = ((StatisticsFunction) function).result();
                    }
                }
            }

            // 关闭所有支持关闭的处理器
            if (stdoutHandlerChain != null) {
                for (Function<String, String> function : stdoutHandlerChain) {
                    if (function instanceof CloseFunction) {
                        ((CloseFunction) function).close();
                    }
                }
            }
        }
    }
}

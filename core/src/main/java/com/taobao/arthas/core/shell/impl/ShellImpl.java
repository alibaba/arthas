package com.taobao.arthas.core.shell.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.security.AuthUtils;
import com.taobao.arthas.core.security.SecurityAuthenticator;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.Shell;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.shell.*;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.impl.SessionImpl;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.impl.TermImpl;
import com.taobao.arthas.core.shell.term.impl.http.ExtHttpTtyConnection;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.FileUtils;

import io.netty.channel.ChannelHandlerContext;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.telnet.netty.NettyTelnetConnection;
import io.termd.core.tty.TtyConnection;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * Shell会话实现类
 *
 * 从Shell服务器角度来看，代表一个客户端连接的Shell会话
 * 负责管理用户交互、命令执行、任务生命周期等核心功能
 *
 * 主要职责：
 * 1. 管理终端(Term)与会话(Session)的绑定关系
 * 2. 处理用户输入并创建相应的Job来执行命令
 * 3. 管理前台和后台任务的执行状态
 * 4. 处理会话的认证和鉴权
 * 5. 保存命令历史记录
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellImpl implements Shell {
    private static final Logger logger = LoggerFactory.getLogger(ShellImpl.class);

    /**
     * 安全认证器，用于处理用户登录和权限验证
     */
    private SecurityAuthenticator securityAuthenticator = ArthasBootstrap.getInstance().getSecurityAuthenticator();

    /**
     * 任务控制器，负责管理该Shell会话中的所有任务
     */
    private JobControllerImpl jobController;

    /**
     * Shell会话的唯一标识符
     */
    final String id;

    /**
     * Shell关闭的Future对象，用于异步通知Shell已关闭
     */
    final Future<Void> closedFuture;

    /**
     * 内部命令管理器，用于注册和解析命令
     */
    private InternalCommandManager commandManager;

    /**
     * 会话对象，存储会话相关的所有上下文信息
     */
    private Session session = new SessionImpl();

    /**
     * 终端对象，负责与用户进行交互（输入输出）
     */
    private Term term;

    /**
     * 欢迎消息，在用户连接时显示
     */
    private String welcome;

    /**
     * 当前正在前台运行的任务
     */
    private Job currentForegroundJob;

    /**
     * 命令提示符，显示在终端中提示用户输入
     */
    private String prompt;

    /**
     * 构造函数，创建Shell实例
     *
     * @param server Shell服务器实例
     * @param term 终端对象，用于与用户交互
     * @param commandManager 命令管理器
     * @param instrumentation Java Instrumentation实例
     * @param pid 当前JVM进程的PID
     * @param jobController 任务控制器
     */
    public ShellImpl(ShellServer server, Term term, InternalCommandManager commandManager,
            Instrumentation instrumentation, long pid, JobControllerImpl jobController) {
        // 处理Telnet和HTTP连接的特殊认证逻辑
        if (term instanceof TermImpl) {
            TermImpl termImpl = (TermImpl) term;
            TtyConnection conn = termImpl.getConn();
            // 处理telnet本地连接鉴权
            if (conn instanceof TelnetTtyConnection) {
                TelnetConnection telnetConnection = ((TelnetTtyConnection) conn).getTelnetConnection();
                if (telnetConnection instanceof NettyTelnetConnection) {
                    ChannelHandlerContext handlerContext = ((NettyTelnetConnection) telnetConnection)
                            .channelHandlerContext();
                    Principal principal = AuthUtils.localPrincipal(handlerContext);
                    if (principal != null) {
                        try {
                            // 尝试使用本地Principal进行登录认证
                            Subject subject = securityAuthenticator.login(principal);
                            if (subject != null) {
                                // 将认证成功的Subject放入会话中
                                session.put(ArthasConstants.SUBJECT_KEY, subject);
                            }
                        } catch (LoginException e) {
                            logger.error("local connection auth error", e);
                        }
                    }
                }
            }

            // 处理HTTP连接的会话信息传递
            if (conn instanceof ExtHttpTtyConnection) {
                // 传递http cookie 里的鉴权信息到新建立的session中
                ExtHttpTtyConnection extConn = (ExtHttpTtyConnection) conn;
                Map<String, Object> extSessions = extConn.extSessions();
                // 将HTTP连接的扩展会话信息复制到当前会话中
                for (Entry<String, Object> entry : extSessions.entrySet()) {
                    session.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // 将核心组件放入会话属性中，供命令使用
        session.put(Session.COMMAND_MANAGER, commandManager);
        session.put(Session.INSTRUMENTATION, instrumentation);
        session.put(Session.PID, pid);
        session.put(Session.SERVER, server);
        session.put(Session.TTY, term);
        // 生成唯一的会话ID
        this.id = UUID.randomUUID().toString();
        session.put(Session.ID, id);
        this.commandManager = commandManager;
        // 初始化关闭Future
        this.closedFuture = Future.future();
        this.term = term;
        this.jobController = jobController;

        // 将会话绑定到终端
        if (term != null) {
            term.setSession(session);
        }

        // 设置命令提示符
        this.setPrompt();
    }

    /**
     * 获取任务控制器
     *
     * @return 任务控制器实例
     */
    public JobController jobController() {
        return jobController;
    }

    /**
     * 获取当前Shell中的所有任务
     *
     * @return 任务集合
     */
    public Set<Job> jobs() {
        return jobController.jobs();
    }

    /**
     * 根据命令行参数创建一个新的任务
     *
     * @param args 命令行参数列表
     * @return 新创建的任务对象
     */
    @Override
    public synchronized Job createJob(List<CliToken> args) {
        // 创建任务并绑定Shell任务处理器
        Job job = jobController.createJob(commandManager, args, session, new ShellJobHandler(this), term, null);
        return job;
    }

    /**
     * 根据命令行字符串创建一个新的任务
     *
     * @param line 命令行字符串
     * @return 新创建的任务对象
     */
    @Override
    public Job createJob(String line) {
        // 将命令行字符串解析为Token列表，然后创建任务
        return createJob(CliTokens.tokenize(line));
    }

    /**
     * 获取会话对象
     *
     * @return 当前会话对象
     */
    @Override
    public Session session() {
        return session;
    }

    /**
     * 获取终端对象
     *
     * @return 当前终端对象
     */
    public Term term() {
        return term;
    }

    /**
     * 获取关闭Future的处理器
     *
     * @return FutureHandler对象
     */
    public FutureHandler closedFutureHandler() {
        return new FutureHandler(closedFuture);
    }

    /**
     * 获取最后访问时间
     *
     * @return 最后一次访问的时间戳
     */
    public long lastAccessedTime() {
        return term.lastAccessedTime();
    }

    /**
     * 设置欢迎消息
     *
     * @param welcome 欢迎消息字符串
     */
    public void setWelcome(String welcome) {
        this.welcome = welcome;
    }

    /**
     * 设置命令提示符
     * 提示符格式为：[arthas@PID]$
     */
    private void setPrompt(){
        this.prompt = "[arthas@" +
                session.getPid() +
                "]$ ";
    }

    /**
     * 初始化Shell会话
     *
     * 设置终端的各种处理器（中断、挂起、关闭）并显示欢迎消息
     *
     * @return 当前Shell实例，支持链式调用
     */
    public ShellImpl init() {
        // 设置中断信号处理器（如Ctrl+C）
        term.interruptHandler(new InterruptHandler(this));
        // 设置挂起信号处理器（如Ctrl+Z）
        term.suspendHandler(new SuspendHandler(this));
        // 设置关闭处理器
        term.closeHandler(new CloseHandler(this));

        // 如果设置了欢迎消息，则输出到终端
        if (welcome != null && welcome.length() > 0) {
            term.write(welcome + "\n");
        }
        return this;
    }

    /**
     * 生成任务的状态行信息
     *
     * 状态行包含：任务ID、状态、命令行、执行次数、开始时间、缓存位置、超时时间、会话信息等
     *
     * @param job 任务对象
     * @param status 任务执行状态
     * @return 格式化的状态行字符串
     */
    public String statusLine(Job job, ExecStatus status) {
        StringBuilder sb = new StringBuilder("[").append(job.id()).append("]");
        // 如果是当前会话的任务，添加*标记
        if (this.session().equals(job.getSession())) {
            sb.append("*");
        }
        sb.append("\n");
        // 添加任务状态（首字母大写）
        sb.append("       ").append(Character.toUpperCase(status.name().charAt(0)))
                .append(status.name().substring(1).toLowerCase());
        // 添加命令行
        sb.append("           ").append(job.line()).append("\n");
        // 添加执行次数
        sb.append("       execution count : ").append(job.process().times()).append("\n");
        // 添加开始时间
        sb.append("       start time      : ").append(job.process().startTime()).append("\n");
        // 添加缓存位置（如果有）
        String cacheLocation = job.process().cacheLocation();
        if (cacheLocation != null) {
            sb.append("       cache location  : ").append(cacheLocation).append("\n");
        }
        // 添加超时时间（如果有）
        Date timeoutDate = job.timeoutDate();
        if (timeoutDate != null) {
            sb.append("       timeout date    : ").append(timeoutDate).append("\n");
        }
        // 添加会话信息
        sb.append("       session         : ").append(job.getSession().getSessionId()).append(
                session.equals(job.getSession()) ? " (current)" : "").append("\n");
        return sb.toString();
    }

    /**
     * 读取用户输入
     *
     * 显示提示符并等待用户输入命令
     * 使用ShellLineHandler处理输入，CommandManagerCompletionHandler提供命令自动补全
     */
    public void readline() {
        term.readline(prompt, new ShellLineHandler(this),
                new CommandManagerCompletionHandler(commandManager));
    }

    /**
     * 关闭Shell会话
     *
     * @param reason 关闭原因
     */
    public void close(String reason) {
        if (term != null) {
            try {
                // 向终端输出关闭消息
                term.write("session (" + session.getSessionId() + ") is closed because " + reason + "\n");
            } catch (Throwable t) {
                // 有时通过web-socket关闭时会抛出NPE
                // 这确保关闭过程能够正确完成
                // https://github.com/alibaba/arthas/issues/320
                logger.error("Error writing data:", t);
            }
            // 关闭终端
            term.close();
        } else {
            // 如果终端不存在，直接关闭任务控制器
            jobController.close(closedFutureHandler());
        }
    }

    /**
     * 设置前台任务
     *
     * @param job 要设置为前台的任务
     */
    public void setForegroundJob(Job job) {
        currentForegroundJob = job;
    }

    /**
     * 获取当前的前台任务
     *
     * @return 当前正在前台运行的任务，如果没有则返回null
     */
    public Job getForegroundJob() {
        return currentForegroundJob;
    }

    /**
     * Shell任务处理器
     *
     * 监听任务的生命周期事件，并在事件发生时执行相应的处理逻辑
     * 包括：任务进入前台、后台、终止、挂起等事件
     */
    private static class ShellJobHandler implements JobListener {
        ShellImpl shell;

        /**
         * 构造函数
         *
         * @param shell Shell实例
         */
        public ShellJobHandler(ShellImpl shell) {
            this.shell = shell;
        }

        /**
         * 任务进入前台时的处理
         *
         * @param job 进入前台的任务
         */
        @Override
        public void onForeground(Job job) {
            // 设置当前前台任务
            shell.setForegroundJob(job);
            // 重置stdin处理器为任务的原始处理器
            //shell.term().stdinHandler(job.process().getStdinHandler());
        }

        /**
         * 任务进入后台时的处理
         *
         * @param job 进入后台的任务
         */
        @Override
        public void onBackground(Job job) {
            // 重置并读取下一行命令
            resetAndReadLine();
        }

        /**
         * 任务终止时的处理
         *
         * @param job 已终止的任务
         */
        @Override
        public void onTerminated(Job job) {
            // 如果不是后台任务，则读取下一行命令
            if (!job.isRunInBackground()){
                resetAndReadLine();
            }

            // 保存命令历史
            Term term = shell.term();
            if (term instanceof TermImpl) {
                // 获取命令历史
                List<int[]> history = ((TermImpl) term).getReadline().getHistory();
                // 将历史保存到文件
                FileUtils.saveCommandHistory(history, new File(Constants.CMD_HISTORY_FILE));
            }
        }

        /**
         * 任务挂起时的处理
         *
         * @param job 被挂起的任务
         */
        @Override
        public void onSuspend(Job job) {
            // 如果不是后台任务，则读取下一行命令
            if (!job.isRunInBackground()){
                resetAndReadLine();
            }
        }

        /**
         * 重置Shell状态并读取下一行命令
         *
         * 该方法在任务结束或挂起时调用，恢复Shell到等待输入状态
         */
        private void resetAndReadLine() {
            // 重置stdin处理器为echo处理器
            //shell.term().stdinHandler(null);
            // 清除前台任务引用
            shell.setForegroundJob(null);
            // 开始读取下一行命令
            shell.readline();
        }
    }

}

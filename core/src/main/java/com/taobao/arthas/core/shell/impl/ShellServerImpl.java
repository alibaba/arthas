package com.taobao.arthas.core.shell.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.arthas.tunnel.client.TunnelClient;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.Shell;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.server.SessionClosedHandler;
import com.taobao.arthas.core.shell.handlers.server.SessionsClosedHandler;
import com.taobao.arthas.core.shell.handlers.server.TermServerListenHandler;
import com.taobao.arthas.core.shell.handlers.server.TermServerTermHandler;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.impl.GlobalJobControllerImpl;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import com.taobao.arthas.core.util.ArthasBanner;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shell服务器实现类
 *
 * 负责管理多个终端服务器和客户端会话，是Arthas的核心服务器组件
 * 提供终端连接管理、命令解析、会话管理、超时清理等功能
 *
 * 主要职责：
 * 1. 管理多个TermServer（如Telnet、HTTP等）
 * 2. 处理客户端连接并创建对应的Shell会话
 * 3. 注册和管理命令解析器
 * 4. 定期清理超时的会话
 * 5. 优雅地关闭所有服务和会话
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellServerImpl extends ShellServer {

    private static final Logger logger = LoggerFactory.getLogger(ShellServerImpl.class);

    /**
     * 命令解析器列表，使用CopyOnWriteArrayList保证线程安全
     * 支持动态添加新的命令解析器
     */
    private final CopyOnWriteArrayList<CommandResolver> resolvers;

    /**
     * 内部命令管理器，负责管理所有命令的注册和解析
     */
    private final InternalCommandManager commandManager;

    /**
     * 终端服务器列表，支持多种连接方式（Telnet、HTTP等）
     */
    private final List<TermServer> termServers;

    /**
     * 会话超时时间（毫秒），超过此时间未活跃的会话将被清理
     */
    private final long timeoutMillis;

    /**
     * 会话清理任务的执行间隔（毫秒）
     */
    private final long reaperInterval;

    /**
     * 欢迎消息，在客户端连接时显示
     */
    private String welcomeMessage;

    /**
     * Java Instrumentation实例，用于字节码增强
     */
    private Instrumentation instrumentation;

    /**
     * 当前JVM进程的PID
     */
    private long pid;

    /**
     * 服务器是否已关闭的标志
     * 初始值为true，在listen()方法调用后设置为false
     */
    private boolean closed = true;

    /**
     * 存储所有活跃的Shell会话，键为会话ID
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, ShellImpl> sessions;

    /**
     * 所有会话关闭的Future，用于异步通知所有会话已关闭
     */
    private final Future<Void> sessionsClosed = Future.future();

    /**
     * 定时任务执行器，用于定期执行会话清理任务
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * 任务控制器，使用全局任务控制器实现
     */
    private JobControllerImpl jobController = new GlobalJobControllerImpl();

    /**
     * 构造函数，初始化Shell服务器
     *
     * @param options Shell服务器配置选项
     */
    public ShellServerImpl(ShellServerOptions options) {
        // 从配置中获取欢迎消息
        this.welcomeMessage = options.getWelcomeMessage();
        // 初始化终端服务器列表
        this.termServers = new ArrayList<TermServer>();
        // 从配置中获取会话超时时间
        this.timeoutMillis = options.getSessionTimeout();
        // 初始化会话存储
        this.sessions = new ConcurrentHashMap<String, ShellImpl>();
        // 从配置中获取清理间隔
        this.reaperInterval = options.getReaperInterval();
        // 初始化命令解析器列表
        this.resolvers = new CopyOnWriteArrayList<CommandResolver>();
        // 创建内部命令管理器，传入解析器列表
        this.commandManager = new InternalCommandManager(resolvers);
        // 从配置中获取Instrumentation实例
        this.instrumentation = options.getInstrumentation();
        // 从配置中获取PID
        this.pid = options.getPid();

        // 注册内置命令解析器，使其在help命令中可见
        resolvers.add(new BuiltinCommandResolver());
    }

    /**
     * 注册命令解析器
     *
     * 新的解析器会被添加到列表头部，具有更高的优先级
     *
     * @param resolver 要注册的命令解析器
     * @return 当前ShellServer实例，支持链式调用
     */
    @Override
    public synchronized ShellServer registerCommandResolver(CommandResolver resolver) {
        // 添加到列表头部，优先级最高
        resolvers.add(0, resolver);
        return this;
    }

    /**
     * 注册终端服务器
     *
     * 支持注册多个终端服务器，如Telnet、HTTP等
     *
     * @param termServer 要注册的终端服务器
     * @return 当前ShellServer实例，支持链式调用
     */
    @Override
    public synchronized ShellServer registerTermServer(TermServer termServer) {
        termServers.add(termServer);
        return this;
    }

    /**
     * 处理终端连接
     *
     * 当有新的客户端连接时，创建对应的Shell会话并进行初始化
     *
     * @param term 终端对象
     */
    public void handleTerm(Term term) {
        synchronized (this) {
            // 如果服务器已关闭，直接关闭终端连接
            // 这可能在多个服务器同时运行时发生
            if (closed) {
                term.close();
                return;
            }
        }

        // 创建Shell会话
        ShellImpl session = createShell(term);
        // 尝试更新欢迎消息（如果配置了Tunnel客户端）
        tryUpdateWelcomeMessage();
        // 设置欢迎消息
        session.setWelcome(welcomeMessage);
        // 设置会话关闭处理器
        session.closedFuture.setHandler(new SessionClosedHandler(this, session));
        // 初始化会话
        session.init();
        // 将会话添加到会话映射表中（放在init之后，确保连接的关闭处理器已设置）
        sessions.put(session.id, session);
        // 开始读取用户输入
        session.readline();
    }

    /**
     * 尝试更新欢迎消息
     *
     * 如果配置了Tunnel客户端，会在欢迎消息中显示Tunnel ID
     */
    private void tryUpdateWelcomeMessage() {
        // 获取Tunnel客户端实例
        TunnelClient tunnelClient = ArthasBootstrap.getInstance().getTunnelClient();
        if (tunnelClient != null) {
            // 获取Tunnel ID
            String id = tunnelClient.getId();
            if (id != null) {
                // 创建包含Tunnel ID的欢迎信息
                Map<String, String> welcomeInfos = new HashMap<String, String>();
                welcomeInfos.put("id", id);
                // 重新生成欢迎消息
                this.welcomeMessage = ArthasBanner.welcome(welcomeInfos);
            }
        }
    }

    /**
     * 启动Shell服务器
     *
     * 启动所有已注册的终端服务器，开始监听客户端连接
     *
     * @param listenHandler 监听完成的回调处理器
     * @return 当前ShellServer实例，支持链式调用
     */
    @Override
    public ShellServer listen(final Handler<Future<Void>> listenHandler) {
        final List<TermServer> toStart;
        synchronized (this) {
            // 检查服务器是否已经在监听
            if (!closed) {
                throw new IllegalStateException("Server listening");
            }
            // 获取要启动的终端服务器列表
            toStart = termServers;
        }
        // 使用计数器跟踪所有服务器的启动状态
        final AtomicInteger count = new AtomicInteger(toStart.size());
        // 如果没有服务器需要启动，直接返回成功
        if (count.get() == 0) {
            setClosed(false);
            listenHandler.handle(Future.<Void>succeededFuture());
            return this;
        }
        // 创建服务器监听处理器
        Handler<Future<TermServer>> handler = new TermServerListenHandler(this, listenHandler, toStart);
        // 启动所有终端服务器
        for (TermServer termServer : toStart) {
            // 设置终端处理器
            termServer.termHandler(new TermServerTermHandler(this));
            // 开始监听
            termServer.listen(handler);
        }
        return this;
    }

    /**
     * 清理超时的会话
     *
     * 定期检查所有会话，移除超过超时时间且没有正在运行任务的会话
     * 对于长时间运行的命令（如trace），会等待其执行完成
     */
    private void evictSessions() {
        long now = System.currentTimeMillis();
        Set<ShellImpl> toClose = new HashSet<ShellImpl>();
        // 遍历所有会话，检查是否需要清理
        for (ShellImpl session : sessions.values()) {
            // 不要关闭还在运行任务的会话
            // 例如：trace命令可能需要等待很长时间才能满足触发条件
            if (now - session.lastAccessedTime() > timeoutMillis && session.jobs().size() == 0) {
                // 标记需要关闭的会话
                toClose.add(session);
            }
            // 输出调试日志
            logger.debug(session.id + ":" + session.lastAccessedTime());
        }
        // 关闭所有标记为超时的会话
        for (ShellImpl session : toClose) {
            // 计算超时分钟数，用于日志和提示
            long timeOutInMinutes = timeoutMillis / 1000 / 60;
            String reason = "session is inactive for " + timeOutInMinutes + " min(s).";
            // 关闭会话
            session.close(reason);
        }
    }

    /**
     * 设置会话清理定时器
     *
     * 创建一个单线程的定时任务执行器，按照配置的间隔定期执行会话清理任务
     * 定时线程被设置为守护线程，不会阻止JVM退出
     */
    public synchronized void setTimer() {
        // 只有在未关闭且配置了有效的清理间隔时才启动定时器
        if (!closed && reaperInterval > 0) {
            // 创建单线程定时执行器
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    // 创建名为"arthas-shell-server"的守护线程
                    final Thread t = new Thread(r, "arthas-shell-server");
                    t.setDaemon(true);
                    return t;
                }
            });
            // 按照固定间隔执行会话清理任务，初始延迟为0
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    evictSessions();
                }
            }, 0, reaperInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 设置服务器的关闭状态
     *
     * @param closed 是否关闭
     */
    public synchronized void setClosed(boolean closed) {
        this.closed = closed;
    }

    /**
     * 移除指定的Shell会话
     *
     * 当会话关闭时调用此方法，清理会话相关的资源
     *
     * @param shell 要移除的Shell会话
     */
    public void removeSession(ShellImpl shell) {
        boolean completeSessionClosed;

        // 获取会话的前台任务
        Job job = shell.getForegroundJob();
        if (job != null) {
            // 终止会话的前台任务
            job.terminate();
            logger.info("Session {} closed, so terminate foreground job, id: {}, line: {}",
                        shell.session().getSessionId(), job.id(), job.line());
        }

        synchronized (ShellServerImpl.this) {
            // 从会话映射表中移除该会话
            sessions.remove(shell.id);
            // 关闭Shell
            shell.close("network error");
            // 检查是否所有会话都已关闭且服务器已关闭
            completeSessionClosed = sessions.isEmpty() && closed;
        }
        // 如果所有会话都已关闭且服务器已关闭，完成关闭Future
        if (completeSessionClosed) {
            sessionsClosed.complete();
        }
    }

    /**
     * 创建一个不绑定终端的Shell实例
     *
     * @return 新创建的Shell实例
     */
    @Override
    public synchronized Shell createShell() {
        // 创建不绑定终端的Shell（传入null）
        return createShell(null);
    }

    /**
     * 创建一个绑定到指定终端的Shell实例
     *
     * @param term 终端对象，可以为null
     * @return 新创建的Shell实例
     */
    @Override
    public synchronized ShellImpl createShell(Term term) {
        // 如果服务器已关闭，抛出异常
        if (closed) {
            throw new IllegalStateException("Closed");
        }
        // 创建并返回新的Shell实例
        return new ShellImpl(this, term, commandManager, instrumentation, pid, jobController);
    }

    /**
     * 关闭Shell服务器
     *
     * 关闭所有终端服务器和会话，释放所有资源
     *
     * @param completionHandler 关闭完成的回调处理器
     */
    @Override
    public synchronized void close(final Handler<Future<Void>> completionHandler) {
        List<TermServer> toStop;
        List<ShellImpl> toClose;
        synchronized (this) {
            // 如果服务器已经关闭，直接返回
            if (closed) {
                toStop = Collections.emptyList();
                toClose = Collections.emptyList();
            } else {
                // 标记服务器为已关闭
                setClosed(true);
                // 停止定时清理任务
                if (scheduledExecutorService != null) {
                    scheduledExecutorService.shutdownNow();
                }
                // 获取要停止的终端服务器列表
                toStop = termServers;
                // 获取要关闭的会话列表
                toClose = new ArrayList<ShellImpl>(sessions.values());
                // 如果没有会话需要关闭，直接完成关闭Future
                if (toClose.isEmpty()) {
                    sessionsClosed.complete();
                }
            }
        }
        // 如果没有服务器和会话需要关闭，直接返回成功
        if (toStop.isEmpty() && toClose.isEmpty()) {
            completionHandler.handle(Future.<Void>succeededFuture());
        } else {
            // 使用计数器跟踪所有资源的关闭状态
            final AtomicInteger count = new AtomicInteger(1 + toClose.size());
            Handler<Future<Void>> handler = new SessionsClosedHandler(count, completionHandler);

            // 关闭所有会话
            for (ShellImpl shell : toClose) {
                shell.close("server is going to shutdown.");
            }

            // 停止所有终端服务器
            for (TermServer termServer : toStop) {
                termServer.close(handler);
            }
            // 关闭任务控制器
            jobController.close();
            // 设置会话关闭完成的处理器
            sessionsClosed.setHandler(handler);
        }
    }

    /**
     * 获取任务控制器
     *
     * @return 任务控制器实例
     */
    public JobControllerImpl getJobController() {
        return jobController;
    }

    /**
     * 获取命令管理器
     *
     * @return 内部命令管理器实例
     */
    public InternalCommandManager getCommandManager() {
        return commandManager;
    }
}

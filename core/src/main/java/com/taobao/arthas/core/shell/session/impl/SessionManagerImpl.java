package com.taobao.arthas.core.shell.session.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.*;

/**
 * Arthas 会话管理器实现类
 *
 * 负责管理所有Arthas客户端连接的会话生命周期，提供会话的创建、查询、移除和超时清理功能
 * 同时管理命令解析器、任务控制器和Java Instrumentation实例等核心组件
 *
 * 主要功能：
 * 1. 会话生命周期管理：创建、获取、移除会话
 * 2. 会话超时清理：定期清理长时间未活跃的会话
 * 3. ResultConsumer超时清理：清理长时间未活跃的结果消费者
 * 4. 资源管理：在关闭时清理所有相关资源
 *
 * @author gongdewei 2020-03-20
 */
public class SessionManagerImpl implements SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManagerImpl.class);

    /**
     * 内部命令管理器，负责注册和解析所有Arthas命令
     */
    private final InternalCommandManager commandManager;

    /**
     * Java Instrumentation实例，用于字节码增强和类重定义
     */
    private final Instrumentation instrumentation;

    /**
     * 任务控制器，负责管理所有前台和后台任务的执行
     */
    private final JobController jobController;

    /**
     * 会话超时时间（毫秒），超过此时间未活跃的会话将被清理
     */
    private final long sessionTimeoutMillis;

    /**
     * ResultConsumer超时时间（毫秒），默认5分钟
     * 超过此时间未活跃的消费者将被移除
     */
    private final int consumerTimeoutMillis;

    /**
     * 会话清理任务的执行间隔（毫秒）
     * 定时任务会按照此间隔检查并清理超时的会话
     */
    private final long reaperInterval;

    /**
     * 存储所有活跃会话的映射表，键为会话ID
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, Session> sessions;

    /**
     * 当前JVM进程的PID
     */
    private final long pid;

    /**
     * 会话管理器是否已关闭的标志
     */
    private boolean closed = false;

    /**
     * 定时任务执行器，用于定期执行会话清理任务
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * 构造函数，初始化会话管理器
     *
     * @param options Shell服务器配置选项，包含超时时间、PID等配置信息
     * @param commandManager 内部命令管理器
     * @param jobController 任务控制器
     */
    public SessionManagerImpl(ShellServerOptions options, InternalCommandManager commandManager,
                              JobController jobController) {
        this.commandManager = commandManager;
        this.jobController = jobController;
        // 使用并发HashMap存储会话，支持高并发访问
        this.sessions = new ConcurrentHashMap<String, Session>();
        // 从配置中获取会话超时时间
        this.sessionTimeoutMillis = options.getSessionTimeout();
        // ResultConsumer默认超时时间为5分钟
        this.consumerTimeoutMillis = 5 * 60 * 1000; // 5 minutes
        // 从配置中获取清理任务执行间隔
        this.reaperInterval = options.getReaperInterval();
        // 获取Java Instrumentation实例
        this.instrumentation = options.getInstrumentation();
        // 获取当前进程PID
        this.pid = options.getPid();
        // 启动会话超时清理定时器
        this.setEvictTimer();
    }


    /**
     * 创建一个新的会话
     *
     * 每个会话包含：
     * 1. 命令管理器引用
     * 2. Instrumentation实例
     * 3. 当前进程PID
     * 4. 唯一的会话ID（使用UUID生成）
     *
     * @return 新创建的会话对象
     */
    @Override
    public Session createSession() {
        // 创建会话实现类实例
        Session session = new SessionImpl();
        // 将命令管理器放入会话属性中
        session.put(Session.COMMAND_MANAGER, commandManager);
        // 将Instrumentation实例放入会话属性中，用于字节码操作
        session.put(Session.INSTRUMENTATION, instrumentation);
        // 将PID放入会话属性中，用于显示和识别
        session.put(Session.PID, pid);
        //session.put(Session.SERVER, server); // 服务器实例暂时不放入会话
        //session.put(Session.TTY, term); // 终端实例暂时不放入会话
        // 生成唯一的会话ID
        String sessionId = UUID.randomUUID().toString();
        session.put(Session.ID, sessionId);

        // 将新创建的会话添加到会话映射表中
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * 根据会话ID获取对应的会话对象
     *
     * @param sessionId 会话ID
     * @return 对应的会话对象，如果不存在则返回null
     */
    @Override
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 移除指定ID的会话
     *
     * 移除操作包括：
     * 1. 中断会话的前台任务（如果存在）
     * 2. 关闭结果分发器
     * 3. 从会话映射表中移除该会话
     *
     * @param sessionId 要移除的会话ID
     * @return 被移除的会话对象，如果会话不存在则返回null
     */
    @Override
    public Session removeSession(String sessionId) {
        // 先获取要移除的会话对象
        Session session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }

        // 中断会话的前台任务（如果存在正在运行的前台任务）
        Job job = session.getForegroundJob();
        if (job != null) {
            job.interrupt();
        }

        // 关闭会话的结果分发器，释放相关资源
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            resultDistributor.close();
        }

        // 从会话映射表中移除该会话
        return sessions.remove(sessionId);
    }

    /**
     * 更新会话的最后访问时间
     *
     * 此方法应该在每次会话有活动时调用，用于防止活跃会话被超时清理机制清理
     *
     * @param session 需要更新访问时间的会话对象
     */
    @Override
    public void updateAccessTime(Session session) {
        session.setLastAccessTime(System.currentTimeMillis());
    }

    /**
     * 关闭会话管理器
     *
     * 关闭操作包括：
     * 1. 停止定时清理任务
     * 2. 向所有会话发送关闭通知消息
     * 3. 移除并清理所有会话
     * 4. 关闭任务控制器
     *
     * TODO: 在关闭Arthas时需要清理更多资源
     */
    @Override
    public void close() {
        //TODO clear resources while shutdown arthas
        // 标记管理器为已关闭状态
        closed = true;
        // 停止定时清理任务
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }

        // 创建会话列表的副本，避免在遍历时修改集合
        ArrayList<Session> sessions = new ArrayList<Session>(this.sessions.values());
        // 遍历所有会话，逐个清理
        for (Session session : sessions) {
            // 获取会话的结果分发器
            SharingResultDistributor resultDistributor = session.getResultDistributor();
            if (resultDistributor != null) {
                // 向客户端发送服务器关闭通知
                resultDistributor.appendResult(new MessageModel("arthas server is going to shutdown."));
            }
            // 记录会话关闭日志
            logger.info("Removing session before shutdown: {}, last access time: {}", session.getSessionId(), session.getLastAccessTime());
            // 移除会话
            this.removeSession(session.getSessionId());
        }

        // 关闭任务控制器，清理所有任务
        jobController.close();
    }

    /**
     * 设置会话超时清理定时器
     *
     * 创建一个单线程的定时任务执行器，按照配置的间隔定期执行会话清理任务
     * 定时线程被设置为守护线程，不会阻止JVM退出
     */
    private synchronized void setEvictTimer() {
        // 只有在未关闭且配置了有效的清理间隔时才启动定时器
        if (!closed && reaperInterval > 0) {
            // 创建单线程定时执行器
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    // 创建名为"arthas-session-manager"的守护线程
                    final Thread t = new Thread(r, "arthas-session-manager");
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
     * 检查并移除不活跃的会话
     *
     * 清理条件：
     * 1. 会话最后访问时间超过配置的超时时间
     * 2. 会话没有正在运行的前台任务（避免中断正在执行的命令）
     *
     * 对于长时间运行的命令（如trace），会等待其执行完成后再清理会话
     */
    public void evictSessions() {
        long now = System.currentTimeMillis();
        List<Session> toClose = new ArrayList<Session>();
        // 遍历所有会话，检查是否需要清理
        for (Session session : sessions.values()) {
            // 不要关闭还在运行任务的会话
            // 例如：trace命令可能需要等待很长时间才能满足触发条件
            //TODO: 检查后台任务数量
            if (now - session.getLastAccessTime() > sessionTimeoutMillis && session.getForegroundJob() == null) {
                // 标记需要关闭的会话
                toClose.add(session);
            }
            // 清理会话中的不活跃消费者
            evictConsumers(session);
        }
        // 关闭所有标记为超时的会话
        for (Session session : toClose) {
            // 中断前台任务（双重检查）
            Job job = session.getForegroundJob();
            if (job != null) {
                job.interrupt();
            }
            // 计算超时分钟数，用于日志和提示
            long timeOutInMinutes = sessionTimeoutMillis / 1000 / 60;
            String reason = "session is inactive for " + timeOutInMinutes + " min(s).";
            // 获取结果分发器
            SharingResultDistributor resultDistributor = session.getResultDistributor();
            if (resultDistributor != null) {
                // 向客户端发送会话超时关闭的消息
                resultDistributor.appendResult(new MessageModel(reason));
            }
            // 移除会话
            this.removeSession(session.getSessionId());
            // 记录会话移除日志
            logger.info("Removing inactive session: {}, last access time: {}", session.getSessionId(), session.getLastAccessTime());
        }
    }

    /**
     * 检查并移除会话中不活跃的结果消费者
     *
     * ResultConsumer用于向客户端分发命令执行结果
     * 当客户端（如浏览器页面）长时间未活动时，对应的消费者会被清理
     *
     * @param session 要检查的会话对象
     */
    public void evictConsumers(Session session) {
        // 获取会话的结果分发器
        SharingResultDistributor distributor = session.getResultDistributor();
        if (distributor != null) {
            // 获取所有结果消费者
            List<ResultConsumer> consumers = distributor.getConsumers();
            // 直接从会话中移除不活跃的消费者
            long now = System.currentTimeMillis();
            // 遍历所有消费者，检查是否超时
            for (ResultConsumer consumer : consumers) {
                // 计算消费者的不活跃时长
                long inactiveTime = now - consumer.getLastAccessTime();
                if (inactiveTime > consumerTimeoutMillis) {
                    // 不活跃时长必须大于轮询时间限制
                    logger.info("Removing inactive consumer from session, sessionId: {}, consumerId: {}, inactive duration: {}",
                            session.getSessionId(), consumer.getConsumerId(), inactiveTime);
                    // 向客户端发送消费者被移除的消息
                    consumer.appendResult(new MessageModel("consumer is inactive for a while, please refresh the page."));
                    // 从分发器中移除不活跃的消费者
                    distributor.removeConsumer(consumer);
                }
            }
        }
    }

    /**
     * 获取内部命令管理器
     *
     * @return 内部命令管理器实例
     */
    @Override
    public InternalCommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * 获取Java Instrumentation实例
     *
     * @return Instrumentation实例
     */
    @Override
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * 获取任务控制器
     *
     * @return 任务控制器实例
     */
    @Override
    public JobController getJobController() {
        return jobController;
    }
}

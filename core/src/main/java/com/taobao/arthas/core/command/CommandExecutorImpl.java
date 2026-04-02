package com.taobao.arthas.core.command;

import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.distribution.impl.PackingResultDistributorImpl;
import com.taobao.arthas.core.distribution.impl.ResultConsumerImpl;
import com.taobao.arthas.core.distribution.impl.SharingResultDistributorImpl;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.mcp.server.CommandExecutor;
import io.termd.core.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.taobao.arthas.common.ArthasConstants.SUBJECT_KEY;

/**
 * 命令执行器实现类
 * 用于执行Arthas命令，支持同步和异步执行
 * 提供会话管理、任务管理、结果分发等功能
 */
public class CommandExecutorImpl implements CommandExecutor {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutorImpl.class);

    // 一次性会话的标识键
    private static final String ONETIME_SESSION_KEY = "oneTimeSession";

    // 会话管理器，负责创建、获取和销毁会话
    private final SessionManager sessionManager;

    // 任务控制器，负责任务的创建和管理
    private final JobController jobController;

    // 内部命令管理器，负责命令的解析和注册
    private final InternalCommandManager commandManager;

    /**
     * 构造函数
     *
     * @param sessionManager 会话管理器
     */
    public CommandExecutorImpl(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        // 从会话管理器中获取命令管理器
        this.commandManager = sessionManager.getCommandManager();
        // 从会话管理器中获取任务控制器
        this.jobController = sessionManager.getJobController();
    }

    /**
     * 获取当前会话
     * 如果sessionId为空或为空字符串，根据oneTimeIsAllowed参数决定是否创建临时会话
     *
     * @param sessionId 会话ID，可能为null
     * @param oneTimeIsAllowed 是否允许创建一次性会话
     * @return 会话对象
     * @throws SessionNotFoundException 如果会话不存在且不允许创建临时会话
     */
    public Session getCurrentSession(String sessionId, boolean oneTimeIsAllowed) {
        // 如果sessionId为null或空字符串
        if (sessionId == null || sessionId.trim().isEmpty()) {
            // 如果不允许创建一次性会话，抛出异常
            if (!oneTimeIsAllowed) {
                throw new SessionNotFoundException("SessionId is required for this operation");
            }

            // 创建新的临时会话
            Session session = sessionManager.createSession();
            if (session == null) {
                throw new SessionNotFoundException("Failed to create temporary session");
            }
            // 标记为一次性会话
            session.put(ONETIME_SESSION_KEY, new Object());
            logger.debug("Created one-time session {}", session.getSessionId());
            return session;
        } else {
            // 获取已存在的会话
            Session session = sessionManager.getSession(sessionId);
            if (session == null) {
                throw new SessionNotFoundException("Session not found: " + sessionId);
            }
            // 更新会话的访问时间
            sessionManager.updateAccessTime(session);
            logger.debug("Using existing session {}", sessionId);
            return session;
        }
    }

    /**
     * 内部同步执行方法，统一处理认证和session管理
     *
     * @param commandLine 命令行字符串
     * @param timeout 超时时间（毫秒）
     * @param sessionId 会话ID，如果为null则创建临时会话
     * @param authSubject 认证主体，如果不为null则应用到session
     * @param userId 用户ID，用于统计上报
     * @return 执行结果，包含命令、状态、结果列表等信息
     */
    @Override
    public Map<String, Object> executeSync(String commandLine, long timeout, String sessionId, Object authSubject, String userId) {
        Session session = null;
        // 标记是否为一次性会话访问
        boolean oneTimeAccess = false;

        try {
            // 获取或创建会话
            session = getCurrentSession(sessionId, true);

            // 如果提供了认证主体，将其应用到会话中
            if (authSubject != null) {
                session.put(SUBJECT_KEY, authSubject);
                logger.debug("Applied auth subject to session: {} (authSubject: {})",
                           session.getSessionId(), authSubject.getClass().getSimpleName());
            }

            // 设置 userId 到 session，用于统计上报
            if (userId != null && !userId.trim().isEmpty()) {
                session.setUserId(userId);
                logger.debug("Set userId to session: {} (userId: {})", session.getSessionId(), userId);
            }

            // 检查是否为一次性会话
            if (session.get(ONETIME_SESSION_KEY) != null) {
                oneTimeAccess = true;
            }

            // 创建打包结果分发器，用于收集命令执行结果
            PackingResultDistributorImpl resultDistributor = new PackingResultDistributorImpl(session);

            // 创建任务
            Job job = this.createJob(commandLine, session, resultDistributor);

            // 如果任务创建失败，返回错误结果
            if (job == null) {
                logger.error("Failed to create job for command: {}", commandLine);
                return createErrorResult(commandLine, "Failed to create job");
            }

            // 运行任务
            job.run();

            // 等待任务完成（带超时）
            boolean finished = waitForJob(job, (int) timeout);
            if (!finished) {
                // 任务超时，中断任务
                logger.warn("Command timeout after {} ms: {}", timeout, commandLine);
                job.interrupt();
                return createTimeoutResult(commandLine, timeout);
            }

            // 构建成功结果
            Map<String, Object> result = new TreeMap<>();
            result.put("command", commandLine);
            result.put("success", true);
            result.put("sessionId", session.getSessionId());
            result.put("executionTime", System.currentTimeMillis());

            // 获取并添加结果列表
            List<ResultModel> results = resultDistributor.getResults();
            if (results != null && !results.isEmpty()) {
                result.put("results", results);
                result.put("resultCount", results.size());
            } else {
                result.put("results", results);
                result.put("resultCount", 0);
            }

            return result;

        } catch (SessionNotFoundException e) {
            // 会话未找到异常
            logger.error("Session error for command: {}", commandLine, e);
            return createErrorResult(commandLine, e.getMessage());
        } catch (Exception e) {
            // 其他异常
            logger.error("Error executing command: {}", commandLine, e);
            return createErrorResult(commandLine, "Error executing command: " + e.getMessage());
        } finally {
            // 如果是一次性会话，使用完毕后销毁
            if (oneTimeAccess && session != null) {
                try {
                    sessionManager.removeSession(session.getSessionId());
                    logger.debug("Destroyed one-time session {}", session.getSessionId());
                } catch (Exception e) {
                    logger.warn("Error removing one-time session", e);
                }
            }
        }
    }

    /**
     * 异步执行命令
     * 命令会在后台执行，调用者可以通过pullResults方法获取执行结果
     *
     * @param commandLine 命令行字符串
     * @param sessionId 会话ID，不能为null
     * @return 执行结果，包含命令、状态、任务ID等信息
     */
    @Override
    public Map<String, Object> executeAsync(String commandLine, String sessionId) {
        Map<String, Object> result = new TreeMap<>();
        // 获取会话（不允许创建临时会话）
        Session session = getCurrentSession(sessionId, false);

        // 尝试获取会话锁，如果失败说明有其他命令正在执行
        if (!session.tryLock()) {
            logger.warn("Another command is executing in session: {}", session.getSessionId());
            return createErrorResult(commandLine, "Another command is executing");
        }
        // 记录锁的版本号
        int lock = session.getLock();

        try {
            // 检查是否已有前台任务在运行
            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                logger.warn("Another job is running in session: {}, jobId: {}", session.getSessionId(), foregroundJob.id());
                session.unLock();
                return createErrorResult(commandLine, "Another job is running, jobId: " + foregroundJob.id());
            }

            // 创建异步任务，使用会话的共享结果分发器
            Job job = this.createJob(commandLine, session, session.getResultDistributor());

            // 如果任务创建失败，返回错误结果
            if (job == null) {
                logger.error("Failed to create job for command: {}", commandLine);
                session.unLock();
                return createErrorResult(commandLine, "Failed to create job");
            }

            // 设置为前台任务
            session.setForegroundJob(job);
            // 更新会话输入状态为允许中断
            updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

            // 启动任务（异步执行）
            job.run();

            // 构建返回结果
            result.put("success", true);
            result.put("command", commandLine);
            result.put("sessionId", session.getSessionId());
            result.put("jobId", job.id());
            result.put("jobStatus", job.status().toString());

            return result;

        } catch (SessionNotFoundException e) {
            // 会话未找到异常
            logger.error("Session error for async command: {}", commandLine, e);
            return createErrorResult(commandLine, e.getMessage());
        } catch (Exception e) {
            // 其他异常
            logger.error("Error executing async command: {}", commandLine, e);
            return createErrorResult(commandLine, "Error executing async command: " + e.getMessage());
        } finally {
            // 如果锁没有被其他线程修改，释放锁
            if (session.getLock() == lock) {
                session.unLock();
            }
        }
    }

    /**
     * 从会话的结果分发器中拉取执行结果
     * 用于异步命令执行后获取结果
     *
     * @param sessionId 会话ID
     * @param consumerId 消费者ID，用于标识结果消费者
     * @return 执行结果，包含结果列表；如果没有新结果则返回null
     */
    @Override
    public Map<String, Object> pullResults(String sessionId, String consumerId) {
        // 检查消费者ID是否为空
        if (StringUtils.isBlank(consumerId)) {
            return createErrorResult(null, "Consumer ID is null or empty");
        }

        try {
            // 获取会话
            Session session = getCurrentSession(sessionId, false);

            // 获取会话的结果分发器
            SharingResultDistributor resultDistributor = session.getResultDistributor();
            if (resultDistributor == null) {
                return createErrorResult(null, "No result distributor found for session: " + sessionId);
            }

            // 获取指定ID的结果消费者
            ResultConsumer consumer = resultDistributor.getConsumer(consumerId);
            if (consumer == null) {
                return createErrorResult(null, "Consumer not found: " + consumerId);
            }

            // 从消费者中拉取结果
            List<ResultModel> results = consumer.pollResults();

            // 如果结果列表为空，返回null
            if (results != null && results.isEmpty()) {
                logger.debug("Filtered empty result list for session: {}, consumer: {}", sessionId, consumerId);
                return null;
            }

            // 构建返回结果
            Map<String, Object> result = new TreeMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("consumerId", consumerId);
            result.put("results", results);

            // 如果有前台任务，添加任务信息
            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                result.put("jobId", foregroundJob.id());
                result.put("jobStatus", foregroundJob.status().toString());
            }

            return result;

        } catch (SessionNotFoundException e) {
            // 会话未找到异常
            return createErrorResult(null, e.getMessage());
        }
    }

    /**
     * 中断会话中正在运行的前台任务
     *
     * @param sessionId 会话ID
     * @return 操作结果，包含任务ID和状态
     */
    @Override
    public Map<String, Object> interruptJob(String sessionId) {
        try {
            // 获取会话
            Session session = getCurrentSession(sessionId, false);

            // 获取前台任务
            Job job = session.getForegroundJob();
            if (job == null) {
                return createErrorResult(null, "no foreground job is running");
            }

            // 中断任务
            job.interrupt();

            // 构建返回结果
            Map<String, Object> result = new TreeMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("jobId", job.id());
            result.put("jobStatus", job.status().toString());
            return result;

        } catch (SessionNotFoundException e) {
            // 会话未找到异常
            return createErrorResult(null, e.getMessage());
        }
    }

    /**
     * 创建新的会话
     * 会话包含结果分发器和结果消费者，用于接收命令执行结果
     *
     * @return 操作结果，包含会话ID和消费者ID
     */
    @Override
    public Map<String, Object> createSession() {
        // 创建新会话
        Session session = sessionManager.createSession();
        if (session == null) {
            return createErrorResult(null, "create api session failed");
        }

        // 创建共享结果分发器
        SharingResultDistributorImpl resultDistributor = new SharingResultDistributorImpl(session);

        // 创建结果消费者
        ResultConsumer resultConsumer = new ResultConsumerImpl();

        // 将消费者添加到分发器中
        resultDistributor.addConsumer(resultConsumer);
        // 设置会话的结果分发器
        session.setResultDistributor(resultDistributor);

        // 添加欢迎消息
        resultDistributor.appendResult(new MessageModel("Welcome to arthas!"));

        // 构建欢迎信息模型
        WelcomeModel welcomeModel = new WelcomeModel();
        welcomeModel.setVersion(ArthasBanner.version());
        welcomeModel.setWiki(ArthasBanner.wiki());
        welcomeModel.setTutorials(ArthasBanner.tutorials());
        welcomeModel.setMainClass(PidUtils.mainClass());
        welcomeModel.setPid(PidUtils.currentPid());
        welcomeModel.setTime(DateUtils.getCurrentDateTime());
        resultDistributor.appendResult(welcomeModel);

        // 更新会话输入状态
        updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);

        // 构建返回结果
        Map<String, Object> result = new TreeMap<>();
        result.put("success", true);
        result.put("sessionId", session.getSessionId());
        result.put("consumerId", resultConsumer.getConsumerId());
        return result;
    }

    /**
     * 关闭会话
     * 释放会话资源
     *
     * @param sessionId 要关闭的会话ID
     * @return 操作结果
     */
    @Override
    public Map<String, Object> closeSession(String sessionId) {
        try {
            // 获取会话
            Session session = getCurrentSession(sessionId, false);

            // 如果会话已锁定，先解锁
            if (session.isLocked()) {
                session.unLock();
            }

            // 从会话管理器中移除会话
            sessionManager.removeSession(session.getSessionId());

            // 构建返回结果
            Map<String, Object> result = new TreeMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            return result;

        } catch (SessionNotFoundException e) {
            // 会话未找到异常
            return createErrorResult(null, e.getMessage());
        }
    }

    /**
     * 设置会话的认证主体
     * 用于权限控制和安全认证
     *
     * @param sessionId 会话ID
     * @param authSubject 认证主体对象
     */
    @Override
    public void setSessionAuth(String sessionId, Object authSubject) {
        try {
            // 获取会话
            Session session = getCurrentSession(sessionId, false);
            if (authSubject != null) {
                // 将认证主体放入会话中
                session.put(SUBJECT_KEY, authSubject);
            }
        } catch (SessionNotFoundException e) {
            // 会话不存在时记录警告日志
            logger.warn("Cannot set auth for non-existent session: {}", sessionId);
        }
    }

    /**
     * 设置会话的用户ID
     * 用于统计和追踪用户操作
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    @Override
    public void setSessionUserId(String sessionId, String userId) {
        try {
            // 获取会话
            Session session = getCurrentSession(sessionId, false);
            if (userId != null && !userId.trim().isEmpty()) {
                // 设置用户ID到会话
                session.setUserId(userId);
                logger.debug("Set userId for session {}: {}", sessionId, userId);
            }
        } catch (SessionNotFoundException e) {
            // 会话不存在时记录警告日志
            logger.warn("Cannot set userId for non-existent session: {}", sessionId);
        }
    }

    /**
     * 等待任务完成
     * 轮询任务状态，直到任务停止或超时
     *
     * @param job 要等待的任务
     * @param timeout 超时时间（毫秒）
     * @return true表示任务完成，false表示超时
     */
    private boolean waitForJob(Job job, int timeout) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        while (true) {
            // 检查任务状态
            switch (job.status()) {
                case STOPPED:      // 任务已停止
                case TERMINATED:   // 任务已终止
                    return true;
            }
            // 检查是否超时
            if (System.currentTimeMillis() - startTime > timeout) {
                return false;
            }
            // 休眠100毫秒后继续检查
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // 线程被中断，忽略异常继续等待
            }
        }
    }

    /**
     * 创建错误结果
     *
     * @param commandLine 命令行字符串
     * @param errorMessage 错误消息
     * @return 包含错误信息的Map
     */
    private Map<String, Object> createErrorResult(String commandLine, String errorMessage) {
        Map<String, Object> result = new TreeMap<>();
        result.put("success", false);
        result.put("error", errorMessage);
        if (commandLine != null) {
            result.put("command", commandLine);
        }
        return result;
    }

    /**
     * 创建超时结果
     *
     * @param commandLine 命令行字符串
     * @param timeout 超时时间（毫秒）
     * @return 包含超时信息的Map
     */
    private Map<String, Object> createTimeoutResult(String commandLine, long timeout) {
        Map<String, Object> result = new TreeMap<>();
        result.put("command", commandLine);
        result.put("success", false);
        result.put("error", "Command timeout after " + timeout + " ms");
        result.put("timeout", true);
        result.put("executionTime", System.currentTimeMillis());
        return result;
    }

    /**
     * 更新会话输入状态
     * 通知客户端当前会话的输入状态（如允许输入、允许中断等）
     *
     * @param session 会话对象
     * @param inputStatus 输入状态
     */
    private void updateSessionInputStatus(Session session, InputStatus inputStatus) {
        // 获取会话的结果分发器
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            // 添加输入状态模型到结果中
            resultDistributor.appendResult(new InputStatusModel(inputStatus));
        }
    }

    /**
     * 创建任务（通过命令行字符串）
     *
     * @param line 命令行字符串
     * @param session 会话对象
     * @param resultDistributor 结果分发器
     * @return 创建的任务对象
     */
    private Job createJob(String line, Session session, ResultDistributor resultDistributor) {
        // 将命令行字符串转换为token列表，然后创建任务
        return createJob(CliTokens.tokenize(line), session, resultDistributor);
    }

    /**
     * 创建任务（通过token列表）
     *
     * @param args 命令行token列表
     * @param session 会话对象
     * @param resultDistributor 结果分发器
     * @return 创建的任务对象
     */
    private synchronized Job createJob(List<CliToken> args, Session session, ResultDistributor resultDistributor) {
        // 通过任务控制器创建任务
        Job job = jobController.createJob(commandManager, args, session, new JobHandler(session), new McpTerm(session), resultDistributor);
        return job;
    }

    /**
     * 会话未找到异常
     * 当指定的会话不存在时抛出此异常
     */
    public static class SessionNotFoundException extends RuntimeException {
        /**
         * 构造函数
         *
         * @param message 错误消息
         */
        public SessionNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * 任务监听器
     * 监听任务的生命周期事件，更新会话状态
     */
    private class JobHandler implements JobListener {
        // 关联的会话
        private final Session session;

        /**
         * 构造函数
         *
         * @param session 会话对象
         */
        public JobHandler(Session session) {
            this.session = session;
        }

        /**
         * 任务进入前台时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onForeground(Job job) {
            // 设置会话的前台任务
            session.setForegroundJob(job);
        }

        /**
         * 任务进入后台时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onBackground(Job job) {
            // 如果是当前的前台任务，清除前台任务引用并解锁会话
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
                session.unLock();
            }
        }

        /**
         * 任务终止时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onTerminated(Job job) {
            // 如果是当前的前台任务，清除前台任务引用并解锁会话
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
                session.unLock();
            }
        }

        /**
         * 任务挂起时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onSuspend(Job job) {
            // 如果是当前的前台任务，清除前台任务引用并解锁会话
            if (session.getForegroundJob() == job) {
                session.setForegroundJob(null);
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
                session.unLock();
            }
        }
    }

    /**
     * MCP（Model Context Protocol）终端实现
     * 为命令执行提供一个虚拟的终端环境
     * 大部分方法返回this以支持链式调用，实际操作为空实现
     */
    public static class McpTerm implements Term {
        // 关联的会话
        private Session session;

        /**
         * 构造函数
         *
         * @param session 会话对象
         */
        public McpTerm(Session session) {
            this.session = session;
        }

        /**
         * 设置大小调整处理器（空实现）
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term resizehandler(Handler<Void> handler) {
            return this;
        }

        /**
         * 获取终端类型
         *
         * @return 终端类型字符串"mcp"
         */
        @Override
        public String type() {
            return "mcp";
        }

        /**
         * 获取终端宽度
         *
         * @return 宽度值（固定返回1000）
         */
        @Override
        public int width() {
            return 1000;
        }

        /**
         * 获取终端高度
         *
         * @return 高度值（固定返回200）
         */
        @Override
        public int height() {
            return 200;
        }

        /**
         * 设置标准输入处理器（空实现）
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term stdinHandler(Handler<String> handler) {
            return this;
        }

        /**
         * 设置标准输出处理器（空实现）
         *
         * @param handler 处理器函数
         * @return this
         */
        @Override
        public Term stdoutHandler(Function<String, String> handler) {
            return this;
        }

        /**
         * 向终端写入数据（空实现）
         *
         * @param data 要写入的数据
         * @return this
         */
        @Override
        public Term write(String data) {
            return this;
        }

        /**
         * 获取最后访问时间
         *
         * @return 会话的最后访问时间
         */
        @Override
        public long lastAccessedTime() {
            return session.getLastAccessTime();
        }

        /**
         * 回显文本（空实现）
         *
         * @param text 要回显的文本
         * @return this
         */
        @Override
        public Term echo(String text) {
            return this;
        }

        /**
         * 设置会话（空实现）
         *
         * @param session 会话对象
         * @return this
         */
        @Override
        public Term setSession(Session session) {
            return this;
        }

        /**
         * 设置中断信号处理器（空实现）
         *
         * @param handler 信号处理器
         * @return this
         */
        @Override
        public Term interruptHandler(SignalHandler handler) {
            return this;
        }

        /**
         * 设置挂起信号处理器（空实现）
         *
         * @param handler 信号处理器
         * @return this
         */
        @Override
        public Term suspendHandler(SignalHandler handler) {
            return this;
        }

        /**
         * 读取一行输入（空实现）
         *
         * @param prompt 提示符
         * @param lineHandler 行处理器
         */
        @Override
        public void readline(String prompt, Handler<String> lineHandler) {

        }

        /**
         * 读取一行输入，支持自动完成（空实现）
         *
         * @param prompt 提示符
         * @param lineHandler 行处理器
         * @param completionHandler 自动完成处理器
         */
        @Override
        public void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler) {

        }

        /**
         * 设置关闭处理器（空实现）
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term closeHandler(Handler<Void> handler) {
            return this;
        }

        /**
         * 关闭终端（空实现）
         */
        @Override
        public void close() {

        }
    }
}

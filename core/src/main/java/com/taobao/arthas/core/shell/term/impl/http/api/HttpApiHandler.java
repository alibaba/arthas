package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.distribution.PackingResultDistributor;
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
import com.taobao.arthas.core.shell.history.HistoryManager;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSession;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.termd.core.function.Function;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * HTTP RESTful API 处理器
 * <p>
 * 该处理器负责处理 Arthas 的 HTTP RESTful API 请求，支持以下操作：
 * <ul>
 *   <li>INIT_SESSION: 初始化会话</li>
 *   <li>EXEC: 同步执行命令</li>
 *   <li>ASYNC_EXEC: 异步执行命令</li>
 *   <li>INTERRUPT_JOB: 中断任务</li>
 *   <li>PULL_RESULTS: 拉取执行结果</li>
 *   <li>SESSION_INFO: 获取会话信息</li>
 *   <li>JOIN_SESSION: 加入已有会话</li>
 *   <li>CLOSE_SESSION: 关闭会话</li>
 * </ul>
 * </p>
 *
 * @author gongdewei 2020-03-18
 */
public class HttpApiHandler {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(HttpApiHandler.class);

    /**
     * JSON 序列化过滤器数组
     * <p>
     * 用于在序列化对象时进行自定义处理
     * </p>
     */
    private static final ValueFilter[] JSON_FILTERS = new ValueFilter[] { new ObjectVOFilter() };

    /**
     * 一次性会话的会话键
     * <p>
     * 用于标记一次性会话，该会话在命令执行完毕后会被自动删除
     * </p>
     */
    private static final String ONETIME_SESSION_KEY = "oneTimeSession";

    /**
     * 默认执行超时时间（毫秒）
     * <p>
     * 同步执行命令时的默认超时时间为 30 秒
     * </p>
     */
    public static final int DEFAULT_EXEC_TIMEOUT = 30000;

    /**
     * 会话管理器
     * <p>
     * 用于创建、获取和管理 Arthas 会话
     * </p>
     */
    private final SessionManager sessionManager;

    /**
     * 命令管理器
     * <p>
     * 用于管理 Arthas 的内部命令
     * </p>
     */
    private final InternalCommandManager commandManager;

    /**
     * 任务控制器
     * <p>
     * 用于创建和管理命令执行任务
     * </p>
     */
    private final JobController jobController;

    /**
     * 历史记录管理器
     * <p>
     * 用于记录和管理命令历史
     * </p>
     */
    private final HistoryManager historyManager;

    /**
     * 构造函数
     *
     * @param historyManager  历史记录管理器
     * @param sessionManager  会话管理器
     */
    public HttpApiHandler(HistoryManager historyManager, SessionManager sessionManager) {
        this.historyManager = historyManager;
        this.sessionManager = sessionManager;
        commandManager = this.sessionManager.getCommandManager();
        jobController = this.sessionManager.getJobController();
    }

    /**
     * 处理 HTTP API 请求
     * <p>
     * 该方法是 HTTP API 的主入口，负责：
     * <ol>
     *   <li>验证 HTTP 方法（仅支持 POST）</li>
     *   <li>解析请求体</li>
     *   <li>处理请求</li>
     *   <li>构建响应</li>
     * </ol>
     * </p>
     *
     * @param ctx     通道处理器上下文
     * @param request HTTP 请求对象
     * @return HTTP 响应对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    public HttpResponse handle(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        ApiResponse result;
        String requestBody = null;
        String requestId = null;
        try {
            HttpMethod method = request.method();
            // 目前只支持 POST 方法
            if (HttpMethod.POST.equals(method)) {
                // 获取请求体
                requestBody = getBody(request);
                // 解析 API 请求
                ApiRequest apiRequest = parseRequest(requestBody);
                requestId = apiRequest.getRequestId();
                // 处理请求
                result = processRequest(ctx, apiRequest);
            } else {
                // 不支持的 HTTP 方法
                result = createResponse(ApiState.REFUSED, "Unsupported http method: " + method.name());
            }
        } catch (Throwable e) {
            // 处理过程中发生异常
            result = createResponse(ApiState.FAILED, "Process request error: " + e.getMessage());
            logger.error("arthas process http api request error: " + request.uri() + ", request body: " + requestBody, e);
        }
        if (result == null) {
            // 如果结果为空，返回失败响应
            result = createResponse(ApiState.FAILED, "The request was not processed");
        }
        // 设置请求 ID
        result.setRequestId(requestId);

        // 序列化为 JSON
        byte[] jsonBytes = JSON.toJSONBytes(result, JSON_FILTERS);

        // create http response
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(jsonBytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
        return response;
    }

    /**
     * 解析 API 请求
     * <p>
     * 将 JSON 格式的请求体解析为 ApiRequest 对象
     * </p>
     *
     * @param requestBody JSON 格式的请求体
     * @return 解析后的 ApiRequest 对象
     * @throws ApiException 如果请求体为空或解析失败
     */
    private ApiRequest parseRequest(String requestBody) throws ApiException {
        if (StringUtils.isBlank(requestBody)) {
            throw new ApiException("parse request failed: request body is empty");
        }
        try {
            //ObjectMapper objectMapper = new ObjectMapper();
            //return objectMapper.readValue(requestBody, ApiRequest.class);
            // 使用 FastJSON 解析
            return JSON.parseObject(requestBody, ApiRequest.class);
        } catch (Exception e) {
            throw new ApiException("parse request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 处理 API 请求
     * <p>
     * 根据请求的 action 类型分发到不同的处理方法：
     * <ul>
     *   <li>INIT_SESSION: 初始化新会话</li>
     *   <li>EXEC/ASYNC_EXEC: 执行命令</li>
     *   <li>其他: 需要已有会话</li>
     * </ul>
     * </p>
     * <p>
     * 该方法还会处理会话管理、用户认证信息传递等逻辑
     * </p>
     *
     * @param ctx        通道处理器上下文
     * @param apiRequest API 请求对象
     * @return API 响应对象
     */
    private ApiResponse processRequest(ChannelHandlerContext ctx, ApiRequest apiRequest) {

        String actionStr = apiRequest.getAction();
        try {
            // 验证 action 参数
            if (StringUtils.isBlank(actionStr)) {
                throw new ApiException("'action' is required");
            }
            ApiAction action;
            try {
                // 将 action 字符串转换为枚举
                action = ApiAction.valueOf(actionStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("unknown action: " + actionStr);
            }

            //no session required
            // INIT_SESSION 操作不需要已有会话
            if (ApiAction.INIT_SESSION.equals(action)) {
                return processInitSessionRequest(apiRequest);
            }

            //required session
            // 其他操作需要会话
            Session session = null;
            // EXEC 操作允许没有 sessionId（会创建一次性会话）
            boolean allowNullSession = ApiAction.EXEC.equals(action);
            String sessionId = apiRequest.getSessionId();
            if (StringUtils.isBlank(sessionId)) {
                if (!allowNullSession) {
                    throw new ApiException("'sessionId' is required");
                }
            } else {
                // 获取已有会话
                session = sessionManager.getSession(sessionId);
                if (session == null) {
                    throw new ApiException("session not found: " + sessionId);
                }
                // 更新会话最后访问时间
                sessionManager.updateAccessTime(session);
            }

            // 标记所谓的一次性session
            if (session == null) {
                // 创建一个一次性会话，用于单次命令执行
                session = sessionManager.createSession();
                session.put(ONETIME_SESSION_KEY, new Object());
            }

            // 请求到达这里，如果有需要鉴权，则已经在前面的handler里处理过了
            // 如果有鉴权取到的 Subject，则传递到 arthas的session里
            HttpSession httpSession = HttpSessionManager.getHttpSessionFromContext(ctx);
            if (httpSession != null) {
                // 从 HTTP 会话中获取认证主体并传递到 Arthas 会话
                Object subject = httpSession.getAttribute(ArthasConstants.SUBJECT_KEY);
                if (subject != null) {
                    session.put(ArthasConstants.SUBJECT_KEY, subject);
                }
                // get userId from httpSession
                Object userId = httpSession.getAttribute(ArthasConstants.USER_ID_KEY);
                if (userId != null && session.getUserId() == null) {
                    session.setUserId((String) userId);
                }
            }

            // set userId from apiRequest if provided
            // 如果请求中提供了 userId，则设置到会话中
            if (!StringUtils.isBlank(apiRequest.getUserId())) {
                session.setUserId(apiRequest.getUserId());
            }

            //dispatch requests
            // 根据 action 类型分发请求
            ApiResponse response = dispatchRequest(action, apiRequest, session);
            if (response != null) {
                return response;
            }

        } catch (ApiException e) {
            logger.info("process http api request failed: {}", e.getMessage());
            return createResponse(ApiState.FAILED, e.getMessage());
        } catch (Throwable e) {
            logger.error("process http api request failed: " + e.getMessage(), e);
            return createResponse(ApiState.FAILED, "process http api request failed: " + e.getMessage());
        }

        return createResponse(ApiState.REFUSED, "Unsupported action: " + actionStr);
    }

    /**
     * 分发请求到具体的处理方法
     * <p>
     * 根据 ApiAction 类型调用相应的处理方法
     * </p>
     *
     * @param action     操作类型
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象
     * @throws ApiException 处理过程中可能抛出的异常
     */
    private ApiResponse dispatchRequest(ApiAction action, ApiRequest apiRequest, Session session) throws ApiException {
        switch (action) {
            case EXEC:
                return processExecRequest(apiRequest, session);
            case ASYNC_EXEC:
                return processAsyncExecRequest(apiRequest, session);
            case INTERRUPT_JOB:
                return processInterruptJob(apiRequest, session);
            case PULL_RESULTS:
                return processPullResultsRequest(apiRequest, session);
            case SESSION_INFO:
                return processSessionInfoRequest(apiRequest, session);
            case JOIN_SESSION:
                return processJoinSessionRequest(apiRequest, session);
            case CLOSE_SESSION:
                return processCloseSessionRequest(apiRequest, session);
            case INIT_SESSION:
                break;
        }
        return null;
    }

    /**
     * 处理初始化会话请求
     * <p>
     * 创建一个新的 Arthas 会话，包括：
     * <ul>
     *   <li>创建会话对象</li>
     *   <li>创建结果分发器和消费者</li>
     *   <li>发送欢迎消息</li>
     *   <li>设置会话允许输入状态</li>
     * </ul>
     * </p>
     *
     * @param apiRequest API 请求对象
     * @return API 响应对象，包含会话 ID 和消费者 ID
     * @throws ApiException 如果创建会话失败
     */
    private ApiResponse processInitSessionRequest(ApiRequest apiRequest) throws ApiException {
        ApiResponse response = new ApiResponse();

        //create session
        Session session = sessionManager.createSession();
        if (session != null) {

            // set userId if provided
            // 如果请求中提供了 userId，则设置到会话中
            if (!StringUtils.isBlank(apiRequest.getUserId())) {
                session.setUserId(apiRequest.getUserId());
            }

            //Result Distributor
            // 创建共享结果分发器，用于将命令执行结果分发给多个消费者
            SharingResultDistributorImpl resultDistributor = new SharingResultDistributorImpl(session);
            //create consumer
            // 创建结果消费者
            ResultConsumer resultConsumer = new ResultConsumerImpl();
            resultDistributor.addConsumer(resultConsumer);
            session.setResultDistributor(resultDistributor);

            // 发送欢迎消息
            resultDistributor.appendResult(new MessageModel("Welcome to arthas!"));

            //welcome message
            // 创建欢迎消息模型，包含版本、文档链接等信息
            WelcomeModel welcomeModel = new WelcomeModel();
            welcomeModel.setVersion(ArthasBanner.version());
            welcomeModel.setWiki(ArthasBanner.wiki());
            welcomeModel.setTutorials(ArthasBanner.tutorials());
            welcomeModel.setMainClass(PidUtils.mainClass());
            welcomeModel.setPid(PidUtils.currentPid());
            welcomeModel.setTime(DateUtils.getCurrentDateTime());
            resultDistributor.appendResult(welcomeModel);

            //allow input
            // 更新会话输入状态为允许输入
            updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);

            // 设置响应
            response.setSessionId(session.getSessionId())
                    .setConsumerId(resultConsumer.getConsumerId())
                    .setState(ApiState.SUCCEEDED);
        } else {
            throw new ApiException("create api session failed");
        }
        return response;
    }

    /**
     * 更新会话输入状态
     * <p>
     * 向所有消费者发送输入状态变更通知
     * </p>
     *
     * @param session     Arthas 会话
     * @param inputStatus 新的输入状态
     */
    private void updateSessionInputStatus(Session session, InputStatus inputStatus) {
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            // 将输入状态变更作为结果发送给所有消费者
            resultDistributor.appendResult(new InputStatusModel(inputStatus));
        }
    }

    /**
     * 处理加入会话请求
     * <p>
     * 创建一个新的结果消费者加入已有会话，可以接收该会话的命令执行结果
     * </p>
     *
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象
     */
    private ApiResponse processJoinSessionRequest(ApiRequest apiRequest, Session session) {

        //create consumer
        // 创建新的结果消费者
        ResultConsumer resultConsumer = new ResultConsumerImpl();
        //disable input and interrupt
        // 对于加入的消费者，禁用输入和中断功能
        resultConsumer.appendResult(new InputStatusModel(InputStatus.DISABLED));
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            // 将消费者添加到结果分发器
            resultDistributor.addConsumer(resultConsumer);
        }

        ApiResponse response = new ApiResponse();
        response.setSessionId(session.getSessionId())
                .setConsumerId(resultConsumer.getConsumerId())
                .setState(ApiState.SUCCEEDED);
        return response;
    }

    /**
     * 处理会话信息请求
     * <p>
     * 返回会话的基本信息，包括 PID、创建时间、最后访问时间等
     * </p>
     *
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象，包含会话信息
     */
    private ApiResponse processSessionInfoRequest(ApiRequest apiRequest, Session session) {
        ApiResponse response = new ApiResponse();
        Map<String, Object> body = new TreeMap<String, Object>();
        // 添加进程 ID
        body.put("pid", session.getPid());
        // 添加会话创建时间
        body.put("createTime", session.getCreateTime());
        // 添加最后访问时间
        body.put("lastAccessTime", session.getLastAccessTime());

        response.setState(ApiState.SUCCEEDED)
                .setSessionId(session.getSessionId())
                //.setConsumerId(consumerId)
                .setBody(body);
        return response;
    }

    /**
     * 处理关闭会话请求
     * <p>
     * 从会话管理器中移除指定会话
     * </p>
     *
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象
     */
    private ApiResponse processCloseSessionRequest(ApiRequest apiRequest, Session session) {
        // 从会话管理器中移除会话
        sessionManager.removeSession(session.getSessionId());
        ApiResponse response = new ApiResponse();
        response.setState(ApiState.SUCCEEDED);
        return response;
    }

    /**
     * 处理同步执行命令请求
     * <p>
     * 同步执行命令，等待任务完成或超时，然后立即返回所有结果。
     * 适用于需要一次性获取命令执行结果的场景。
     * </p>
     * <p>
     * 执行流程：
     * <ol>
     *   <li>检查会话锁定状态</li>
     *   <li>创建任务并执行</li>
     *   <li>等待任务完成或超时</li>
     *   <li>收集并返回所有结果</li>
     * </ol>
     * </p>
     *
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象，包含命令执行结果
     */
    private ApiResponse processExecRequest(ApiRequest apiRequest, Session session) {
        // 检查是否为一次性会话
        boolean oneTimeAccess = false;
        if (session.get(ONETIME_SESSION_KEY) != null) {
            oneTimeAccess = true;
        }

        try {
            String commandLine = apiRequest.getCommand();
            Map<String, Object> body = new TreeMap<String, Object>();
            body.put("command", commandLine);

            ApiResponse response = new ApiResponse();
            response.setSessionId(session.getSessionId())
                    .setBody(body);

            // 尝试获取会话锁
            if (!session.tryLock()) {
                response.setState(ApiState.REFUSED)
                        .setMessage("Another command is executing.");
                return response;
            }

            int lock = session.getLock();
            PackingResultDistributor packingResultDistributor = null;
            Job job = null;
            try {
                // 检查是否有前台任务正在运行
                Job foregroundJob = session.getForegroundJob();
                if (foregroundJob != null) {
                    response.setState(ApiState.REFUSED)
                            .setMessage("Another job is running.");
                    logger.info("Another job is running, jobId: {}", foregroundJob.id());
                    return response;
                }

                // 创建结果打包分发器，用于收集命令执行结果
                packingResultDistributor = new PackingResultDistributorImpl(session);
                //distribute result message both to origin session channel and request channel by CompositeResultDistributor
                //ResultDistributor resultDistributor = new CompositeResultDistributorImpl(packingResultDistributor, session.getResultDistributor());
                // 创建任务
                job = this.createJob(commandLine, session, packingResultDistributor);
                session.setForegroundJob(job);
                // 更新会话状态为允许中断
                updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

                // 运行任务
                job.run();

            } catch (Throwable e) {
                logger.error("Exec command failed:" + e.getMessage() + ", command:" + commandLine, e);
                response.setState(ApiState.FAILED).setMessage("Exec command failed:" + e.getMessage());
                return response;
            } finally {
                // 确保释放会话锁
                if (session.getLock() == lock) {
                    session.unLock();
                }
            }

            //wait for job completed or timeout
            // 等待任务完成或超时
            Integer timeout = apiRequest.getExecTimeout();
            if (timeout == null || timeout <= 0) {
                timeout = DEFAULT_EXEC_TIMEOUT;
            }
            boolean timeExpired = !waitForJob(job, timeout);
            if (timeExpired) {
                // 超时，强制中断任务
                logger.warn("Job is exceeded time limit, force interrupt it, jobId: {}", job.id());
                job.interrupt();
                response.setState(ApiState.INTERRUPTED).setMessage("The job is exceeded time limit, force interrupt");
            } else {
                // 任务正常完成
                response.setState(ApiState.SUCCEEDED);
            }

            //packing results
            // 打包结果
            body.put("jobId", job.id());
            body.put("jobStatus", job.status());
            body.put("timeExpired", timeExpired);
            if (timeExpired) {
                body.put("timeout", timeout);
            }
            body.put("results", packingResultDistributor.getResults());

            response.setSessionId(session.getSessionId())
                    //.setConsumerId(consumerId)
                    .setBody(body);
            return response;
        } finally {
            // 如果是一次性会话，执行完毕后删除
            if (oneTimeAccess) {
                sessionManager.removeSession(session.getSessionId());
            }
        }
    }

    /**
     * 处理异步执行命令请求
     * <p>
     * 异步执行命令，立即返回，不等待任务完成。
     * 客户端需要通过 PULL_RESULTS 操作拉取执行结果。
     * </p>
     * <p>
     * 适用于长时间运行的命令或需要流式输出结果的场景。
     * </p>
     *
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象，包含任务 ID
     */
    private ApiResponse processAsyncExecRequest(ApiRequest apiRequest, Session session) {
        String commandLine = apiRequest.getCommand();
        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("command", commandLine);

        ApiResponse response = new ApiResponse();
        response.setSessionId(session.getSessionId())
                .setBody(body);

        // 尝试获取会话锁
        if (!session.tryLock()) {
            response.setState(ApiState.REFUSED)
                    .setMessage("Another command is executing.");
            return response;
        }
        int lock = session.getLock();
        try {

            // 检查是否有前台任务正在运行
            Job foregroundJob = session.getForegroundJob();
            if (foregroundJob != null) {
                response.setState(ApiState.REFUSED)
                        .setMessage("Another job is running.");
                logger.info("Another job is running, jobId: {}", foregroundJob.id());
                return response;
            }

            //create job
            // 创建异步任务
            Job job = this.createJob(commandLine, session, session.getResultDistributor());
            body.put("jobId", job.id());
            body.put("jobStatus", job.status());
            response.setState(ApiState.SCHEDULED);

            //add command before exec job
            // 添加命令请求模型，记录命令开始执行
            CommandRequestModel commandRequestModel = new CommandRequestModel(commandLine, response.getState().name());
            commandRequestModel.setJobId(job.id());
            SharingResultDistributor resultDistributor = session.getResultDistributor();
            if (resultDistributor != null) {
                resultDistributor.appendResult(commandRequestModel);
            }
            session.setForegroundJob(job);
            // 更新会话状态为允许中断
            updateSessionInputStatus(session, InputStatus.ALLOW_INTERRUPT);

            //run job
            // 异步运行任务
            job.run();

            return response;
        } catch (Throwable e) {
            logger.error("Async exec command failed:" + e.getMessage() + ", command:" + commandLine, e);
            response.setState(ApiState.FAILED).setMessage("Async exec command failed:" + e.getMessage());
            // 记录失败状态
            CommandRequestModel commandRequestModel = new CommandRequestModel(commandLine, response.getState().name(), response.getMessage());
            session.getResultDistributor().appendResult(commandRequestModel);
            return response;
        } finally {
            // 确保释放会话锁
            if (session.getLock() == lock) {
                session.unLock();
            }
        }
    }

    /**
     * 处理中断任务请求
     * <p>
     * 中断当前正在运行的前台任务
     * </p>
     *
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象
     */
    private ApiResponse processInterruptJob(ApiRequest apiRequest, Session session) {
        // 获取前台任务
        Job job = session.getForegroundJob();
        if (job == null) {
            return new ApiResponse().setState(ApiState.FAILED).setMessage("no foreground job is running");
        }
        // 中断任务
        job.interrupt();

        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("jobId", job.id());
        body.put("jobStatus", job.status());
        return new ApiResponse()
                .setState(ApiState.SUCCEEDED)
                .setBody(body);
    }

    /**
     * 处理拉取结果请求
     * <p>
     * 从结果队列中拉取命令执行结果。
     * 用于异步执行命令后，客户端主动拉取执行结果的场景。
     * </p>
     *
     * @param apiRequest API 请求对象
     * @param session    Arthas 会话
     * @return API 响应对象，包含拉取到的结果
     * @throws ApiException 如果 consumerId 无效
     */
    private ApiResponse processPullResultsRequest(ApiRequest apiRequest, Session session) throws ApiException {
        String consumerId = apiRequest.getConsumerId();
        if (StringUtils.isBlank(consumerId)) {
            throw new ApiException("'consumerId' is required");
        }
        // 获取结果消费者
        ResultConsumer consumer = null;
        SharingResultDistributor resultDistributor = session.getResultDistributor();
        if (resultDistributor != null) {
            consumer = resultDistributor.getConsumer(consumerId);
        }
        if (consumer == null) {
            throw new ApiException("consumer not found: " + consumerId);
        }

        // 从消费者队列中拉取结果
        List<ResultModel> results = consumer.pollResults();
        Map<String, Object> body = new TreeMap<String, Object>();
        body.put("results", results);

        ApiResponse response = new ApiResponse();
        response.setState(ApiState.SUCCEEDED)
                .setSessionId(session.getSessionId())
                .setConsumerId(consumerId)
                .setBody(body);
        return response;
    }

    /**
     * 等待任务完成或超时
     * <p>
     * 轮询检查任务状态，直到任务停止/终止或超时
     * </p>
     *
     * @param job     任务对象
     * @param timeout 超时时间（毫秒）
     * @return 如果任务在超时前完成返回 true，否则返回 false
     */
    private boolean waitForJob(Job job, int timeout) {
        long startTime = System.currentTimeMillis();
        while (true) {
            // 检查任务状态
            switch (job.status()) {
                case STOPPED:
                case TERMINATED:
                    return true;
            }
            // 检查是否超时
            if (System.currentTimeMillis() - startTime > timeout) {
                return false;
            }
            // 休眠 100ms 后继续检查
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // 被中断，继续循环
            }
        }
    }

    /**
     * 创建任务（基于命令行参数列表）
     * <p>
     * 使用命令行参数列表创建一个 Arthas 任务
     * </p>
     *
     * @param args              命令行参数列表
     * @param session           Arthas 会话
     * @param resultDistributor 结果分发器
     * @return 创建的任务对象
     */
    private synchronized Job createJob(List<CliToken> args, Session session, ResultDistributor resultDistributor) {
        Job job = jobController.createJob(commandManager, args, session, new ApiJobHandler(session), new ApiTerm(session), resultDistributor);
        return job;
    }

    /**
     * 创建任务（基于命令行字符串）
     * <p>
     * 将命令行字符串解析为参数列表，然后创建任务
     * </p>
     *
     * @param line              命令行字符串
     * @param session           Arthas 会话
     * @param resultDistributor 结果分发器
     * @return 创建的任务对象
     */
    private Job createJob(String line, Session session, ResultDistributor resultDistributor) {
        // 添加到历史记录
        historyManager.addHistory(line);
        // 将命令行字符串解析为参数列表
        return createJob(CliTokens.tokenize(line), session, resultDistributor);
    }

    /**
     * 创建 API 响应对象
     * <p>
     * 创建一个简单的响应对象，包含状态和消息
     * </p>
     *
     * @param apiState API 状态
     * @param message  响应消息
     * @return API 响应对象
     */
    private ApiResponse createResponse(ApiState apiState, String message) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setState(apiState);
        apiResponse.setMessage(message);
        return apiResponse;
    }

    /**
     * 获取 HTTP 请求体
     * <p>
     * 从 HTTP 请求中提取请求体内容
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 请求体字符串
     */
    private String getBody(FullHttpRequest request) {
        ByteBuf buf = request.content();
        return buf.toString(CharsetUtil.UTF_8);
    }

    /**
     * API 任务监听器
     * <p>
     * 监听任务生命周期事件，管理会话的前台任务和输入状态
     * </p>
     */
    private class ApiJobHandler implements JobListener {

        /**
         * 关联的 Arthas 会话
         */
        private Session session;

        /**
         * 构造函数
         *
         * @param session Arthas 会话
         */
        public ApiJobHandler(Session session) {
            this.session = session;
        }

        /**
         * 任务进入前台时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onForeground(Job job) {
            // 设置前台任务
            session.setForegroundJob(job);
        }

        /**
         * 任务进入后台时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onBackground(Job job) {
            if (session.getForegroundJob() == job) {
                // 清除前台任务
                session.setForegroundJob(null);
                // 更新输入状态为允许输入
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
            }
        }

        /**
         * 任务终止时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onTerminated(Job job) {
            if (session.getForegroundJob() == job) {
                // 清除前台任务
                session.setForegroundJob(null);
                // 更新输入状态为允许输入
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
            }
        }

        /**
         * 任务挂起时调用
         *
         * @param job 任务对象
         */
        @Override
        public void onSuspend(Job job) {
            if (session.getForegroundJob() == job) {
                // 清除前台任务
                session.setForegroundJob(null);
                // 更新输入状态为允许输入
                updateSessionInputStatus(session, InputStatus.ALLOW_INPUT);
            }
        }
    }

    /**
     * API 终端实现
     * <p>
     * 为 HTTP API 提供一个简单的终端实现，大部分方法为空实现，
     * 因为 HTTP API 不需要真正的终端交互功能
     * </p>
     */
    private static class ApiTerm implements Term {

        /**
         * 关联的 Arthas 会话
         */
        private Session session;

        /**
         * 构造函数
         *
         * @param session Arthas 会话
         */
        public ApiTerm(Session session) {
            this.session = session;
        }

        /**
         * 设置终端大小调整处理器
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
         * @return 终端类型，返回 "web"
         */
        @Override
        public String type() {
            return "web";
        }

        /**
         * 获取终端宽度
         *
         * @return 终端宽度，固定返回 1000
         */
        @Override
        public int width() {
            return 1000;
        }

        /**
         * 获取终端高度
         *
         * @return 终端高度，固定返回 200
         */
        @Override
        public int height() {
            return 200;
        }

        /**
         * 设置标准输入处理器
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term stdinHandler(Handler<String> handler) {
            return this;
        }

        /**
         * 设置标准输出处理器
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term stdoutHandler(Function<String, String> handler) {
            return this;
        }

        /**
         * 写入数据到终端
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
         * @return 最后访问时间戳
         */
        @Override
        public long lastAccessedTime() {
            return session.getLastAccessTime();
        }

        /**
         * 回显文本
         *
         * @param text 要回显的文本
         * @return this
         */
        @Override
        public Term echo(String text) {
            return this;
        }

        /**
         * 设置会话
         *
         * @param session 会话对象
         * @return this
         */
        @Override
        public Term setSession(Session session) {
            return this;
        }

        /**
         * 设置中断信号处理器
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term interruptHandler(SignalHandler handler) {
            return this;
        }

        /**
         * 设置挂起信号处理器
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term suspendHandler(SignalHandler handler) {
            return this;
        }

        /**
         * 读取一行输入
         *
         * @param prompt       提示符
         * @param lineHandler  行处理器
         */
        @Override
        public void readline(String prompt, Handler<String> lineHandler) {
            // 空实现
        }

        /**
         * 读取一行输入（带自动补全）
         *
         * @param prompt             提示符
         * @param lineHandler        行处理器
         * @param completionHandler  补全处理器
         */
        @Override
        public void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler) {
            // 空实现
        }

        /**
         * 设置关闭处理器
         *
         * @param handler 处理器
         * @return this
         */
        @Override
        public Term closeHandler(Handler<Void> handler) {
            return this;
        }

        /**
         * 关闭终端
         */
        @Override
        public void close() {
            // 空实现
        }
    }
}

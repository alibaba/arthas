package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MCP Server 的命令执行上下文。
 * <p>
 * 该类封装了单次命令执行所需的全部状态，包括：
 * <ul>
 *   <li>命令执行器（{@link CommandExecutor}）的引用</li>
 *   <li>与 MCP Session 绑定的 Arthas Session 信息（可选）</li>
 *   <li>命令执行完成标志</li>
 *   <li>命令执行结果列表及对应的锁</li>
 * </ul>
 * <p>
 * 支持两种使用模式：
 * <ol>
 *   <li><b>临时模式</b>：通过 {@code ArthasCommandContext(CommandExecutor)} 构造，
 *       不绑定 Session，仅支持同步执行命令。</li>
 *   <li><b>Session 模式</b>：通过 {@code ArthasCommandContext(CommandExecutor, CommandSessionBinding)} 构造，
 *       绑定具体的 Arthas Session，支持异步执行、拉取结果、中断任务等操作。</li>
 * </ol>
 */
public class ArthasCommandContext {

    private static final Logger logger = LoggerFactory.getLogger(ArthasCommandContext.class);

    /** 同步执行命令的默认超时时间，单位毫秒（30 秒） */
    private static final long DEFAULT_SYNC_TIMEOUT = 30000L;

    /** 命令执行器，负责与 Arthas Agent 通信并执行具体命令 */
    private final CommandExecutor commandExecutor;

    /**
     * MCP Session 与 Arthas Session 的绑定关系。
     * 在临时模式下为 {@code null}，在 Session 模式下不为 {@code null}。
     */
    private final ArthasCommandSessionManager.CommandSessionBinding binding;

    /**
     * 命令执行完成标志。
     * 使用 {@code volatile} 保证多线程可见性，在命令执行完毕后由执行线程设置为 {@code true}。
     */
    private volatile boolean executionComplete = false;

    /**
     * 命令执行结果列表。
     * 使用 {@link CopyOnWriteArrayList} 以支持多线程并发写入结果，同时保证读操作的线程安全。
     */
    private final List<Object> results = new CopyOnWriteArrayList<>();

    /**
     * 结果操作锁，用于在需要对结果列表进行复合操作时保证原子性。
     * 例如：先检查结果数量再批量消费的场景。
     */
    private final Lock resultLock = new ReentrantLock();

    /**
     * 临时模式构造方法，不绑定任何 Session。
     * <p>
     * 使用此构造方法创建的上下文不支持异步执行（{@link #executeAsync}）、
     * 拉取结果（{@link #pullResults}）、中断任务（{@link #interruptJob}）等
     * 依赖 Session 的操作，调用这些方法时会抛出 {@link IllegalStateException}。
     *
     * @param commandExecutor 命令执行器，不能为 {@code null}
     * @throws NullPointerException 若 {@code commandExecutor} 为 {@code null}
     */
    public ArthasCommandContext(CommandExecutor commandExecutor) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor cannot be null");
        this.binding = null;
    }

    /**
     * Session 模式构造方法，绑定指定的 MCP-Arthas Session。
     * <p>
     * 使用此构造方法创建的上下文支持全部命令操作，包括异步执行、拉取结果、中断任务等。
     *
     * @param commandExecutor 命令执行器，不能为 {@code null}
     * @param binding         MCP Session 与 Arthas Session 的绑定关系，可以为 {@code null}（退化为临时模式）
     * @throws NullPointerException 若 {@code commandExecutor} 为 {@code null}
     */
    public ArthasCommandContext(CommandExecutor commandExecutor, ArthasCommandSessionManager.CommandSessionBinding binding) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor cannot be null");
        this.binding = binding;
    }

    /**
     * 获取命令执行器。
     *
     * @return 当前上下文关联的 {@link CommandExecutor} 实例
     */
    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    /**
     * 获取当前绑定的 Arthas Session ID。
     * <p>
     * 若处于临时模式（{@link #binding} 为 {@code null}），则返回 {@code null}。
     *
     * @return Arthas Session ID，临时模式下返回 {@code null}
     */
    public String getSessionId() {
        return binding != null ? binding.getArthasSessionId() : null;
    }

    /**
     * 获取 Arthas Session ID（与 {@link #getSessionId()} 功能相同，保留用于兼容性）。
     * <p>
     * 与 {@link #getSessionId()} 不同，此方法要求必须处于 Session 模式，
     * 若处于临时模式（{@link #binding} 为 {@code null}），则抛出 {@link IllegalStateException}。
     *
     * @return Arthas Session ID，不会为 {@code null}
     * @throws IllegalStateException 若当前处于临时模式，未绑定 Session
     */
    public String getArthasSessionId() {
        requireSessionSupport();
        return binding.getArthasSessionId();
    }

    /**
     * 检查当前上下文是否处于 Session 模式，若处于临时模式则抛出异常。
     * <p>
     * 所有依赖 Session 绑定的方法在执行前均需调用此方法做前置校验，
     * 避免在临时模式下调用 Session 相关操作导致空指针异常。
     *
     * @throws IllegalStateException 若 {@link #binding} 为 {@code null}，即当前为临时模式
     */
    private void requireSessionSupport() {
        if (binding == null) {
            throw new IllegalStateException("Session-based operations are not supported in temporary mode. " +
                    "Use ArthasCommandContext(CommandExecutor, CommandSessionBinding) constructor to enable session support.");
        }
    }

    /**
     * 获取当前绑定的消费者 ID（Consumer ID）。
     * <p>
     * Consumer ID 由 Arthas Agent 在创建 Session 时分配，
     * 用于从 Arthas 异步命令结果队列中拉取属于当前消费者的结果。
     * 临时模式下返回 {@code null}。
     *
     * @return 消费者 ID，临时模式下返回 {@code null}
     */
    public String getConsumerId() {
        return binding != null ? binding.getConsumerId() : null;
    }

    /**
     * 获取当前的 Session 绑定对象。
     * <p>
     * 返回的对象包含 MCP Session ID、Arthas Session ID、Consumer ID 及时间戳信息。
     * 临时模式下返回 {@code null}。
     *
     * @return Session 绑定对象，临时模式下返回 {@code null}
     */
    public ArthasCommandSessionManager.CommandSessionBinding getBinding() {
        return binding;
    }

    /**
     * 判断命令是否已执行完成。
     * <p>
     * 该标志由命令执行回调或轮询逻辑在确认命令执行完毕后设置为 {@code true}。
     *
     * @return 若命令已执行完成则返回 {@code true}，否则返回 {@code false}
     */
    public boolean isExecutionComplete() {
        return executionComplete;
    }

    /**
     * 设置命令执行完成标志。
     *
     * @param executionComplete {@code true} 表示命令执行已完成，{@code false} 表示尚未完成
     */
    public void setExecutionComplete(boolean executionComplete) {
        this.executionComplete = executionComplete;
    }

    /**
     * 向结果列表中添加一条执行结果。
     * <p>
     * 底层使用 {@link CopyOnWriteArrayList}，支持多线程并发写入，无需外部同步。
     *
     * @param result 待添加的结果对象
     */
    public void addResult(Object result) {
        results.add(result);
    }

    /**
     * 获取当前所有已收集的执行结果列表。
     * <p>
     * 返回的列表是底层 {@link CopyOnWriteArrayList} 的引用，读取操作是线程安全的，
     * 但若需要与写操作配合做复合操作，请使用 {@link #getResultLock()} 加锁。
     *
     * @return 执行结果列表，不会为 {@code null}
     */
    public List<Object> getResults() {
        return results;
    }

    /**
     * 清空已收集的所有执行结果。
     * <p>
     * 通常在命令重新执行前调用，以清除上一次执行遗留的结果数据。
     */
    public void clearResults() {
        results.clear();
    }

    /**
     * 获取结果操作锁。
     * <p>
     * 当需要对结果列表执行"检查-操作"等复合原子操作时，
     * 调用方需先获取此锁，再进行操作，使用完毕后务必在 {@code finally} 中释放锁，例如：
     * <pre>{@code
     * Lock lock = context.getResultLock();
     * lock.lock();
     * try {
     *     // 复合操作
     * } finally {
     *     lock.unlock();
     * }
     * }</pre>
     *
     * @return 结果操作的 {@link ReentrantLock} 锁对象
     */
    public Lock getResultLock() {
        return resultLock;
    }

    /**
     * 使用默认超时时间（{@value #DEFAULT_SYNC_TIMEOUT} 毫秒）同步执行命令。
     * <p>
     * 该方法会阻塞调用线程，直至命令执行完毕或超时。
     *
     * @param commandLine 要执行的 Arthas 命令字符串
     * @return 命令执行结果，格式由 {@link CommandExecutor#executeSync} 定义
     */
    public Map<String, Object> executeSync(String commandLine) {
        return executeSync(commandLine, DEFAULT_SYNC_TIMEOUT);
    }

    /**
     * 使用指定超时时间同步执行命令。
     * <p>
     * 该方法会阻塞调用线程，直至命令执行完毕或超过指定超时时间。
     *
     * @param commandLine 要执行的 Arthas 命令字符串
     * @param timeout     超时时间，单位毫秒
     * @return 命令执行结果，格式由 {@link CommandExecutor#executeSync} 定义
     */
    public Map<String, Object> executeSync(String commandLine, long timeout) {
        return commandExecutor.executeSync(commandLine, timeout);
    }

    /**
     * 携带认证主体信息，使用默认超时时间同步执行命令。
     * <p>
     * 认证主体（authSubject）用于在多租户场景下鉴权，确保命令在授权的上下文中执行。
     * 此重载不传入 {@code userId}，统计上报时用户 ID 为 {@code null}。
     *
     * @param commandStr  要执行的 Arthas 命令字符串
     * @param authSubject 认证主体对象，用于鉴权，可以为 {@code null}
     * @return 命令执行结果
     */
    public Map<String, Object> executeSync(String commandStr, Object authSubject) {
        return commandExecutor.executeSync(commandStr, DEFAULT_SYNC_TIMEOUT, null, authSubject, null);
    }

    /**
     * 携带认证主体和用户 ID，使用默认超时时间同步执行命令。
     * <p>
     * 在同步执行的基础上，额外传入 {@code userId} 用于统计上报，
     * 方便在多用户场景下追踪每条命令的执行来源。
     *
     * @param commandStr  要执行的 Arthas 命令字符串
     * @param authSubject 认证主体对象，用于鉴权
     * @param userId      用户 ID，用于统计上报，可以为 {@code null}
     * @return 命令执行结果
     */
    public Map<String, Object> executeSync(String commandStr, Object authSubject, String userId) {
        return commandExecutor.executeSync(commandStr, DEFAULT_SYNC_TIMEOUT, null, authSubject, userId);
    }

    /**
     * 异步执行命令（仅 Session 模式可用）。
     * <p>
     * 命令提交后立即返回，不等待执行结果。
     * 调用方随后可通过 {@link #pullResults()} 轮询获取执行结果，
     * 或通过 {@link #interruptJob()} 中断正在执行的任务。
     * <p>
     * 该方法使用当前绑定的 Arthas Session ID 提交命令。
     *
     * @param commandLine 要异步执行的 Arthas 命令字符串
     * @return 命令提交的响应结果（不包含命令执行结果）
     * @throws IllegalStateException 若当前处于临时模式（未绑定 Session）
     */
    public Map<String, Object> executeAsync(String commandLine) {
        requireSessionSupport();
        return commandExecutor.executeAsync(commandLine, binding.getArthasSessionId());
    }

    /**
     * 拉取异步命令的执行结果（仅 Session 模式可用）。
     * <p>
     * 通过当前绑定的 Arthas Session ID 和 Consumer ID，
     * 从 Arthas Agent 的结果队列中拉取待消费的命令输出。
     * 通常需要轮询调用，直至收到命令执行完成的信号。
     *
     * @return 拉取到的命令执行结果，若暂无新结果则返回空结构
     * @throws IllegalStateException 若当前处于临时模式（未绑定 Session）
     */
    public Map<String, Object> pullResults() {
        requireSessionSupport();
        return commandExecutor.pullResults(binding.getArthasSessionId(), binding.getConsumerId());
    }

    /**
     * 中断当前 Session 中正在执行的任务（仅 Session 模式可用）。
     * <p>
     * 向 Arthas Agent 发送中断请求，终止当前 Session 中正在运行的前台任务，
     * 等效于在 Arthas 交互终端中按下 {@code Ctrl+C}。
     *
     * @return 中断操作的响应结果
     * @throws IllegalStateException 若当前处于临时模式（未绑定 Session）
     */
    public Map<String, Object> interruptJob() {
        requireSessionSupport();
        return commandExecutor.interruptJob(binding.getArthasSessionId());
    }

    /**
     * 为当前绑定的 Arthas Session 设置用户 ID（仅 Session 模式下生效）。
     * <p>
     * 用户 ID 主要用于统计上报，帮助平台追踪哪个用户发起了哪些诊断操作。
     * 若当前处于临时模式（{@link #binding} 为 {@code null}）或 {@code userId} 为 {@code null}，则不执行任何操作。
     *
     * @param userId 用户 ID，为 {@code null} 时跳过设置
     */
    public void setSessionUserId(String userId) {
        if (binding != null && userId != null) {
            // 调用执行器将 userId 与 Arthas Session 关联，后续统计上报时会使用该 ID
            commandExecutor.setSessionUserId(binding.getArthasSessionId(), userId);
            logger.debug("Set userId for session {}: {}", binding.getArthasSessionId(), userId);
        }
    }
}

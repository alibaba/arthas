package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Session 到 Arthas Command Session 的绑定管理器。
 * <p>
 * MCP 协议层维护着自己的 Session（{@code mcpSessionId}），
 * 而 Arthas Agent 内部也有自己的 Session（{@code arthasSessionId}）。
 * 本类负责在两者之间建立并维护一对一的映射关系，实现以下功能：
 * <ul>
 *   <li>懒创建：首次访问某个 MCP Session 时自动向 Arthas Agent 申请 Command Session</li>
 *   <li>自动重建：当 Arthas Session 因超时失效时，透明地重建新 Session 并更新映射</li>
 *   <li>资源释放：提供显式关闭单个或全部 Session 的接口</li>
 * </ul>
 * <p>
 * 线程安全：内部使用 {@link ConcurrentHashMap} 存储映射，支持多线程并发访问。
 */
public class ArthasCommandSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(ArthasCommandSessionManager.class);

    /**
     * Session 过期判断阈值，单位毫秒（25 分钟）。
     * <p>
     * Arthas Agent 默认 Session 超时时间为 30 分钟。
     * 为在 Arthas 主动清理 Session 之前主动重建，此处将阈值设为 25 分钟：
     * 若距离上次访问超过该时长，则认为 Session 可能已失效，触发重建逻辑。
     */
    private static final long SESSION_EXPIRY_THRESHOLD_MS = 25 * 60 * 1000; // 25 分钟

    /** 命令执行器，用于与 Arthas Agent 通信，执行创建/关闭 Session 等操作 */
    private final CommandExecutor commandExecutor;

    /**
     * MCP Session ID 到 Arthas Command Session 绑定关系的映射表。
     * <p>
     * Key：MCP Session ID（由 MCP 协议层生成的字符串标识）
     * Value：{@link CommandSessionBinding}（包含对应的 Arthas Session 信息）
     * <p>
     * 使用 {@link ConcurrentHashMap} 保证多线程并发读写的线程安全。
     */
    private final ConcurrentHashMap<String, CommandSessionBinding> sessionBindings = new ConcurrentHashMap<>();

    /**
     * 构造方法，注入命令执行器。
     *
     * @param commandExecutor 命令执行器，不能为 {@code null}
     */
    public ArthasCommandSessionManager(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * MCP Session 与 Arthas Command Session 的绑定关系数据对象。
     * <p>
     * 封装了一次 Session 绑定的全部元信息，包括：
     * <ul>
     *   <li>MCP 侧的 Session ID</li>
     *   <li>Arthas Agent 侧的 Session ID</li>
     *   <li>Consumer ID（用于从 Arthas 结果队列中拉取结果）</li>
     *   <li>创建时间和最后访问时间（用于过期检测）</li>
     * </ul>
     */
    public static class CommandSessionBinding {
        /** MCP 协议层的 Session 标识符 */
        private final String mcpSessionId;

        /** Arthas Agent 内部的 Session 标识符，由 Arthas 在创建 Session 时分配 */
        private final String arthasSessionId;

        /**
         * 消费者 ID，由 Arthas 在创建 Session 时与 Session 一同返回。
         * 拉取异步命令结果时，需要同时提供 Session ID 和 Consumer ID，
         * 以确保每个调用方只消费属于自己的结果。
         */
        private final String consumerId;

        /** Session 绑定的创建时间戳，单位毫秒，创建后不可变 */
        private final long createdTime;

        /**
         * Session 最后一次被访问的时间戳，单位毫秒。
         * 使用 {@code volatile} 保证多线程可见性，每次通过 {@link #updateAccessTime()} 更新。
         */
        private volatile long lastAccessTime;

        /**
         * 构造 Session 绑定对象，并将创建时间和最后访问时间初始化为当前时间。
         *
         * @param mcpSessionId    MCP Session ID
         * @param arthasSessionId Arthas Agent 分配的 Session ID
         * @param consumerId      Arthas Agent 分配的 Consumer ID
         */
        public CommandSessionBinding(String mcpSessionId, String arthasSessionId, String consumerId) {
            this.mcpSessionId = mcpSessionId;
            this.arthasSessionId = arthasSessionId;
            this.consumerId = consumerId;
            this.createdTime = System.currentTimeMillis();
            // 初始最后访问时间等于创建时间
            this.lastAccessTime = this.createdTime;
        }

        /**
         * 获取 MCP 协议层的 Session ID。
         *
         * @return MCP Session ID
         */
        public String getMcpSessionId() {
            return mcpSessionId;
        }

        /**
         * 获取 Arthas Agent 内部的 Session ID。
         *
         * @return Arthas Session ID
         */
        public String getArthasSessionId() {
            return arthasSessionId;
        }

        /**
         * 获取 Consumer ID。
         * <p>
         * 该 ID 在拉取异步命令执行结果时使用，与 {@link #arthasSessionId} 配合
         * 从 Arthas Agent 的结果队列中定向拉取属于本消费者的数据。
         *
         * @return Consumer ID
         */
        public String getConsumerId() {
            return consumerId;
        }

        /**
         * 获取 Session 绑定的创建时间。
         *
         * @return 创建时间戳，单位毫秒
         */
        public long getCreatedTime() {
            return createdTime;
        }

        /**
         * 获取 Session 最后一次被访问的时间。
         *
         * @return 最后访问时间戳，单位毫秒
         */
        public long getLastAccessTime() {
            return lastAccessTime;
        }

        /**
         * 将最后访问时间更新为当前时间。
         * <p>
         * 每次通过 {@link ArthasCommandSessionManager#getCommandSession} 获取到该 Session 时，
         * 都应调用此方法以延续 Session 的有效期，防止被过早判定为过期。
         */
        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * 为指定的 MCP Session 向 Arthas Agent 申请一个新的 Command Session。
     * <p>
     * 内部调用 {@link CommandExecutor#createSession()} 向 Arthas Agent 发起创建请求，
     * 从响应中提取 {@code sessionId} 和 {@code consumerId}，构造并返回 {@link CommandSessionBinding}。
     * <p>
     * 注意：此方法仅创建绑定对象，<b>不会</b>将其存入 {@link #sessionBindings} 映射表，
     * 调用方（通常是 {@link #getCommandSession}）负责将返回的绑定对象放入映射表。
     *
     * @param mcpSessionId MCP Session ID，用于在绑定对象中记录关联关系
     * @return 包含新创建的 Arthas Session 信息的 {@link CommandSessionBinding} 对象
     */
    public CommandSessionBinding createCommandSession(String mcpSessionId) {
        // 调用命令执行器，向 Arthas Agent 发送创建 Session 请求
        Map<String, Object> result = commandExecutor.createSession();

        // 从响应结果中提取 sessionId 和 consumerId，构造绑定对象
        CommandSessionBinding binding = new CommandSessionBinding(
            mcpSessionId,
            (String) result.get("sessionId"),
            (String) result.get("consumerId")
        );

        return binding;
    }

    /**
     * 获取与指定 MCP Session 绑定的 Arthas Command Session，支持传入认证主体。
     * <p>
     * 该方法实现了完整的 Session 生命周期管理逻辑：
     * <ol>
     *   <li>若映射表中不存在对应绑定，则调用 {@link #createCommandSession} 创建新 Session 并存入映射表。</li>
     *   <li>若映射表中已存在对应绑定，但通过 {@link #isSessionValid} 判断 Session 已过期，
     *       则尝试关闭旧 Session（容忍关闭失败），重新创建新 Session 并替换映射表中的记录。</li>
     *   <li>若映射表中已存在有效绑定，则直接复用。</li>
     *   <li>无论哪种情况，均会调用 {@link CommandSessionBinding#updateAccessTime()} 更新最后访问时间。</li>
     *   <li>若 {@code authSubject} 不为 {@code null}，则调用 {@link CommandExecutor#setSessionAuth}
     *       将认证信息应用到当前 Session，失败时仅打印警告日志，不抛出异常。</li>
     * </ol>
     *
     * @param mcpSessionId MCP Session ID
     * @param authSubject  认证主体对象，用于多租户鉴权，可以为 {@code null}
     * @return 当前有效的 {@link CommandSessionBinding}，不会为 {@code null}
     */
    public CommandSessionBinding getCommandSession(String mcpSessionId, Object authSubject) {
        CommandSessionBinding binding = sessionBindings.get(mcpSessionId);

        if (binding == null) {
            // 首次访问，创建新的 Arthas Session 并记录绑定关系
            binding = createCommandSession(mcpSessionId);
            sessionBindings.put(mcpSessionId, binding);
            logger.debug("Created new command session: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());
        } else if (!isSessionValid(binding)) {
            // Session 判定为过期，记录日志后执行重建流程
            logger.info("Session expired, recreating: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());

            try {
                // 尝试通知 Arthas Agent 关闭已过期的旧 Session，释放服务端资源
                commandExecutor.closeSession(binding.getArthasSessionId());
            } catch (Exception e) {
                // 旧 Session 可能已被 Arthas Agent 自动清理，关闭失败时仅记录 debug 日志，不阻断重建流程
                logger.debug("Failed to close expired session (may already be cleaned up): {}", e.getMessage());
            }

            // 创建新 Session，替换映射表中的旧绑定
            CommandSessionBinding newBinding = createCommandSession(mcpSessionId);
            sessionBindings.put(mcpSessionId, newBinding);
            logger.info("Recreated command session: MCP={}, Old Arthas={}, New Arthas={}",
                       mcpSessionId, binding.getArthasSessionId(), newBinding.getArthasSessionId());
            binding = newBinding;
        } else {
            // Session 有效，直接复用，记录 debug 日志便于排查
            logger.debug("Using existing valid session: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());
        }

        // 每次获取 Session 后更新最后访问时间，延续 Session 有效期
        binding.updateAccessTime();

        if (authSubject != null) {
            try {
                // 将认证主体应用到 Arthas Session，用于后续命令执行时的权限校验
                commandExecutor.setSessionAuth(binding.getArthasSessionId(), authSubject);
                logger.debug("Applied auth to Arthas session: MCP={}, Arthas={}",
                           mcpSessionId, binding.getArthasSessionId());
            } catch (Exception e) {
                // 认证设置失败时仅打印警告，不抛出异常，避免因鉴权操作失败而中断正常业务
                logger.warn("Failed to apply auth to session: MCP={}, Arthas={}, error={}",
                          mcpSessionId, binding.getArthasSessionId(), e.getMessage());
            }
        }

        return binding;
    }

    /**
     * 检查指定的 Session 绑定是否仍然有效。
     * <p>
     * 当前实现基于时间阈值判断：若距离上次访问时间超过 {@link #SESSION_EXPIRY_THRESHOLD_MS}（25 分钟），
     * 则认为 Session 可能已在 Arthas Agent 侧超时（Arthas 默认 30 分钟），返回 {@code false}；
     * 否则认为 Session 仍有效，返回 {@code true}。
     * <p>
     * 注意：此方法仅通过本地时间戳做快速预判断，不发起网络请求验证 Session 在 Arthas Agent 侧的真实状态。
     * 若需要更精确的验证，可扩展此方法向 Arthas Agent 发送轻量级探活请求。
     *
     * @param binding 待检查的 Session 绑定对象
     * @return 若 Session 有效则返回 {@code true}，否则返回 {@code false}
     */
    private boolean isSessionValid(CommandSessionBinding binding) {
        // 计算距离上次访问的空闲时长
        long timeSinceLastAccess = System.currentTimeMillis() - binding.getLastAccessTime();

        if (timeSinceLastAccess > SESSION_EXPIRY_THRESHOLD_MS) {
            // 超过阈值，记录调试日志并返回失效
            logger.debug("Session possibly expired (inactive for {} ms): MCP={}, Arthas={}",
                       timeSinceLastAccess, binding.getMcpSessionId(), binding.getArthasSessionId());
            return false;
        }

        return true;
    }

    /**
     * 关闭指定 MCP Session 对应的 Arthas Command Session，并从映射表中移除绑定关系。
     * <p>
     * 若映射表中不存在指定 MCP Session 的绑定，则此方法为空操作（no-op）。
     * 关闭操作会通知 Arthas Agent 释放 Session 占用的服务端资源（线程、连接等）。
     *
     * @param mcpSessionId 要关闭的 MCP Session ID
     */
    public void closeCommandSession(String mcpSessionId) {
        // 从映射表中原子性地移除绑定关系，同时获取被移除的绑定对象
        CommandSessionBinding binding = sessionBindings.remove(mcpSessionId);
        if (binding != null) {
            // 通知 Arthas Agent 关闭对应的 Session，释放服务端资源
            commandExecutor.closeSession(binding.getArthasSessionId());
            logger.debug("Closed command session: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());
        }
    }

    /**
     * 关闭当前管理器中所有已注册的 Arthas Command Session，并清空映射表。
     * <p>
     * 通常在 MCP Server 关闭时调用，确保所有 Arthas Agent 侧的 Session 资源被正确释放，
     * 避免 Agent 侧出现僵尸 Session。
     * <p>
     * 内部通过遍历映射表中所有 MCP Session ID，依次调用 {@link #closeCommandSession} 完成关闭。
     */
    public void closeAllSessions() {
        // 遍历所有已注册的 MCP Session ID，逐一关闭对应的 Arthas Session
        sessionBindings.keySet().forEach(this::closeCommandSession);
    }
}

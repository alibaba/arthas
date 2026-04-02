// Arthas MCP工具抽象基类包
package com.taobao.arthas.core.mcp.tool.function;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.core.mcp.util.McpAuthExtractor;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

// 导入MCP工具工具类的静态方法
import static com.taobao.arthas.core.mcp.tool.util.McpToolUtils.*;
// 导入可流式处理工具工具类的静态方法
import static com.taobao.arthas.core.mcp.tool.function.StreamableToolUtils.*;

/**
 * Arthas工具的抽象基类
 * 提供所有Arthas工具类共有的基础功能，包括同步和异步命令执行、命令构建等
 *
 * @author Arthas Team
 */
public abstract class AbstractArthasTool {

    // 日志记录器，子类可以直接使用
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    // 默认超时时间（秒），从毫秒转换而来
    public static final int DEFAULT_TIMEOUT_SECONDS = (int) (StreamableToolUtils.DEFAULT_TIMEOUT_MS / 1000);

    // 异步启动重试间隔时间（毫秒），默认100毫秒
    private static final long DEFAULT_ASYNC_START_RETRY_INTERVAL_MS = 100L;
    // 异步启动最大等待时间（毫秒），默认3秒
    private static final long DEFAULT_ASYNC_START_MAX_WAIT_MS = 3000L;
    
    /**
     * 工具执行上下文，包含所有必要的上下文信息
     * 封装了工具执行过程中需要的各种上下文对象，包括命令上下文、传输上下文、认证信息等
     */
    protected static class ToolExecutionContext {
        // Arthas命令上下文，用于执行命令
        private final ArthasCommandContext commandContext;
        // MCP传输上下文，包含传输层相关信息
        private final McpTransportContext mcpTransportContext;
        // 认证主题对象，用于安全验证
        private final Object authSubject;
        // 用户ID，用于标识当前用户
        private final String userId;
        // Netty服务器交换对象，用于处理HTTP请求响应
        private final McpNettyServerExchange exchange;
        // 进度令牌，用于跟踪长时间运行的任务进度
        private final String progressToken;
        // 是否为可流式处理的工具
        private final boolean isStreamable;

        /**
         * 构造工具执行上下文
         *
         * @param toolContext 工具上下文对象
         * @param isStreamable 是否为可流式处理的工具
         */
        public ToolExecutionContext(ToolContext toolContext, boolean isStreamable) {
            // 从工具上下文中提取Arthas命令上下文
            this.commandContext = (ArthasCommandContext) toolContext.getContext().get(TOOL_CONTEXT_COMMAND_CONTEXT_KEY);
            this.isStreamable = isStreamable;

            // 尝试获取 Exchange (在 Stateless 模式下为 null)
            this.exchange = (McpNettyServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY);

            // 尝试获取 Progress Token
            Object progressTokenObj = toolContext.getContext().get(PROGRESS_TOKEN);
            this.progressToken = progressTokenObj != null ? String.valueOf(progressTokenObj) : null;

            // 尝试获取 Transport Context (在 Stateless 模式下可能为 null)
            this.mcpTransportContext = (McpTransportContext) toolContext.getContext().get(MCP_TRANSPORT_CONTEXT);

            // 从 Transport Context 中提取认证信息
            if (this.mcpTransportContext != null) {
                this.authSubject = mcpTransportContext.get(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY);
                this.userId = (String) mcpTransportContext.get(McpAuthExtractor.MCP_USER_ID_KEY);
            } else {
                this.authSubject = null;
                this.userId = null;
            }
        }

        /**
         * 获取Arthas命令上下文
         *
         * @return Arthas命令上下文对象
         */
        public ArthasCommandContext getCommandContext() {
            return commandContext;
        }

        /**
         * 获取MCP传输上下文
         *
         * @return MCP传输上下文对象
         */
        public McpTransportContext getMcpTransportContext() {
            return mcpTransportContext;
        }

        /**
         * 获取认证主题对象
         *
         * @return 认证主题对象
         */
        public Object getAuthSubject() {
            return authSubject;
        }

        /**
         * 获取用户 ID
         *
         * @return 用户 ID，如果未设置则返回 null
         */
        public String getUserId() {
            return userId;
        }

        /**
         * 获取Netty服务器交换对象
         *
         * @return Netty服务器交换对象
         */
        public McpNettyServerExchange getExchange() {
            return exchange;
        }

        /**
         * 获取进度令牌
         *
         * @return 进度令牌字符串
         */
        public String getProgressToken() {
            return progressToken;
        }

        /**
         * 判断是否为可流式处理的工具
         *
         * @return true表示可流式处理，false表示不可流式处理
         */
        public boolean isStreamable() {
            return isStreamable;
        }
    }

    /**
     * 同步执行Arthas命令
     * 该方法会阻塞直到命令执行完成并返回结果
     *
     * @param toolContext 工具上下文对象
     * @param commandStr 要执行的命令字符串
     * @return 命令执行结果的JSON字符串
     */
    protected String executeSync(ToolContext toolContext, String commandStr) {
        try {
            // 创建工具执行上下文，非流式模式
            ToolExecutionContext execContext = new ToolExecutionContext(toolContext, false);
            // 使用带 userId 参数的 executeSync 方法执行命令
            Object result = execContext.getCommandContext().executeSync(
                    commandStr,
                    execContext.getAuthSubject(),  // 传入认证主题
                    execContext.getUserId()        // 传入用户ID
            );
            // 将结果转换为JSON格式返回
            return JsonParser.toJson(result);
        } catch (Exception e) {
            // 记录错误日志
            logger.error("Error executing sync command: {}", commandStr, e);
            // 返回错误响应的JSON字符串
            return JsonParser.toJson(createErrorResponse("Error executing command: " + e.getMessage()));
        }
    }

    /**
     * 执行可流式处理的Arthas命令
     * 该方法支持异步执行命令并收集多个结果，适用于会产生多次输出的命令
     *
     * @param toolContext 工具上下文对象
     * @param commandStr 要执行的命令字符串
     * @param expectedResultCount 期望的结果数量
     * @param pollIntervalMs 轮询间隔时间（毫秒）
     * @param timeoutMs 超时时间（毫秒）
     * @param successMessage 成功时的消息
     * @return 命令执行结果的JSON字符串
     */
    protected String executeStreamable(ToolContext toolContext, String commandStr,
                                     Integer expectedResultCount, Integer pollIntervalMs,
                                     Integer timeoutMs,
                                     String successMessage) {
        ToolExecutionContext execContext = null;
        try {
            // 创建工具执行上下文，流式模式
            execContext = new ToolExecutionContext(toolContext, true);

            logger.info("Starting streamable execution: {}", commandStr);

            // 在异步执行前设置userId到session，用于统计上报
            if (execContext.getUserId() != null) {
                execContext.getCommandContext().setSessionUserId(execContext.getUserId());
            }

            // 带重试机制的异步执行
            Map<String, Object> asyncResult = executeAsyncWithRetry(execContext, commandStr, timeoutMs);
            // 检查异步执行是否成功启动
            if (!isAsyncExecutionStarted(asyncResult)) {
                String errorMessage = asyncResult != null ? String.valueOf(asyncResult.get("error")) : "unknown error";
                return JsonParser.toJson(createErrorResponse("Failed to start command: " + errorMessage));
            }
            logger.debug("Async execution started: {}", asyncResult);

            // 执行并收集结果
            Map<String, Object> results = executeAndCollectResults(
                execContext.getExchange(),
                execContext.getCommandContext(),
                expectedResultCount,
                pollIntervalMs,
                timeoutMs,
                execContext.getProgressToken()
            );

            // 处理收集到的结果
            if (results != null) {
                // 使用自定义成功消息或默认消息
                String message = successMessage != null ? successMessage : "Command execution completed successfully";

                // 检查是否超时
                if (Boolean.TRUE.equals(results.get("timedOut"))) {
                    Integer count = (Integer) results.get("resultCount");
                    if (count != null && count > 0) {
                        // 超时但捕获到了部分结果
                        message = "Command execution ended (Timed out). Captured " + count + " results.";
                    } else {
                        // 超时且没有捕获到结果
                        message = "Command execution ended (Timed out). No results captured within the time limit.";
                    }
                }

                // 返回成功响应
                return JsonParser.toJson(createCompletedResponse(message, results));
            } else {
                // 返回失败响应
                return JsonParser.toJson(createErrorResponse("Command execution failed due to timeout or error limits exceeded"));
            }

        } catch (Exception e) {
            // 捕获并记录异常
            logger.error("Error executing streamable command: {}", commandStr, e);
            return JsonParser.toJson(createErrorResponse("Error executing command: " + e.getMessage()));
        } finally {
            // 确保前台任务被及时释放，避免占用 session 影响后续 streamable 工具执行
            if (execContext != null) {
                try {
                    execContext.getCommandContext().interruptJob();
                } catch (Exception ignored) {
                    // 忽略中断异常
                }
            }
        }
    }

    /**
     * 判断异步执行是否成功启动
     *
     * @param asyncResult 异步执行结果映射
     * @return true表示成功启动，false表示启动失败
     */
    private static boolean isAsyncExecutionStarted(Map<String, Object> asyncResult) {
        if (asyncResult == null) {
            return false;
        }
        Object success = asyncResult.get("success");
        return Boolean.TRUE.equals(success);
    }

    /**
     * 判断异步启动错误是否可重试
     * 当出现"另一个任务正在运行"或"另一个命令正在执行"的错误时，可以重试
     *
     * @param asyncResult 异步执行结果映射
     * @return true表示可重试，false表示不可重试
     */
    private static boolean isRetryableAsyncStartError(Map<String, Object> asyncResult) {
        if (asyncResult == null) {
            return false;
        }
        Object success = asyncResult.get("success");
        if (Boolean.TRUE.equals(success)) {
            return false;
        }
        Object error = asyncResult.get("error");
        if (error == null) {
            return false;
        }
        String message = String.valueOf(error);
        // 检查是否为可重试的错误类型
        return message.contains("Another job is running") || message.contains("Another command is executing");
    }

    /**
     * 带重试机制的异步执行方法
     * 当异步执行启动失败时，如果错误是可重试的，会进行重试
     *
     * @param execContext 工具执行上下文
     * @param commandStr 要执行的命令字符串
     * @param timeoutMs 超时时间（毫秒）
     * @return 异步执行结果映射
     */
    private static Map<String, Object> executeAsyncWithRetry(ToolExecutionContext execContext, String commandStr, Integer timeoutMs) {
        // 计算最大等待时间，取默认值和用户指定值的较小者
        long maxWaitMs = DEFAULT_ASYNC_START_MAX_WAIT_MS;
        if (timeoutMs != null && timeoutMs > 0) {
            maxWaitMs = Math.min(maxWaitMs, timeoutMs);
        }

        // 计算截止时间
        long deadline = System.currentTimeMillis() + maxWaitMs;
        Map<String, Object> asyncResult = null;

        // 在截止时间前循环尝试执行
        while (System.currentTimeMillis() < deadline) {
            // 尝试异步执行命令
            asyncResult = execContext.getCommandContext().executeAsync(commandStr);
            if (isAsyncExecutionStarted(asyncResult)) {
                // 成功启动，返回结果
                return asyncResult;
            }

            // 检查是否为可重试的错误
            if (isRetryableAsyncStartError(asyncResult)) {
                try {
                    // 中断当前任务
                    execContext.getCommandContext().interruptJob();
                } catch (Exception ignored) {
                    // 忽略中断异常
                }
                try {
                    // 等待一段时间后重试
                    Thread.sleep(DEFAULT_ASYNC_START_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    // 线程被中断，恢复中断状态并返回当前结果
                    Thread.currentThread().interrupt();
                    return asyncResult;
                }
                continue;
            }

            // 不可重试的错误，直接返回
            return asyncResult;
        }

        // 达到截止时间，返回最后一次执行结果
        return asyncResult;
    }

    /**
     * 构建命令字符串构建器
     *
     * @param baseCommand 基础命令字符串
     * @return 命令字符串构建器
     */
    protected StringBuilder buildCommand(String baseCommand) {
        return new StringBuilder(baseCommand);
    }

    /**
     * 添加带标志的参数到命令中
     *
     * @param cmd 命令字符串构建器
     * @param flag 参数标志（如 "-n", "-c" 等）
     * @param value 参数值
     */
    protected void addParameter(StringBuilder cmd, String flag, String value) {
        if (value != null && !value.trim().isEmpty()) {
            cmd.append(" ").append(flag).append(" ").append(value.trim());
        }
    }

    /**
     * 添加参数到命令中（带引号，防止命令注入）
     * 会自动转义参数中的单引号，防止命令注入攻击
     *
     * @param cmd 命令字符串构建器
     * @param value 参数值
     */
    protected void addParameter(StringBuilder cmd, String value) {
        if (value != null && !value.trim().isEmpty()) {
            // 安全地引用值以防止命令注入，转义单引号
            cmd.append(" '").append(value.trim().replace("'", "'\\''")).append("'");
        }
    }

    /**
     * 添加标志到命令中
     * 只有当条件为true时才会添加标志
     *
     * @param cmd 命令字符串构建器
     * @param flag 标志字符串
     * @param condition 是否添加标志的条件
     */
    protected void addFlag(StringBuilder cmd, String flag, Boolean condition) {
        if (Boolean.TRUE.equals(condition)) {
            cmd.append(" ").append(flag);
        }
    }

    /**
     * 添加引用参数到命令中（用于包含空格的参数）
     * 简单地用单引号包裹参数值
     *
     * @param cmd 命令字符串构建器
     * @param value 参数值
     */
    protected void addQuotedParameter(StringBuilder cmd, String value) {
        if (value != null && !value.trim().isEmpty()) {
            cmd.append(" '").append(value.trim()).append("'");
        }
    }

    /**
     * 获取整数值，如果为null或小于等于0则返回默认值
     *
     * @param value 输入值
     * @param defaultValue 默认值
     * @return 有效值或默认值
     */
    protected int getDefaultValue(Integer value, int defaultValue) {
        return (value != null && value > 0) ? value : defaultValue;
    }

    /**
     * 获取字符串值，如果为null或空字符串则返回默认值
     *
     * @param value 输入值
     * @param defaultValue 默认值
     * @return 有效值或默认值
     */
    protected String getDefaultValue(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }
}

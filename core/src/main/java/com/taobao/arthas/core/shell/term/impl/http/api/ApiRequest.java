package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * HTTP API 请求对象
 * <p>
 * 该类封装了客户端向 Arthas HTTP API 发送请求时所需的所有参数。
 * 包含操作类型、命令内容、会话信息、超时设置等关键信息。
 * </p>
 *
 * @author gongdewei 2020-03-19
 */
public class ApiRequest {
    /**
     * 操作类型
     * <p>
     * 对应 {@link ApiAction} 枚举值，指定要执行的具体操作。
     * 例如：EXEC（同步执行）、ASYNC_EXEC（异步执行）、INIT_SESSION（创建会话）等。
     * </p>
     */
    private String action;

    /**
     * 要执行的 Arthas 命令
     * <p>
     * 具体的 Arthas 命令字符串，例如 "watch demo.MathGame primeFactors returnObj"。
     * 仅在 action 为 EXEC 或 ASYNC_EXEC 时需要设置此字段。
     * </p>
     */
    private String command;

    /**
     * 请求标识符
     * <p>
     * 用于唯一标识一次 API 请求，支持请求追踪和结果匹配。
     * 客户端应确保每个请求的 requestId 唯一性。
     * </p>
     */
    private String requestId;

    /**
     * 会话标识符
     * <p>
     * 指定要操作的目标会话 ID。
     * 对于 INIT_SESSION 操作，此字段可为空；对于其他操作，通常需要指定 sessionId。
     * </p>
     */
    private String sessionId;

    /**
     * 消费者标识符
     * <p>
     * 标识请求发起的客户端或消费者。
     * 用于区分不同的客户端连接，支持多客户端共享同一会话。
     * </p>
     */
    private String consumerId;

    /**
     * 命令执行超时时间（单位：秒）
     * <p>
     * 设置命令执行的最大等待时间，防止长时间运行的命令占用资源。
     * 如果设置为 null 或 0，表示不限制执行时间。
     * </p>
     */
    private Integer execTimeout;

    /**
     * 用户标识符
     * <p>
     * 用于标识发起请求的用户，支持用户级别的权限控制和操作审计。
     * </p>
     */
    private String userId;

    /**
     * 生成请求对象的字符串表示
     * <p>
     * 将请求对象的所有字段格式化为易读的字符串，便于日志记录和调试。
     * </p>
     *
     * @return 包含所有字段值的字符串表示
     */
    @Override
    public String toString() {
        return "ApiRequest{" +
                "action='" + action + '\'' +
                ", command='" + command + '\'' +
                ", requestId='" + requestId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", consumerId='" + consumerId + '\'' +
                ", execTimeout=" + execTimeout +
                ", userId='" + userId + '\'' +
                '}';
    }

    /**
     * 获取操作类型
     *
     * @return 操作类型字符串
     */
    public String getAction() {
        return action;
    }

    /**
     * 设置操作类型
     *
     * @param action 操作类型字符串
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 获取要执行的命令
     *
     * @return Arthas 命令字符串
     */
    public String getCommand() {
        return command;
    }

    /**
     * 设置要执行的命令
     *
     * @param command Arthas 命令字符串
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * 获取请求标识符
     *
     * @return 请求 ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 设置请求标识符
     *
     * @param requestId 请求 ID
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * 获取会话标识符
     *
     * @return 会话 ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话标识符
     *
     * @param sessionId 会话 ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取消费者标识符
     *
     * @return 消费者 ID
     */
    public String getConsumerId() {
        return consumerId;
    }

    /**
     * 设置消费者标识符
     *
     * @param consumerId 消费者 ID
     */
    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    /**
     * 获取命令执行超时时间
     *
     * @return 超时时间（秒）
     */
    public Integer getExecTimeout() {
        return execTimeout;
    }

    /**
     * 设置命令执行超时时间
     *
     * @param execTimeout 超时时间（秒）
     */
    public void setExecTimeout(Integer execTimeout) {
        this.execTimeout = execTimeout;
    }

    /**
     * 获取用户标识符
     *
     * @return 用户 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户标识符
     *
     * @param userId 用户 ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
}

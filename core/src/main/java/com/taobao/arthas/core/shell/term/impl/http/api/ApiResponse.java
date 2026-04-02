package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * HTTP API 响应对象
 * <p>
 * 该类封装了 Arthas HTTP API 的响应数据，采用泛型设计以支持不同类型的响应体。
 * 所有 Setter 方法都返回当前对象实例，支持链式调用，便于构建响应对象。
 * </p>
 * <p>
 * 典型的使用场景包括：
 * <ul>
 *   <li>命令执行结果：body 中包含命令的输出内容</li>
 *   <li>会话信息：body 中包含会话的详细信息</li>
 *   <li>错误响应：state 为错误状态，message 中包含错误描述</li>
 * </ul>
 * </p>
 *
 * @param <T> 响应体的数据类型，可以是字符串、对象或其他自定义类型
 * @author gongdewei 2020-03-19
 */
public class ApiResponse<T> {
    /**
     * 请求标识符
     * <p>
     * 对应请求中的 requestId，用于将响应与原始请求进行匹配。
     * 客户端可以通过此字段确认响应是对应哪个请求的。
     * </p>
     */
    private String requestId;

    /**
     * 响应状态
     * <p>
     * 表示 API 调用的执行状态，对应 {@link ApiState} 枚举值。
     * 可能的状态包括：
     * <ul>
     *   <li>WAITING：等待执行</li>
     *   <li>RUNNING：正在执行</li>
     *   <li>SUCCEEDED：执行成功</li>
     *   <li>FAILED：执行失败</li>
     *   <li>TERMINATED：已终止</li>
     *   <li>TIMEOUT：执行超时</li>
     * </ul>
     * </p>
     */
    private ApiState state;

    /**
     * 响应消息
     * <p>
     * 包含响应的详细信息或错误描述。
     * 当 state 为 FAILED 或 TIMEOUT 时，此字段通常包含错误原因。
     * </p>
     */
    private String message;

    /**
     * 会话标识符
     * <p>
     * 当前响应对应的会话 ID，可用于后续的会话操作。
     * 对于 INIT_SESSION 操作，成功后会返回新创建的 sessionId。
     * </p>
     */
    private String sessionId;

    /**
     * 消费者标识符
     * <p>
     * 标识处理响应的消费者，支持多客户端场景下的响应路由。
     * </p>
     */
    private String consumerId;

    /**
     * 任务标识符
     * <p>
     * 对于异步执行的命令，返回对应的任务 ID。
     * 客户端可以使用此 ID 来查询任务状态或中断任务。
     * </p>
     */
    private String jobId;

    /**
     * 响应体
     * <p>
     * 包含实际的响应数据，类型由泛型参数 T 决定。
     * 根据不同的操作类型，body 可能包含：
     * <ul>
     *   <li>命令执行结果的文本输出</li>
     *   <li>会话信息的 JSON 对象</li>
     *   <li>其他结构化数据</li>
     * </ul>
     * </p>
     */
    private T body;

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
     * <p>
     * 支持链式调用，方便构建响应对象。
     * </p>
     *
     * @param requestId 请求 ID
     * @return 当前响应对象实例
     */
    public ApiResponse<T> setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * 获取响应状态
     *
     * @return API 状态枚举值
     */
    public ApiState getState() {
        return state;
    }

    /**
     * 设置响应状态
     * <p>
     * 支持链式调用，方便构建响应对象。
     * </p>
     *
     * @param state API 状态枚举值
     * @return 当前响应对象实例
     */
    public ApiResponse<T> setState(ApiState state) {
        this.state = state;
        return this;
    }

    /**
     * 获取响应消息
     *
     * @return 响应消息字符串
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置响应消息
     * <p>
     * 支持链式调用，方便构建响应对象。
     * </p>
     *
     * @param message 响应消息字符串
     * @return 当前响应对象实例
     */
    public ApiResponse<T> setMessage(String message) {
        this.message = message;
        return this;
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
     * <p>
     * 支持链式调用，方便构建响应对象。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 当前响应对象实例
     */
    public ApiResponse<T> setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
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
     * <p>
     * 支持链式调用，方便构建响应对象。
     * </p>
     *
     * @param consumerId 消费者 ID
     * @return 当前响应对象实例
     */
    public ApiResponse<T> setConsumerId(String consumerId) {
        this.consumerId = consumerId;
        return this;
    }

    /**
     * 获取任务标识符
     *
     * @return 任务 ID
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * 设置任务标识符
     * <p>
     * 支持链式调用，方便构建响应对象。
     * </p>
     *
     * @param jobId 任务 ID
     * @return 当前响应对象实例
     */
    public ApiResponse<T> setJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    /**
     * 获取响应体
     *
     * @return 响应体对象
     */
    public T getBody() {
        return body;
    }

    /**
     * 设置响应体
     * <p>
     * 支持链式调用，方便构建响应对象。
     * </p>
     *
     * @param body 响应体对象
     * @return 当前响应对象实例
     */
    public ApiResponse<T> setBody(T body) {
        this.body = body;
        return this;
    }

}

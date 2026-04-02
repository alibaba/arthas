package com.taobao.arthas.mcp.server;

import java.util.Map;

/**
 * 命令执行器接口
 * <p>
 * 定义了命令执行的核心方法，包括同步执行、异步执行、结果拉取、任务中断、会话管理等功能。
 * </p>
 *
 * @author Yeaury 2025/5/26
 */
public interface CommandExecutor {

    /**
     * 同步执行命令（使用默认超时时间30秒）
     *
     * @param commandLine 命令行字符串
     * @param timeout 超时时间（毫秒）
     * @return 执行结果的Map集合
     */
    default Map<String, Object> executeSync(String commandLine, long timeout) {
        return executeSync(commandLine, timeout, null, null, null);
    }

    /**
     * 同步执行命令（使用默认超时时间30秒，指定认证主体）
     *
     * @param commandLine 命令行字符串
     * @param authSubject 认证主体对象
     * @return 执行结果的Map集合
     */
    default Map<String, Object> executeSync(String commandLine, Object authSubject) {
        return executeSync(commandLine, 30000L, null, authSubject, null);
    }

    /**
     * 同步执行命令，支持指定 userId
     * <p>
     * 使用默认超时时间30秒，并支持用户ID统计上报
     * </p>
     *
     * @param commandLine 命令行字符串
     * @param authSubject 认证主体对象
     * @param userId 用户 ID，用于统计上报
     * @return 执行结果的Map集合
     */
    default Map<String, Object> executeSync(String commandLine, Object authSubject, String userId) {
        return executeSync(commandLine, 30000L, null, authSubject, userId);
    }

    /**
     * 同步执行命令
     * <p>
     * 完整版本的同步执行方法，支持所有参数配置
     * </p>
     *
     * @param commandLine 命令行字符串
     * @param timeout 超时时间（毫秒）
     * @param sessionId session ID，如果为null则创建临时session
     * @param authSubject 认证主体对象，如果不为null则应用到session
     * @param userId 用户 ID，用于统计上报
     * @return 执行结果的Map集合
     */
    Map<String, Object> executeSync(String commandLine, long timeout, String sessionId, Object authSubject, String userId);

    /**
     * 异步执行命令
     * <p>
     * 在指定session中异步执行命令，立即返回
     * </p>
     *
     * @param commandLine 命令行字符串
     * @param sessionId session ID
     * @return 执行结果的Map集合（包含任务ID等信息）
     */
    Map<String, Object> executeAsync(String commandLine, String sessionId);

    /**
     * 拉取异步任务执行结果
     * <p>
     * 从指定session中拉取异步任务的执行结果
     * </p>
     *
     * @param sessionId session ID
     * @param consumerId 消费者ID
     * @return 执行结果的Map集合
     */
    Map<String, Object> pullResults(String sessionId, String consumerId);

    /**
     * 中断正在执行的任务
     * <p>
     * 中断指定session中正在执行的任务
     * </p>
     *
     * @param sessionId session ID
     * @return 操作结果的Map集合
     */
    Map<String, Object> interruptJob(String sessionId);

    /**
     * 创建新的会话
     * <p>
     * 创建一个新的命令执行会话，用于隔离命令执行环境
     * </p>
     *
     * @return 包含session ID的Map集合
     */
    Map<String, Object> createSession();

    /**
     * 关闭会话
     * <p>
     * 关闭指定session并释放相关资源
     * </p>
     *
     * @param sessionId 要关闭的session ID
     * @return 操作结果的Map集合
     */
    Map<String, Object> closeSession(String sessionId);

    /**
     * 设置session的认证主体
     * <p>
     * 为指定session设置认证信息，用于权限控制
     * </p>
     *
     * @param sessionId session ID
     * @param authSubject 认证主体对象
     */
    void setSessionAuth(String sessionId, Object authSubject);

    /**
     * 设置 session 的 userId
     * <p>
     * 为指定session设置用户ID，用于统计和审计
     * </p>
     *
     * @param sessionId session ID
     * @param userId 用户 ID
     */
    void setSessionUserId(String sessionId, String userId);
}

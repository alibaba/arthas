package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * HTTP API 响应状态枚举
 * <p>
 * 定义了 Arthas HTTP API 处理请求后可能返回的各种状态
 * </p>
 *
 * @author gongdewei 2020-03-19
 */
public enum ApiState {
    /**
     * 已调度异步执行任务
     * <p>
     * 表示请求已被接受，异步任务已被调度执行，但尚未完成
     * </p>
     */
    SCHEDULED,

//    RUNNING,

    /**
     * 请求处理成功
     * <p>
     * 表示请求已被成功处理，操作已完成
     * </p>
     */
    SUCCEEDED,

    /**
     * 请求处理被中断
     * <p>
     * 表示请求处理过程中被中断，例如超时或用户主动中断
     * </p>
     */
    INTERRUPTED,

    /**
     * 请求处理失败
     * <p>
     * 表示请求处理过程中发生错误，处理失败
     * </p>
     */
    FAILED,

    /**
     * 请求被拒绝
     * <p>
     * 表示请求被拒绝处理，可能是因为权限不足、参数错误等原因
     * </p>
     */
    REFUSED
}

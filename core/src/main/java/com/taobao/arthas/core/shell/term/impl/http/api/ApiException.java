package com.taobao.arthas.core.shell.term.impl.http.api;

/**
 * HTTP API 异常类
 * <p>
 * 用于表示在处理 HTTP API 请求过程中发生的异常情况。
 * 当 API 调用失败、参数错误、执行异常等情况发生时，将抛出此异常。
 * </p>
 *
 * @author gongdewei 2020-03-19
 */
public class ApiException extends Exception {

    /**
     * 构造一个包含详细错误信息的 API 异常
     *
     * @param message 异常的详细描述信息，用于说明错误原因
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * 构造一个包含错误信息和原因的 API 异常
     * <p>
     * 用于在捕获底层异常后包装为 API 异常，保留完整的异常链。
     * </p>
     *
     * @param message 异常的详细描述信息，用于说明错误原因
     * @param cause 导致此异常的底层异常对象，保存异常链以便问题追踪
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

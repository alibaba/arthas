package com.taobao.arthas.core.command.express;

/**
 * 表达式异常类
 * 用于封装在表达式执行过程中发生的异常
 * Created by vlinux on 15/5/20.
 */
public class ExpressException extends Exception {

    // 原始表达式字符串，记录出问题的表达式内容
    private final String express;

    /**
     * 构造表达式异常对象
     *
     * @param express 原始表达式字符串，记录导致异常的表达式内容
     * @param cause   异常原因，底层的异常对象
     */
    public ExpressException(String express, Throwable cause) {
        // 调用父类构造函数，传入底层异常原因
        super(cause);
        // 保存导致异常的表达式
        this.express = express;
    }

    /**
     * 获取导致异常的表达式字符串
     *
     * @return 返回出问题的表达式内容
     */
    public String getExpress() {
        // 返回保存的表达式字符串
        return express;
    }
}

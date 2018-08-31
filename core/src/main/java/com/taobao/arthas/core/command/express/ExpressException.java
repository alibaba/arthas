package com.taobao.arthas.core.command.express;

/**
 * 表达式异常
 * Created by vlinux on 15/5/20.
 */
public class ExpressException extends Exception {

    private final String express;

    /**
     * 表达式异常
     *
     * @param express 原始表达式
     * @param cause   异常原因
     */
    public ExpressException(String express, Throwable cause) {
        super(cause);
        this.express = express;
    }

    /**
     * 获取表达式
     *
     * @return 返回出问题的表达式
     */
    public String getExpress() {
        return express;
    }
}

package com.taobao.arthas.core.command.model;

/**
 * TraceCommand的异常抛出信息节点
 * 用于在方法调用链路追踪中记录异常抛出的详细信息
 *
 * @author gongdewei 2020/7/21
 */
public class ThrowNode extends TraceNode {
    // 异常类型，包含完整的类名（如：java.lang.NullPointerException）
    private String exception;
    // 异常消息，描述异常的具体信息
    private String message;
    // 异常抛出位置的行号，用于定位问题代码
    private int lineNumber;

    /**
     * 默认构造函数
     * 自动设置节点类型为"throw"，表示这是一个异常抛出节点
     */
    public ThrowNode() {
        super("throw");
    }

    /**
     * 获取异常类型
     * @return 异常的完整类名（如：java.lang.NullPointerException）
     */
    public String getException() {
        return exception;
    }

    /**
     * 设置异常类型
     * @param exception 异常的完整类名
     */
    public void setException(String exception) {
        this.exception = exception;
    }

    /**
     * 获取异常抛出的行号
     * @return 代码行号，用于定位异常抛出的具体位置
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * 设置异常抛出的行号
     * @param lineNumber 代码行号
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * 获取异常消息
     * @return 异常的详细描述信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置异常消息
     * @param message 异常的详细描述信息
     */
    public void setMessage(String message) {
        this.message = message;
    }

}

package com.taobao.arthas.common;

/**
 * 反射操作异常类
 * <p>
 * 该类用于封装反射操作过程中抛出的异常。
 * 继承自RuntimeException，表示这是一个非受检异常。
 * 当使用反射API进行类加载、方法调用、字段访问等操作失败时抛出此异常。
 * </p>
 */
public class ReflectException extends RuntimeException {

    /**
     * 序列化版本标识符
     * 用于控制类的序列化版本兼容性
     */
    private static final long serialVersionUID = 1L;

    /**
     * 导致此异常的原始异常对象
     * 保存了底层反射操作抛出的原始异常，便于问题排查
     */
    private Throwable cause;

    /**
     * 构造函数
     * <p>
     * 根据传入的原始异常创建反射异常对象。
     * 异常消息格式为：原始异常类名-->原始异常消息
     * </p>
     *
     * @param cause 导致此反射异常的原始异常对象
     */
    public ReflectException(Throwable cause) {
        // 如果原始异常不为空，则构造异常消息，格式为"异常类名-->异常消息"
        super(cause != null ? cause.getClass().getName() + "-->" + cause.getMessage() : "");
        this.cause = cause;
    }

    /**
     * 获取导致此异常的原始异常对象
     *
     * @return 原始异常对象，可能为null
     */
    public Throwable getCause() {
        return this.cause;
    }
}
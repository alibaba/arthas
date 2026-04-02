package com.taobao.arthas.core.advisor;

/**
 * 方法增强的接入点枚举类
 * <p>
 * 定义了在方法的哪些位置可以进行增强（切面插入），包括：
 * <ul>
 * <li>方法执行前（ACCESS_BEFORE/AtEnter）</li>
 * <li>方法正常返回后（ACCESS_AFTER_RETUNING/AtExit）</li>
 * <li>方法抛出异常后（ACCESS_AFTER_THROWING/AtExceptionExit）</li>
 * </ul>
 * </p>
 * <p>
 * 使用位掩码（bit mask）方式存储，支持同时多个接入点的组合操作
 * </p>
 *
 * @author vlinux
 */
public enum AccessPoint {
    /**
     * 方法执行前的接入点
     * 在方法体执行之前触发，对应值：1（二进制：001）
     */
    ACCESS_BEFORE(1, "AtEnter"),

    /**
     * 方法正常返回后的接入点
     * 在方法正常执行完毕并返回结果后触发（不包括抛出异常的情况），对应值：2（二进制：010）
     */
    ACCESS_AFTER_RETUNING(1 << 1, "AtExit"),

    /**
     * 方法抛出异常后的接入点
     * 在方法执行过程中抛出异常时触发，对应值：4（二进制：100）
     */
    ACCESS_AFTER_THROWING(1 << 2, "AtExceptionExit");

    /**
     * 接入点的数值标识
     * 使用位掩码方式存储，便于进行按位与、或等操作
     */
    private int value;

    /**
     * 接入点的键名
     * 用于标识接入点的名称，如 "AtEnter"、"AtExit"、"AtExceptionExit"
     */
    private String key;

    /**
     * 获取接入点的数值标识
     *
     * @return 接入点的数值（位掩码）
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取接入点的键名
     *
     * @return 接入点的名称字符串
     */
    public String getKey() {
        return key;
    }

    /**
     * 构造方法
     *
     * @param value 接入点的数值标识（位掩码）
     * @param key   接入点的键名
     */
    AccessPoint(int value, String key) {
        this.value = value;
        this.key = key;
    }
}
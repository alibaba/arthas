package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * 时间片段的值对象（VO）
 * 用于在TimeTunnel命令中记录方法调用的时间片段信息，包括调用时间、耗时、参数、返回值等
 *
 * @author gongdewei 2020/4/27
 */
public class TimeFragmentVO {
    /** 时间片段的索引编号，用于唯一标识一个记录的时间片段 */
    private Integer index;

    /** 时间戳，记录方法调用的具体时间 */
    private LocalDateTime timestamp;

    /** 方法执行的耗时，单位为毫秒 */
    private double cost;

    /** 是否为返回类型的时间片段，标识方法是否正常返回 */
    private boolean isReturn;

    /** 是否抛出异常，标识方法执行过程中是否抛出了异常 */
    private boolean isThrow;

    /** 调用对象的字符串表示，记录方法调用的目标对象 */
    private String object;

    /** 类的全限定名，记录方法所在的类 */
    private String className;

    /** 方法名，记录被调用的方法名称 */
    private String methodName;

    /** 方法参数数组，以ObjectVO数组的形式存储方法调用的入参 */
    private ObjectVO[] params;

    /** 返回值对象，以ObjectVO的形式存储方法的返回值 */
    private ObjectVO returnObj;

    /** 抛出的异常对象，以ObjectVO的形式存储方法抛出的异常信息 */
    private ObjectVO throwExp;

    /**
     * 默认构造函数
     * 创建一个空的TimeFragmentVO实例
     */
    public TimeFragmentVO() {
    }

    /**
     * 获取时间片段的索引编号
     *
     * @return 时间片段的索引值
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * 设置时间片段的索引编号
     * 支持链式调用
     *
     * @param index 时间片段的索引值
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * 获取时间戳
     *
     * @return 方法调用的时间戳
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 设置时间戳
     * 支持链式调用
     *
     * @param timestamp 方法调用的时间戳
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * 获取方法执行的耗时
     *
     * @return 方法执行的耗时（毫秒）
     */
    public double getCost() {
        return cost;
    }

    /**
     * 设置方法执行的耗时
     * 支持链式调用
     *
     * @param cost 方法执行的耗时（毫秒）
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setCost(double cost) {
        this.cost = cost;
        return this;
    }

    /**
     * 判断方法是否正常返回
     *
     * @return 如果方法正常返回返回true，否则返回false
     */
    public boolean isReturn() {
        return isReturn;
    }

    /**
     * 设置方法是否正常返回
     * 支持链式调用
     *
     * @param aReturn true表示方法正常返回，false表示未返回或抛出异常
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setReturn(boolean aReturn) {
        isReturn = aReturn;
        return this;
    }

    /**
     * 判断方法是否抛出异常
     *
     * @return 如果方法抛出异常返回true，否则返回false
     */
    public boolean isThrow() {
        return isThrow;
    }

    /**
     * 设置方法是否抛出异常
     * 支持链式调用
     *
     * @param aThrow true表示方法抛出异常，false表示未抛出异常
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setThrow(boolean aThrow) {
        isThrow = aThrow;
        return this;
    }

    /**
     * 获取调用对象的字符串表示
     *
     * @return 调用对象的字符串表示
     */
    public String getObject() {
        return object;
    }

    /**
     * 设置调用对象的字符串表示
     * 支持链式调用
     *
     * @param object 调用对象的字符串表示
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setObject(String object) {
        this.object = object;
        return this;
    }

    /**
     * 获取类的全限定名
     *
     * @return 类的全限定名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置类的全限定名
     * 支持链式调用
     *
     * @param className 类的全限定名
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setClassName(String className) {
        this.className = className;
        return this;
    }

    /**
     * 获取方法名
     *
     * @return 方法名
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * 设置方法名
     * 支持链式调用
     *
     * @param methodName 方法名
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * 获取方法参数数组
     *
     * @return 方法参数的ObjectVO数组
     */
    public ObjectVO[] getParams() {
        return params;
    }

    /**
     * 设置方法参数数组
     * 支持链式调用
     *
     * @param params 方法参数的ObjectVO数组
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setParams(ObjectVO[] params) {
        this.params = params;
        return this;
    }

    /**
     * 获取方法的返回值对象
     *
     * @return 方法返回值的ObjectVO对象
     */
    public ObjectVO getReturnObj() {
        return returnObj;
    }

    /**
     * 设置方法的返回值对象
     * 支持链式调用
     *
     * @param returnObj 方法返回值的ObjectVO对象
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setReturnObj(ObjectVO returnObj) {
        this.returnObj = returnObj;
        return this;
    }

    /**
     * 获取方法抛出的异常对象
     *
     * @return 方法抛出异常的ObjectVO对象
     */
    public ObjectVO getThrowExp() {
        return throwExp;
    }

    /**
     * 设置方法抛出的异常对象
     * 支持链式调用
     *
     * @param throwExp 方法抛出异常的ObjectVO对象
     * @return 当前TimeFragmentVO实例，支持链式调用
     */
    public TimeFragmentVO setThrowExp(ObjectVO throwExp) {
        this.throwExp = throwExp;
        return this;
    }
}

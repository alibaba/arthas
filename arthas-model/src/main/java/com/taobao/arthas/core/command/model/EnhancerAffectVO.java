package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * 类增强影响值对象 - 纯数据传输对象
 * <p>
 * 该类用于表示类增强操作的影响范围和结果，包含增强的类数量、方法数量、
 * 耗时、监听器ID等信息。这是一个纯粹的DTO（Data Transfer Object）类，
 * 用于在不同层之间传输数据。
 * </p>
 *
 * @author gongdewei 2020/6/22
 */
public class EnhancerAffectVO {

    /**
     * 增强操作耗时（毫秒）
     * <p>
     * 记录从开始到完成类增强所消耗的时间
     * </p>
     */
    private long cost;

    /**
     * 增强的方法数量
     * <p>
     * 被增强的方法总数
     * </p>
     */
    private int methodCount;

    /**
     * 增强的类数量
     * <p>
     * 被增强的类总数
     * </p>
     */
    private int classCount;

    /**
     * 监听器ID
     * <p>
     * 用于标识和管理增强操作的监听器
     * </p>
    */
    private long listenerId;

    /**
     * 异常对象
     * <p>
     * 如果增强过程中出现错误，记录异常信息；否则为null
     * </p>
     */
    private Throwable throwable;

    /**
     * 类转储文件列表
     * <p>
     * 增强后的类文件转储路径列表
     * </p>
     */
    private List<String> classDumpFiles;

    /**
     * 增强的方法列表
     * <p>
     * 所有被增强的方法的完整签名列表
     * </p>
     */
    private List<String> methods;

    /**
     * 超限消息
     * <p>
     * 当增强的数量超过限制时，记录警告或错误消息
     * </p>
     */
    private String overLimitMsg;

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的EnhancerAffectVO对象，所有字段使用默认值
     * </p>
     */
    public EnhancerAffectVO() {
    }

    /**
     * 构造函数 - 核心参数
     * <p>
     * 创建包含核心统计信息的EnhancerAffectVO对象
     * </p>
     *
     * @param cost 增强操作耗时（毫秒）
     * @param methodCount 增强的方法数量
     * @param classCount 增强的类数量
     * @param listenerId 监听器ID
     */
    public EnhancerAffectVO(long cost, int methodCount, int classCount, long listenerId) {
        this.cost = cost;
        this.methodCount = methodCount;
        this.classCount = classCount;
        this.listenerId = listenerId;
    }

    /**
     * 获取增强操作耗时
     *
     * @return 耗时（毫秒）
     */
    public long getCost() {
        return cost;
    }

    /**
     * 设置增强操作耗时
     *
     * @param cost 耗时（毫秒）
     */
    public void setCost(long cost) {
        this.cost = cost;
    }

    /**
     * 获取增强的类数量
     *
     * @return 类数量
     */
    public int getClassCount() {
        return classCount;
    }

    /**
     * 设置增强的类数量
     *
     * @param classCount 类数量
     */
    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    /**
     * 获取增强的方法数量
     *
     * @return 方法数量
     */
    public int getMethodCount() {
        return methodCount;
    }

    /**
     * 设置增强的方法数量
     *
     * @param methodCount 方法数量
     */
    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    /**
     * 获取监听器ID
     *
     * @return 监听器ID
     */
    public long getListenerId() {
        return listenerId;
    }

    /**
     * 设置监听器ID
     *
     * @param listenerId 监听器ID
     */
    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    /**
     * 获取异常对象
     *
     * @return 异常对象，如果没有异常则为null
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 设置异常对象
     *
     * @param throwable 异常对象
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * 获取类转储文件列表
     *
     * @return 类转储文件路径列表
     */
    public List<String> getClassDumpFiles() {
        return classDumpFiles;
    }

    /**
     * 设置类转储文件列表
     *
     * @param classDumpFiles 类转储文件路径列表
     */
    public void setClassDumpFiles(List<String> classDumpFiles) {
        this.classDumpFiles = classDumpFiles;
    }

    /**
     * 获取增强的方法列表
     *
     * @return 方法签名列表
     */
    public List<String> getMethods() {
        return methods;
    }

    /**
     * 设置增强的方法列表
     *
     * @param methods 方法签名列表
     */
    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    /**
     * 设置超限消息
     *
     * @param overLimitMsg 超限消息
     */
    public void setOverLimitMsg(String overLimitMsg) {
        this.overLimitMsg = overLimitMsg;
    }

    /**
     * 获取超限消息
     *
     * @return 超限消息字符串
     */
    public String getOverLimitMsg() {
        return overLimitMsg;
    }
}

package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * Watch命令结果模型类
 * 用于封装watch命令的执行结果，包含时间戳、消耗时间、观察值等信息
 * Watch命令用于方法执行观测，可以在方法调用前后输出相关信息
 *
 * @author gongdewei 2020/03/26
 */
public class WatchModel extends ResultModel {

    /**
     * 时间戳
     * 记录watch命令执行的时间点
     */
    private LocalDateTime ts;

    /**
     * 执行耗时（单位：毫秒）
     * 记录watch命令执行所消耗的时间
     */
    private double cost;

    /**
     * 观察到的对象值
     * 包含watch命令捕获的方法参数、返回值或异常信息
     */
    private ObjectVO value;

    /**
     * 大小限制
     * 用于限制输出结果的大小，防止数据量过大
     */
    private Integer sizeLimit;

    /**
     * 类名
     * 被观察方法所在的类的全限定名
     */
    private String className;

    /**
     * 方法名
     * 被观察的方法名称
     */
    private String methodName;

    /**
     * 访问点
     * 表示观察的位置（如方法调用前、返回后、抛出异常时等）
     */
    private String accessPoint;

    /**
     * 默认构造函数
     * 创建一个空的WatchModel对象
     */
    public WatchModel() {
    }

    /**
     * 获取结果模型的类型标识
     *
     * @return 类型标识字符串 "watch"
     */
    @Override
    public String getType() {
        return "watch";
    }

    /**
     * 获取时间戳
     *
     * @return watch命令执行的时间戳
     */
    public LocalDateTime getTs() {
        return ts;
    }

    /**
     * 设置时间戳
     *
     * @param ts watch命令执行的时间戳
     */
    public void setTs(LocalDateTime ts) {
        this.ts = ts;
    }

    /**
     * 获取执行耗时
     *
     * @return 执行所消耗的时间（毫秒）
     */
    public double getCost() {
        return cost;
    }

    /**
     * 获取观察到的对象值
     *
     * @return 包含观察数据的对象视图
     */
    public ObjectVO getValue() {
        return value;
    }

    /**
     * 设置执行耗时
     *
     * @param cost 执行所消耗的时间（毫秒）
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * 设置观察到的对象值
     *
     * @param value 包含观察数据的对象视图
     */
    public void setValue(ObjectVO value) {
        this.value = value;
    }

    /**
     * 设置大小限制
     * 用于控制输出结果的规模
     *
     * @param sizeLimit 大小限制值
     */
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    /**
     * 获取大小限制
     *
     * @return 当前设置的大小限制值
     */
    public Integer getSizeLimit() {
        return sizeLimit;
    }

    /**
     * 获取类名
     *
     * @return 被观察方法所在类的全限定名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置类名
     *
     * @param className 被观察方法所在类的全限定名
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取方法名
     *
     * @return 被观察的方法名称
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * 设置方法名
     *
     * @param methodName 被观察的方法名称
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * 获取访问点
     *
     * @return 观察位置描述（如方法调用前、返回后等）
     */
    public String getAccessPoint() {
        return accessPoint;
    }

    /**
     * 设置访问点
     *
     * @param accessPoint 观察位置描述
     */
    public void setAccessPoint(String accessPoint) {
        this.accessPoint = accessPoint;
    }
}

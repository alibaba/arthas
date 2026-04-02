package com.taobao.arthas.core.command.model;

import java.lang.Thread.State;

/**
 * 线程值对象，用于'dashboard'和'thread'命令
 * 封装线程的详细信息，用于展示线程状态和性能指标
 *
 * @author gongdewei 2020/4/22
 */
public class ThreadVO {
    // 线程唯一标识ID
    private long id;
    // 线程名称
    private String name;
    // 线程所属线程组
    private String group;
    // 线程优先级，范围1-10，数值越大优先级越高
    private int priority;
    // 线程当前状态（NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED）
    private State state;
    // 线程CPU使用率（百分比）
    private double cpu;
    // 线程运行时间增量，用于计算CPU使用率
    private long deltaTime;
    // 线程累计运行时间（毫秒）
    private long time;
    // 线程中断状态标志
    private boolean interrupted;
    // 是否为守护线程
    private boolean daemon;

    /**
     * 默认构造函数
     */
    public ThreadVO() {
    }

    /**
     * 获取线程ID
     * @return 线程唯一标识ID
     */
    public long getId() {
        return id;
    }

    /**
     * 设置线程ID
     * @param id 线程唯一标识ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取线程名称
     * @return 线程名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置线程名称
     * @param name 线程名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取线程组名称
     * @return 线程所属线程组的名称
     */
    public String getGroup() {
        return group;
    }

    /**
     * 设置线程组名称
     * @param group 线程所属线程组的名称
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * 获取线程优先级
     * @return 线程优先级，范围1-10，数值越大优先级越高
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 设置线程优先级
     * @param priority 线程优先级，范围1-10，数值越大优先级越高
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * 获取线程状态
     * @return 线程当前状态（NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED）
     */
    public State getState() {
        return state;
    }

    /**
     * 设置线程状态
     * @param state 线程当前状态
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * 获取线程CPU使用率
     * @return CPU使用率（百分比）
     */
    public double getCpu() {
        return cpu;
    }

    /**
     * 设置线程CPU使用率
     * @param cpu CPU使用率（百分比）
     */
    public void setCpu(double cpu) {
        this.cpu = cpu;
    }

    /**
     * 获取线程运行时间增量
     * @return 运行时间增量，用于计算CPU使用率
     */
    public long getDeltaTime() {
        return deltaTime;
    }

    /**
     * 设置线程运行时间增量
     * @param deltaTime 运行时间增量
     */
    public void setDeltaTime(long deltaTime) {
        this.deltaTime = deltaTime;
    }

    /**
     * 获取线程累计运行时间
     * @return 线程累计运行时间（毫秒）
     */
    public long getTime() {
        return time;
    }

    /**
     * 设置线程累计运行时间
     * @param time 线程累计运行时间（毫秒）
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * 判断线程是否被中断
     * @return true表示线程已中断，false表示未中断
     */
    public boolean isInterrupted() {
        return interrupted;
    }

    /**
     * 设置线程中断状态
     * @param interrupted true表示线程已中断，false表示未中断
     */
    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    /**
     * 判断是否为守护线程
     * @return true表示是守护线程，false表示不是
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * 设置是否为守护线程
     * @param daemon true表示是守护线程，false表示不是
     */
    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * 判断两个ThreadVO对象是否相等
     * 通过线程ID和线程名称来判断相等性
     * @param o 要比较的对象
     * @return true表示相等，false表示不相等
     */
    @Override
    public boolean equals(Object o) {
        // 如果是同一个对象引用，直接返回true
        if (this == o) return true;
        // 如果对象为空或类型不匹配，返回false
        if (o == null || getClass() != o.getClass()) return false;

        // 类型转换为ThreadVO对象
        ThreadVO threadVO = (ThreadVO) o;

        // 比较线程ID是否相等
        if (id != threadVO.id) return false;
        // 比较线程名称是否相等（处理null情况）
        return name != null ? name.equals(threadVO.name) : threadVO.name == null;
    }

    /**
     * 计算ThreadVO对象的哈希码
     * 基于线程ID和线程名称计算哈希值
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        // 使用线程ID的高32位和低32位进行异或运算作为初始值
        int result = (int) (id ^ (id >>> 32));
        // 使用31作为乘数，叠加线程名称的哈希值（31是质数，可以减少哈希冲突）
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

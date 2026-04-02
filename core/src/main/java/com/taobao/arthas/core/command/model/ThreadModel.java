package com.taobao.arthas.core.command.model;

import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;

/**
 * 'thread'命令的数据模型
 * 用于封装thread命令的各种输出结果，支持多种线程信息查询场景
 *
 * @author gongdewei 2020/4/26
 */
public class ThreadModel extends ResultModel {

    // 单个线程信息：用于"thread 12"这样的查询指定线程的场景
    private ThreadInfo threadInfo;

    // 阻塞锁信息：用于"thread -b"查询阻塞线程的场景
    private BlockingLockInfo blockingLockInfo;

    // 繁忙线程列表：用于"thread -n 5"查询最忙的N个线程的场景
    private List<BusyThreadInfo> busyThreads;

    // 线程统计信息：用于"thread"命令的统计输出
    private List<ThreadVO> threadStats;
    // 各状态线程数量统计：key为线程状态，value为该状态的线程数量
    private Map<Thread.State, Integer> threadStateCount;
    // 是否显示所有线程的标志
    private boolean all;

    /**
     * 默认构造函数
     */
    public ThreadModel() {
    }

    /**
     * 构造函数：用于查询单个线程信息
     * @param threadInfo 线程信息对象
     */
    public ThreadModel(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    /**
     * 构造函数：用于查询阻塞锁信息
     * @param blockingLockInfo 阻塞锁信息对象
     */
    public ThreadModel(BlockingLockInfo blockingLockInfo) {
        this.blockingLockInfo = blockingLockInfo;
    }

    /**
     * 构造函数：用于查询繁忙线程列表
     * @param busyThreads 繁忙线程信息列表
     */
    public ThreadModel(List<BusyThreadInfo> busyThreads) {
        this.busyThreads = busyThreads;
    }

    /**
     * 构造函数：用于查询线程统计信息
     * @param threadStats 线程统计信息列表
     * @param threadStateCount 各状态的线程数量统计
     * @param all 是否显示所有线程
     */
    public ThreadModel(List<ThreadVO> threadStats, Map<Thread.State, Integer> threadStateCount, boolean all) {
        this.threadStats = threadStats;
        this.threadStateCount = threadStateCount;
        this.all = all;
    }

    /**
     * 获取模型类型
     * @return 模型类型标识字符串
     */
    @Override
    public String getType() {
        return "thread";
    }

    /**
     * 获取线程信息
     * @return 线程信息对象
     */
    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    /**
     * 设置线程信息
     * @param threadInfo 线程信息对象
     */
    public void setThreadInfo(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    /**
     * 获取阻塞锁信息
     * @return 阻塞锁信息对象
     */
    public BlockingLockInfo getBlockingLockInfo() {
        return blockingLockInfo;
    }

    /**
     * 设置阻塞锁信息
     * @param blockingLockInfo 阻塞锁信息对象
     */
    public void setBlockingLockInfo(BlockingLockInfo blockingLockInfo) {
        this.blockingLockInfo = blockingLockInfo;
    }

    /**
     * 获取繁忙线程列表
     * @return 繁忙线程信息列表
     */
    public List<BusyThreadInfo> getBusyThreads() {
        return busyThreads;
    }

    /**
     * 设置繁忙线程列表
     * @param busyThreads 繁忙线程信息列表
     */
    public void setBusyThreads(List<BusyThreadInfo> busyThreads) {
        this.busyThreads = busyThreads;
    }

    /**
     * 获取线程统计信息列表
     * @return 线程统计信息列表
     */
    public List<ThreadVO> getThreadStats() {
        return threadStats;
    }

    /**
     * 设置线程统计信息列表
     * @param threadStats 线程统计信息列表
     */
    public void setThreadStats(List<ThreadVO> threadStats) {
        this.threadStats = threadStats;
    }

    /**
     * 获取各状态线程数量统计
     * @return 线程状态与数量的映射，key为线程状态，value为该状态的线程数量
     */
    public Map<Thread.State, Integer> getThreadStateCount() {
        return threadStateCount;
    }

    /**
     * 设置各状态线程数量统计
     * @param threadStateCount 线程状态与数量的映射
     */
    public void setThreadStateCount(Map<Thread.State, Integer> threadStateCount) {
        this.threadStateCount = threadStateCount;
    }

    /**
     * 获取是否显示所有线程的标志
     * @return true表示显示所有线程，false表示不显示
     */
    public boolean isAll() {
        return all;
    }

    /**
     * 设置是否显示所有线程的标志
     * @param all true表示显示所有线程，false表示不显示
     */
    public void setAll(boolean all) {
        this.all = all;
    }
}

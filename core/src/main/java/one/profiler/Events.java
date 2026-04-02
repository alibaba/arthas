/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.profiler;

/**
 * 预定义的事件类型常量，用于在{@link AsyncProfiler#start(String, long)}方法中指定要监听的性能事件
 * 这些事件类型决定了性能分析器采集何种类型的性能数据
 */
public class Events {
    /**
     * CPU事件
     * 基于CPU时钟周期的采样事件
     * 当线程在CPU上执行时进行采样，可以捕获CPU密集型代码的性能瓶颈
     * 适用于分析纯CPU计算性能问题
     */
    public static final String CPU    = "cpu";

    /**
     * 内存分配事件
     * 监控Java堆内存分配情况
     * 在每次对象分配时记录调用栈，用于分析内存分配热点
     * 适用于发现内存分配过频繁或单次分配过大的问题
     */
    public static final String ALLOC  = "alloc";

    /**
     * 锁事件
     * 监控Java对象锁的竞争情况
     * 记录线程阻塞在锁等待上的时间，用于分析锁竞争导致的性能问题
     * 适用于发现多线程并发访问同步块/方法时的性能瓶颈
     */
    public static final String LOCK   = "lock";

    /**
     * 墙钟时间事件（Wall Clock Time）
     * 基于实际经过时间的采样，无论线程处于什么状态（运行、阻塞、睡眠等）
     * 可以捕获所有类型的性能问题，包括I/O等待、锁竞争、网络延迟等
     * 相比CPU采样，wall采样能更全面地反映应用的实际性能状况
     */
    public static final String WALL   = "wall";

    /**
     * CPU定时器事件（CPUTime）
     * 使用CPU定时器进行采样，记录线程实际占用CPU的时间
     * 与wall不同，ctimer只统计线程在CPU上执行的时间
     * 适用于分析线程真实的CPU使用情况
     */
    public static final String CTIMER = "ctimer";

    /**
     * 间隔定时器事件（Interval Timer）
     * 使用操作系统的间隔定时器机制进行采样
     * itimer是一种传统的性能分析方式，基于setitimer系统调用
     * 采样频率相对固定，开销较小
     */
    public static final String ITIMER = "itimer";
}
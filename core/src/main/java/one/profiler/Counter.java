/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.profiler;

/**
 * 在生成折叠式堆栈跟踪格式的性能分析数据时使用的度量指标类型
 * 折叠式格式将每个完整的调用栈压缩为一行，用分号分隔各个方法
 * 此枚举定义了如何统计每个调用栈的权重
 */
public enum Counter {
    /**
     * 使用样本数作为度量指标
     * 统计每个调用栈被采样的次数
     * 适用于基于事件（如CPU时钟）的采样分析
     */
    SAMPLES,

    /**
     * 使用总时间作为度量指标
     * 统计每个调用栈的总耗时
     * 适用于基于时间（如wall clock time）的分析
     * 能够反映方法实际执行时间，包括等待时间
     */
    TOTAL
}
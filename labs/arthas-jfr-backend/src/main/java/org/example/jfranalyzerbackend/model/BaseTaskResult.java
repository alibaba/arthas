package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 基础任务结果抽象类
 * 定义任务结果的基本结构和通用行为
 */
@Setter
@Getter
public abstract class BaseTaskResult {
    
    /**
     * 关联的任务信息
     */
    private Task task;
    
    /**
     * 堆栈跟踪采样数据映射
     */
    private Map<StackTrace, Long> samples;

    /**
     * 带任务参数的构造函数
     * @param task 关联的任务
     */
    public BaseTaskResult(Task task) {
        this.task = task;
        this.samples = new HashMap<>();
    }

    /**
     * 默认构造函数
     */
    public BaseTaskResult() {
        this.samples = new HashMap<>();
    }

    /**
     * 合并堆栈跟踪样本数据
     * @param stackTrace 堆栈跟踪
     * @param value 数值
     */
    public void combineSampleData(StackTrace stackTrace, long value) {
        if (samples == null) {
            samples = new HashMap<>();
        }
        if (stackTrace == null || value <= 0) {
            return;
        }
        samples.put(stackTrace, samples.containsKey(stackTrace) ? samples.get(stackTrace) + value : value);
    }

    /**
     * 获取结果数值（子类实现具体逻辑）
     * @return 数值
     */
    public abstract long getValue();

    /**
     * 设置结果数值（子类实现具体逻辑）
     * @param value 数值
     */
    public abstract void setValue(long value);
}

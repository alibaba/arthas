
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 任务求和结果类
 * 用于存储任务数值累加相关的统计信息
 */
@Setter
@Getter
public class TaskSum extends BaseTaskResult {
    
    /**
     * 数值总和
     */
    private long sum;

    /**
     * 默认构造函数
     */
    public TaskSum() {
        super();
    }

    /**
     * 带任务参数的构造函数
     * @param task 关联的任务
     */
    public TaskSum(Task task) {
        super(task);
    }

    @Override
    public long getValue() {
        return sum;
    }

    @Override
    public void setValue(long value) {
        this.sum = value;
    }
}

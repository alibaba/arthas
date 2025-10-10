
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 任务计数结果类
 * 用于存储任务执行次数相关的统计信息
 */
@Setter
@Getter
public class TaskCount extends BaseTaskResult {
    
    /**
     * 事件计数
     */
    private long count;

    /**
     * 默认构造函数
     */
    public TaskCount() {
        super();
    }

    /**
     * 带任务参数的构造函数
     * @param task 关联的任务
     */
    public TaskCount(Task task) {
        super(task);
    }

    @Override
    public long getValue() {
        return count;
    }

    @Override
    public void setValue(long value) {
        this.count = value;
    }
}

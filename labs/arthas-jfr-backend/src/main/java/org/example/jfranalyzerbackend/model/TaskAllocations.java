
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 任务分配结果类
 * 继承自TaskCount，专门用于存储内存分配相关的统计信息
 */
@Setter
@Getter
public class TaskAllocations extends TaskCount {
    
    /**
     * 分配次数
     */
    private long allocations;
}


package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.model.jfr.RecordedThread;

import java.util.Map;

/**
 * 任务数据基类
 * 存储线程相关的采样数据和统计信息
 */
@Setter
@Getter
public class TaskData {
    
    /**
     * 关联的线程信息
     */
    private RecordedThread thread;

    /**
     * 堆栈跟踪采样数据映射
     */
    private Map<RecordedStackTrace, Long> samples;

    /**
     * 构造函数
     * @param thread 关联的线程
     */
    public TaskData(RecordedThread thread) {
        this.thread = thread;
    }
}

package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskCPUTime extends BaseTaskResult {
    private long user;
    private long system;

    public TaskCPUTime() {
        super();
    }

    public TaskCPUTime(Task task) {
        super(task);
    }

    public long totalCPUTime() {
        return user + system;
    }

    @Override
    public long getValue() {
        return totalCPUTime();
    }

    @Override
    public void setValue(long value) {
        // CPU时间不能直接设置，需要通过user和system设置
        throw new UnsupportedOperationException("CPU time cannot be set directly, use setUser() and setSystem()");
    }
}

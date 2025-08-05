
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskCPUTime extends TaskResultBase {

    private long user;

    private long system;

    public TaskCPUTime() {
    }

    public long totalCPUTime() {
        return user + system;
    }

    public TaskCPUTime(Task task) {
        super(task);
    }
}

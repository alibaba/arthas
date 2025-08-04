
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskCount extends TaskResultBase {
    private long count;

    public TaskCount() {
    }

    public TaskCount(Task task) {
        super(task);
    }
}

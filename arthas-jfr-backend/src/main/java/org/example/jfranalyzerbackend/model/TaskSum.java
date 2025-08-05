
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskSum extends TaskResultBase {
    public TaskSum() {
        super(null);
    }

    public TaskSum(Task task) {
        super(task);
    }

    private long sum;
}


package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class TaskResultBase {
    private Task task;
    private Map<StackTrace, Long> samples;

    public TaskResultBase(Task task) {
        this.task = task;
        samples = new HashMap<>();
    }

    public TaskResultBase() {
    }

    public void merge(StackTrace st, long value) {
        if (samples == null) {
            samples = new HashMap<>();
        }
        if (st == null || value <= 0) {
            return;
        }
        samples.put(st, samples.containsKey(st) ? samples.get(st) + value : value);
    }
}

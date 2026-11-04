package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskAllocatedMemory extends TaskSum {
    public TaskAllocatedMemory() {
        super(null);
    }

    private long allocatedMemory;
}

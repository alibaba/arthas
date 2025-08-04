
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.model.jfr.RecordedThread;


import java.util.Map;

@Setter
@Getter
public class TaskData {
    public TaskData(RecordedThread thread) {
        this.thread = thread;
    }

    private RecordedThread thread;

    private Map<RecordedStackTrace, Long> samples;
}
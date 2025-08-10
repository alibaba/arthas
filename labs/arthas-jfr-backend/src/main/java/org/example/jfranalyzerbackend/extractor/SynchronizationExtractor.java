
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynchronizationExtractor extends SumExtractor {
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.JAVA_MONITOR_ENTER);
        }
    });

    public SynchronizationExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitMonitorEnter(RecordedEvent event) {
        visitEvent(event, event.getDurationNano());
    }

    @Override
    void visitThreadPark(RecordedEvent event) {
        visitEvent(event, event.getDurationNano());
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskSums());
        result.setSynchronization(tsResult);
    }
}

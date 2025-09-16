
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynchronizationExtractor extends BaseValueExtractor {
    private static final List<String> INTERESTED = createInterestedList(EventConstant.JAVA_MONITOR_ENTER);

    public SynchronizationExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitMonitorEnter(RecordedEvent event) {
        processValueEvent(event, event.getDurationNano());
    }

    @Override
    void visitThreadPark(RecordedEvent event) {
        processValueEvent(event, event.getDurationNano());
    }

    @Override
    public void fillResult(AnalysisResult result) {
        List<TaskSum> taskSums = generateTaskResults(TaskSum.class);
        populateResult(result, taskSums, result::setSynchronization);
    }
}

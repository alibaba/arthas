
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadParkExtractor extends BaseValueExtractor {
    protected static final List<String> INTERESTED = createInterestedList(EventConstant.THREAD_PARK);

    public ThreadParkExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitThreadPark(RecordedEvent event) {
        long eventValue = event.getDurationNano();
        processValueEvent(event, eventValue);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        List<TaskSum> taskSums = generateTaskResults(TaskSum.class);
        populateResult(result, taskSums, result::setThreadPark);
    }
}

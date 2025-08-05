
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadSleepTimeExtractor extends SumExtractor {
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.THREAD_SLEEP);
        }
    });

    public ThreadSleepTimeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitThreadSleep(RecordedEvent event) {
        visitEvent(event);
    }

    private void visitEvent(RecordedEvent event) {
        long eventValue = event.getLong("time") * 1000 * 1000;
        visitEvent(event, eventValue);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskSums());
        result.setThreadSleepTime(tsResult);
    }
}

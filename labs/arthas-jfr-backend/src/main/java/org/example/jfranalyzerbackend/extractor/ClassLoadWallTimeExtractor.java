
package org.example.jfranalyzerbackend.extractor;

import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.List;

public class ClassLoadWallTimeExtractor extends BaseValueExtractor {
    private static final List<String> INTERESTED = createInterestedList(EventConstant.CLASS_LOAD);

    public ClassLoadWallTimeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitClassLoad(RecordedEvent event) {
        processValueEvent(event, event.getDurationNano());
    }

    @Override
    public void fillResult(AnalysisResult result) {
        List<TaskSum> taskSums = generateTaskResults(TaskSum.class);
        populateResult(result, taskSums, result::setClassLoadWallTime);
    }
}

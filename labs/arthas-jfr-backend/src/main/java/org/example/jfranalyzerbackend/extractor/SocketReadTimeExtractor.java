
package org.example.jfranalyzerbackend.extractor;

import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.List;

public class SocketReadTimeExtractor extends BaseValueExtractor {
    private static final List<String> INTERESTED = createInterestedList(EventConstant.SOCKET_READ);

    public SocketReadTimeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitSocketRead(RecordedEvent event) {
        long eventValue = event.getDurationNano();
        processValueEvent(event, eventValue);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        List<TaskSum> taskSums = generateTaskResults(TaskSum.class);
        populateResult(result, taskSums, result::setSocketReadTime);
    }
}

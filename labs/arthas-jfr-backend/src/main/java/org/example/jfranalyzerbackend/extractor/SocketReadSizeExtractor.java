
package org.example.jfranalyzerbackend.extractor;

import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.List;

public class SocketReadSizeExtractor extends BaseValueExtractor {
    private static final List<String> INTERESTED = createInterestedList(EventConstant.SOCKET_READ);

    public SocketReadSizeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitSocketRead(RecordedEvent event) {
        long eventValue = event.getLong("bytesRead");
        processValueEvent(event, eventValue);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        List<TaskSum> taskSums = generateTaskResults(TaskSum.class);
        populateResult(result, taskSums, result::setSocketReadSize);
    }
}

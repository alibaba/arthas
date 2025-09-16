
package org.example.jfranalyzerbackend.extractor;

import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.List;

public class FileReadExtractor extends BaseValueExtractor {
    private static final List<String> INTERESTED = createInterestedList(EventConstant.FILE_READ);

    public FileReadExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitFileRead(RecordedEvent event) {
        long bytes = event.getLong("bytesRead");
        processValueEvent(event, bytes);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        List<TaskSum> taskSums = generateTaskResults(TaskSum.class);
        populateResult(result, taskSums, result::setFileReadSize);
    }
}
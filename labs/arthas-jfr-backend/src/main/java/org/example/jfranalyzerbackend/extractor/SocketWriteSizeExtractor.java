
package org.example.jfranalyzerbackend.extractor;


import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocketWriteSizeExtractor extends SumExtractor {
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.SOCKET_WRITE);
        }
    });

    public SocketWriteSizeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitSocketWrite(RecordedEvent event) {
        visitEvent(event);
    }

    private void visitEvent(RecordedEvent event) {
        long eventValue = event.getLong("bytesWritten");
        visitEvent(event, eventValue);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskSums());
        result.setSocketWriteSize(tsResult);
    }
}

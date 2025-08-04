
package org.example.jfranalyzerbackend.extractor;


import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileWriteExtractor extends FileIOExtractor {
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.FILE_WRITE);
        }
    });

    public FileWriteExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitFileWrite(RecordedEvent event) {
        long bytes = event.getLong("bytesWritten");
        visitEvent(event, bytes);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskSums());
        result.setFileWriteSize(tsResult);
    }
}
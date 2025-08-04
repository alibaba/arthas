
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskCount;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassLoadCountExtractor extends CountExtractor {
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.CLASS_LOAD);
        }
    });

    public ClassLoadCountExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitClassLoad(RecordedEvent event) {
        visitEvent(event);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCount> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskCounts());
        result.setClassLoadCount(tsResult);
    }
}

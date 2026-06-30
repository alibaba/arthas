
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.Task;
import org.example.jfranalyzerbackend.model.TaskCount;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;
import org.example.jfranalyzerbackend.util.StackTraceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NativeExecutionExtractor extends CountExtractor {

    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.NATIVE_EXECUTION_SAMPLE);
        }
    });

    public NativeExecutionExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitNativeExecutionSample(RecordedEvent event) {
        processCountEvent(event);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCount> nativeResult = new DimensionResult<>();
        nativeResult.setList(generateCountResults());
        result.setNativeExecutionSamples(nativeResult);
    }
}

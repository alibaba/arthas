
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
        visitEvent(event);
    }

    private List<TaskCount> buildTaskExecutionSamples() {
        List<TaskCount> nativeSamples = new ArrayList<>();

        for (TaskCountData data : this.data.values()) {
            if (data.count == 0) {
                continue;
            }

            TaskCount threadSamples = new TaskCount();
            Task ta = new Task();
            ta.setId(data.getThread().getJavaThreadId());
            ta.setName(data.getThread().getJavaName());
            threadSamples.setTask(ta);

            if (data.getSamples() != null) {
                threadSamples.setCount(data.count);
                threadSamples.setSamples(data.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum
                        )
                ));
            }

            nativeSamples.add(threadSamples);
        }

        nativeSamples.sort((o1, o2) -> {
            long delta = o2.getCount() - o1.getCount();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return nativeSamples;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCount> nativeResult = new DimensionResult<>();
        nativeResult.setList(buildTaskExecutionSamples());
        result.setNativeExecutionSamples(nativeResult);
    }
}

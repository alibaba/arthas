
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.Task;
import org.example.jfranalyzerbackend.model.TaskAllocatedMemory;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.util.StackTraceUtil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllocatedMemoryExtractor extends AllocationsExtractor {
    public AllocatedMemoryExtractor(JFRAnalysisContext context) {
        super(context);
    }

    @Override
    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        if (this.useObjectAllocationSample) {
            return;
        }
        this.visitTLABEvent(event, "tlabSize");
    }

    @Override
    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        if (this.useObjectAllocationSample) {
            return;
        }
        this.visitTLABEvent(event, "allocationSize");
    }

    @Override
    void visitObjectAllocationSample(RecordedEvent event) {
        this.visitTLABEvent(event, "weight");
    }

    void visitTLABEvent(RecordedEvent event, String fieldName) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            stackTrace = StackTraceUtil.DUMMY_STACK_TRACE;
        }

        AllocationsExtractor.AllocTaskData allocThreadData = getThreadData(event.getThread());
        if (allocThreadData.getSamples() == null) {
            allocThreadData.setSamples(new HashMap<>());
        }

        long eventTotal = event.getLong(fieldName);

        allocThreadData.getSamples().compute(stackTrace, (k, temp) -> temp == null ? eventTotal : temp + eventTotal);
        allocThreadData.allocatedMemory += eventTotal;
    }

    private List<TaskAllocatedMemory> buildThreadAllocatedMemory() {
        List<TaskAllocatedMemory> taskAllocatedMemoryList = new ArrayList<>();

        for (AllocTaskData data : this.data.values()) {
            if (data.allocatedMemory == 0) {
                continue;
            }

            TaskAllocatedMemory taskAllocatedMemory = new TaskAllocatedMemory();
            Task ta = new Task();
            ta.setId(data.getThread().getJavaThreadId());
            ta.setName(data.getThread().getJavaName());
            taskAllocatedMemory.setTask(ta);

            if (data.getSamples() != null) {
                taskAllocatedMemory.setAllocatedMemory(data.allocatedMemory);
                taskAllocatedMemory.setSamples(data.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum)
                ));
            }

            taskAllocatedMemoryList.add(taskAllocatedMemory);
        }

        taskAllocatedMemoryList.sort((o1, o2) -> {
            long delta = o2.getAllocatedMemory() - o1.getAllocatedMemory();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return taskAllocatedMemoryList;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskAllocatedMemory> memResult = new DimensionResult<>();
        memResult.setList(buildThreadAllocatedMemory());
        result.setAllocatedMemory(memResult);
    }
}

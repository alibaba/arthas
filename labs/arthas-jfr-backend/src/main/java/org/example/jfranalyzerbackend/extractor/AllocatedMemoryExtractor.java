package org.example.jfranalyzerbackend.extractor;

import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.StackTrace;
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

/**
 * 已分配内存事件提取器
 * 专门处理内存分配大小相关的JFR事件，继承自AllocationsExtractor
 */
public class AllocatedMemoryExtractor extends AllocationsExtractor {
    
    public AllocatedMemoryExtractor(JFRAnalysisContext context) {
        super(context);
    }

    @Override
    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        if (this.objectAllocationSamplingEnabled) {
            return;
        }
        this.processMemoryAllocationEvent(event, "tlabSize");
    }

    @Override
    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        if (this.objectAllocationSamplingEnabled) {
            return;
        }
        this.processMemoryAllocationEvent(event, "allocationSize");
    }

    @Override
    void visitObjectAllocationSample(RecordedEvent event) {
        this.processMemoryAllocationEvent(event, "weight");
    }

    private void processMemoryAllocationEvent(RecordedEvent event, String sizeFieldName) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            stackTrace = StackTraceUtil.DUMMY_STACK_TRACE;
        }

        AllocationMetrics memoryMetrics = obtainThreadMetrics(event.getThread());
        initializeSamplesIfNeeded(memoryMetrics);

        long allocationSize = event.getLong(sizeFieldName);
        updateMemoryMetrics(memoryMetrics, stackTrace, allocationSize);
    }

    private void updateMemoryMetrics(AllocationMetrics metrics, RecordedStackTrace stackTrace, long size) {
        metrics.getSamples().compute(stackTrace, (key, existingSize) -> 
            existingSize == null ? size : existingSize + size);
        metrics.totalAllocatedBytes += size;
    }

    private List<TaskAllocatedMemory> generateMemoryAllocationResults() {
        List<TaskAllocatedMemory> memoryResults = new ArrayList<>();

        for (AllocationMetrics metrics : this.threadMetrics.values()) {
            if (metrics.totalAllocatedBytes == 0) {
                continue;
            }

            TaskAllocatedMemory memoryResult = createMemoryAllocationResult(metrics);
            memoryResults.add(memoryResult);
        }

        return sortMemoryAllocationsBySize(memoryResults);
    }

    private TaskAllocatedMemory createMemoryAllocationResult(AllocationMetrics metrics) {
        TaskAllocatedMemory result = new TaskAllocatedMemory();
        Task taskInfo = createTaskInfo(metrics.getThread());
        result.setTask(taskInfo);

        if (metrics.getSamples() != null) {
            result.setAllocatedMemory(metrics.totalAllocatedBytes);
            result.setSamples(transformSamples(metrics.getSamples()));
        }

        return result;
    }

    private List<TaskAllocatedMemory> sortMemoryAllocationsBySize(List<TaskAllocatedMemory> allocations) {
        allocations.sort((first, second) -> {
            long sizeDifference = second.getAllocatedMemory() - first.getAllocatedMemory();
            return sizeDifference > 0 ? 1 : (sizeDifference == 0 ? 0 : -1);
        });
        return allocations;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskAllocatedMemory> memoryDimension = new DimensionResult<>();
        memoryDimension.setList(generateMemoryAllocationResults());
        result.setAllocatedMemory(memoryDimension);
    }
}

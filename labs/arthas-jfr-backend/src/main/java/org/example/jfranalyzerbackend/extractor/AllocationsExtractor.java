
package org.example.jfranalyzerbackend.extractor;

import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.*;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.model.jfr.RecordedThread;
import org.example.jfranalyzerbackend.util.StackTraceUtil;

import java.util.*;
import java.util.stream.Collectors;

import static org.example.jfranalyzerbackend.enums.EventConstant.OBJECT_ALLOCATION_SAMPLE;

/**
 * 内存分配事件提取器
 * 负责处理对象分配相关的JFR事件，包括TLAB内分配、TLAB外分配和分配采样
 */
public class AllocationsExtractor extends Extractor {
    protected boolean objectAllocationSamplingEnabled;

    private static final List<String> MONITORED_EVENTS = Collections.unmodifiableList(Arrays.asList(
            EventConstant.OBJECT_ALLOCATION_IN_NEW_TLAB,
            EventConstant.OBJECT_ALLOCATION_OUTSIDE_TLAB,
            OBJECT_ALLOCATION_SAMPLE
    ));

    /**
     * 线程分配数据容器
     * 存储每个线程的分配统计信息
     */
    protected static class AllocationMetrics extends TaskData {
        AllocationMetrics(RecordedThread thread) {
            super(thread);
        }

        public long allocationCount;
        public long totalAllocatedBytes;
    }

    protected final Map<Long, AllocationMetrics> threadMetrics = new HashMap<>();

    public AllocationsExtractor(JFRAnalysisContext context) {
        super(context, MONITORED_EVENTS);
        initializeAllocationSampling();
    }

    private void initializeAllocationSampling() {
        try {
            this.objectAllocationSamplingEnabled = this.context.getActiveSettingBool(OBJECT_ALLOCATION_SAMPLE, "enabled");
        } catch (Exception e) {
            this.objectAllocationSamplingEnabled = false;
        }
    }

    protected AllocationMetrics obtainThreadMetrics(RecordedThread thread) {
        return threadMetrics.computeIfAbsent(thread.getJavaThreadId(), 
            threadId -> new AllocationMetrics(thread));
    }

    @Override
    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        if (objectAllocationSamplingEnabled) {
            return;
        }
        processAllocationEvent(event);
    }

    @Override
    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        if (objectAllocationSamplingEnabled) {
            return;
        }
        processAllocationEvent(event);
    }

    @Override
    void visitObjectAllocationSample(RecordedEvent event) {
        processAllocationEvent(event);
    }

    private void processAllocationEvent(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            stackTrace = StackTraceUtil.DUMMY_STACK_TRACE;
        }

        AllocationMetrics metrics = obtainThreadMetrics(event.getThread());
        initializeSamplesIfNeeded(metrics);
        
        metrics.getSamples().compute(stackTrace, (key, existingCount) -> 
            existingCount == null ? 1L : existingCount + 1L);
        metrics.allocationCount += 1;
    }

    protected void initializeSamplesIfNeeded(AllocationMetrics metrics) {
        if (metrics.getSamples() == null) {
            metrics.setSamples(new HashMap<>());
        }
    }

    private List<TaskAllocations> generateAllocationResults() {
        List<TaskAllocations> allocationResults = new ArrayList<>();
        
        for (AllocationMetrics metrics : this.threadMetrics.values()) {
            if (metrics.allocationCount == 0) {
                continue;
            }

            TaskAllocations allocationResult = createAllocationResult(metrics);
            allocationResults.add(allocationResult);
        }

        return sortAllocationsByCount(allocationResults);
    }

    private TaskAllocations createAllocationResult(AllocationMetrics metrics) {
        TaskAllocations result = new TaskAllocations();
        Task taskInfo = createTaskInfo(metrics.getThread());
        result.setTask(taskInfo);

        if (metrics.getSamples() != null) {
            result.setAllocations(metrics.allocationCount);
            result.setSamples(transformSamples(metrics.getSamples()));
        }

        return result;
    }

    protected Task createTaskInfo(RecordedThread thread) {
        Task task = new Task();
        task.setId(thread.getJavaThreadId());
        task.setName(thread.getJavaName());
        return task;
    }

    protected Map<StackTrace, Long> transformSamples(Map<RecordedStackTrace, Long> rawSamples) {
        return rawSamples.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> StackTraceUtil.build(entry.getKey(), context.getSymbols()),
                    Map.Entry::getValue,
                    Long::sum
                ));
    }

    private List<TaskAllocations> sortAllocationsByCount(List<TaskAllocations> allocations) {
        allocations.sort((first, second) -> {
            long difference = second.getAllocations() - first.getAllocations();
            return difference > 0 ? 1 : (difference == 0 ? 0 : -1);
        });
        return allocations;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskAllocations> allocationDimension = new DimensionResult<>();
        allocationDimension.setList(generateAllocationResults());
        result.setAllocations(allocationDimension);
    }
}

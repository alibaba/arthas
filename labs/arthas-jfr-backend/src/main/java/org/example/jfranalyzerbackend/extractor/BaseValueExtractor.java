package org.example.jfranalyzerbackend.extractor;

import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.BaseTaskResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.StackTrace;
import org.example.jfranalyzerbackend.model.Task;
import org.example.jfranalyzerbackend.model.TaskData;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.model.jfr.RecordedThread;
import org.example.jfranalyzerbackend.util.StackTraceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基础值提取器抽象类
 * 提供通用的值累加和统计功能，用于处理需要累加数值的JFR事件
 */
public abstract class BaseValueExtractor extends Extractor {
    
    protected static final List<String> createInterestedList(String... eventTypes) {
        return Collections.unmodifiableList(new ArrayList<>() {{
            for (String eventType : eventTypes) {
                add(eventType);
            }
        }});
    }

    /**
     * 任务值数据容器
     * 存储每个线程的数值统计信息
     */
    public static class ValueMetrics extends TaskData {
        ValueMetrics(RecordedThread thread) {
            super(thread);
        }
        long totalValue;
    }

    private final Map<Long, ValueMetrics> threadValues = new HashMap<>();

    BaseValueExtractor(JFRAnalysisContext context, List<String> interested) {
        super(context, interested);
    }

    private ValueMetrics obtainValueMetrics(RecordedThread thread) {
        return threadValues.computeIfAbsent(thread.getJavaThreadId(), 
            threadId -> new ValueMetrics(thread));
    }

    protected void processValueEvent(RecordedEvent event, long eventValue) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            return;
        }

        ValueMetrics metrics = obtainValueMetrics(event.getThread());
        initializeSamplesIfNeeded(metrics);
        
        metrics.getSamples().compute(stackTrace, (key, existingValue) -> 
            existingValue == null ? eventValue : existingValue + eventValue);
        metrics.totalValue += eventValue;
    }

    protected void processValueEvent(RecordedEvent event) {
        processValueEvent(event, 1);
    }

    private void initializeSamplesIfNeeded(ValueMetrics metrics) {
        if (metrics.getSamples() == null) {
            metrics.setSamples(new HashMap<>());
        }
    }

    /**
     * 生成任务结果列表
     */
    protected <T extends BaseTaskResult> List<T> generateTaskResults(Class<T> resultClass) {
        List<T> results = new ArrayList<>();
        
        for (ValueMetrics metrics : this.threadValues.values()) {
            if (metrics.totalValue == 0) {
                continue;
            }

            try {
                T result = createTaskResult(resultClass, metrics);
                results.add(result);
            } catch (Exception e) {
                // 如果无法创建实例，跳过
                continue;
            }
        }

        return sortResultsByValue(results);
    }

    private <T extends BaseTaskResult> T createTaskResult(Class<T> resultClass, ValueMetrics metrics) throws Exception {
        T result = resultClass.getDeclaredConstructor().newInstance();
        Task taskInfo = createTaskInfo(metrics.getThread());
        result.setTask(taskInfo);

        if (metrics.getSamples() != null) {
            result.setValue(metrics.totalValue);
            result.setSamples(transformSamples(metrics.getSamples()));
        }

        return result;
    }

    private Task createTaskInfo(RecordedThread thread) {
        Task task = new Task();
        task.setId(thread.getJavaThreadId());
        task.setName(context.getThread(thread).getName());
        return task;
    }

    private Map<StackTrace, Long> transformSamples(Map<RecordedStackTrace, Long> rawSamples) {
        return rawSamples.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> StackTraceUtil.build(entry.getKey(), context.getSymbols()),
                    Map.Entry::getValue,
                    Long::sum
                ));
    }

    private <T extends BaseTaskResult> List<T> sortResultsByValue(List<T> results) {
        results.sort((first, second) -> {
            long difference = second.getValue() - first.getValue();
            return difference > 0 ? 1 : (difference == 0 ? 0 : -1);
        });
        return results;
    }

    /**
     * 填充结果到AnalysisResult
     */
    protected <T extends BaseTaskResult> void populateResult(AnalysisResult result, 
                                                           List<T> taskResults, 
                                                           java.util.function.Consumer<DimensionResult<T>> setter) {
        DimensionResult<T> dimensionResult = new DimensionResult<>();
        dimensionResult.setList(taskResults);
        setter.accept(dimensionResult);
    }
}

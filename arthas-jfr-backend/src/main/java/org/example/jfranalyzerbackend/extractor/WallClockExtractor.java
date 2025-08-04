
package org.example.jfranalyzerbackend.extractor;

import lombok.extern.slf4j.Slf4j;
import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskData;
import org.example.jfranalyzerbackend.model.TaskSum;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.model.jfr.RecordedThread;
import org.example.jfranalyzerbackend.util.StackTraceUtil;


import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class WallClockExtractor extends Extractor {
    private static final int ASYNC_PROFILER_DEFAULT_INTERVAL = 50 * 1000 * 1000;

    private static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.ACTIVE_SETTING);
            add(EventConstant.WALL_CLOCK_SAMPLE);
        }
    });

    static class TaskWallClockData extends TaskData {
        private long begin = 0;
        private long end = 0;
        private long sampleCount = 0;

        TaskWallClockData(RecordedThread thread) {
            super(thread);
        }

        void updateTime(long time) {
            if (begin == 0 || time < begin) {
                begin = time;
            }
            if (end == 0 || time > end) {
                end = time;
            }
        }

        long getDuration() {
            return end - begin;
        }
    }

    private final Map<Long, TaskWallClockData> data = new HashMap<>();
    private long methodSampleEventId = -1;
    private long interval; // nano

    private boolean isWallClockEvents = false;

    public WallClockExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);

        Long id = context.getEventTypeId(EventConstant.WALL_CLOCK_SAMPLE);
        if (id != null) {
            methodSampleEventId = context.getEventTypeId(EventConstant.WALL_CLOCK_SAMPLE);
        }
    }

    TaskWallClockData getThreadData(RecordedThread thread) {
        return data.computeIfAbsent(thread.getJavaThreadId(), i -> new TaskWallClockData(thread));
    }

    @Override
    void visitActiveSetting(RecordedEvent event) {
        if (EventConstant.EVENT.equals(event.getString("name")) && EventConstant.WALL.equals(event.getString("value"))) {
            this.isWallClockEvents = true;
        }

        if (event.getActiveSetting().eventId() == methodSampleEventId) {
            if (EventConstant.WALL.equals(event.getString("name"))) {
                this.isWallClockEvents = true;
                this.interval = Long.parseLong(event.getString("value")) * 1000 * 1000;
            }
            if (EventConstant.INTERVAL.equals(event.getString("name"))) {
                this.interval = Long.parseLong(event.getString("value")) * 1000 * 1000;
            }
        }
    }

    @Override
    void visitExecutionSample(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            return;
        }

        RecordedThread thread = event.getThread("eventThread");
        if (thread == null) {
            thread = event.getThread("sampledThread");
        }
        if (thread == null) {
            return;
        }
        TaskWallClockData taskWallClockData = getThreadData(thread);

        if (taskWallClockData.getSamples() == null) {
            taskWallClockData.setSamples(new HashMap<>());
        }
        taskWallClockData.updateTime(event.getStartTimeNanos());
        taskWallClockData.getSamples().compute(stackTrace, (k, count) -> count == null ? 1 : count + 1);
        taskWallClockData.sampleCount++;
    }

    private List<TaskSum> buildThreadWallClock() {
        List<TaskSum> taskSumList = new ArrayList<>();
        if (!isWallClockEvents) {
            return taskSumList;
        }

        if (this.interval <= 0) {
            this.interval = ASYNC_PROFILER_DEFAULT_INTERVAL;
            log.warn("use default interval: " + ASYNC_PROFILER_DEFAULT_INTERVAL / 1000 / 1000 + " ms");
        }
        Map<Long, TaskSum> map = new HashMap<>();
        for (TaskWallClockData data : this.data.values()) {
            if (data.getSamples() == null) {
                continue;
            }
            TaskSum taskSum = new TaskSum();
            taskSum.setTask(context.getThread(data.getThread()));
            taskSum.setSum(data.sampleCount > 1 ? data.getDuration() : this.interval);
            data.getSamples().replaceAll((k, v) -> v * (taskSum.getSum() / data.sampleCount));
            taskSum.setSamples(data.getSamples().entrySet().stream().collect(
                    Collectors.toMap(
                            e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                            Map.Entry::getValue,
                            Long::sum)
            ));
            map.put(data.getThread().getJavaThreadId(), taskSum);
        }

        map.forEach((k, v) -> {
            taskSumList.add(v);
        });

        taskSumList.sort((o1, o2) -> {
            long delta = o2.getSum() - o1.getSum();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return taskSumList;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> wallClockResult = new DimensionResult<>();
        wallClockResult.setList(buildThreadWallClock());
        result.setWallClock(wallClockResult);
    }
}

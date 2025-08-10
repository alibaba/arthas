
package org.example.jfranalyzerbackend.extractor;

import lombok.extern.slf4j.Slf4j;
import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.*;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.model.jfr.RecordedThread;
import org.example.jfranalyzerbackend.util.GCUtil;
import org.example.jfranalyzerbackend.util.StackTraceUtil;
import org.example.jfranalyzerbackend.util.TimeUtil;


import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CPUTimeExtractor extends Extractor {

    private static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.UNSIGNED_INT_FLAG);
            add(EventConstant.GARBAGE_COLLECTION);
            add(EventConstant.ACTIVE_SETTING);
            add(EventConstant.CPU_INFORMATION);
            add(EventConstant.ENV_VAR);
            add(EventConstant.THREAD_START);
            add(EventConstant.THREAD_CPU_LOAD);
            add(EventConstant.EXECUTION_SAMPLE);
        }
    });

    private static class CpuTaskData extends TaskData {
        CpuTaskData(RecordedThread thread) {
            super(thread);
        }

        Instant start;

        long user = 0;

        long system = 0;

        long sampleCount;

        boolean firstThreadCPULoadEventIsFired;
    }

    private static final int ASYNC_PROFILER_DEFAULT_INTERVAL = 10 * 1000 * 1000;
    private final Map<Long, CpuTaskData> data = new HashMap<>();

    private long period = -1;

    private long threadCPULoadEventId = -1;
    private boolean profiledByJFR = true;

    private int cpuCores;
    private long intervalAsyncProfiler; // unit: nano
    private long intervalJFR; // unit: nano

    private int concurrentGCThreads = -1;
    private int parallelGCThreads = -1;
    private long concurrentGCWallTime = 0;
    private long parallelGCWallTime = 0;
    private long serialGCWallTime = 0;

    private boolean isWallClockEvents = false;


    private static final RecordedThread DUMMY_THREAD = new RecordedThread("Dummy Thread", -1L, -1L);
    private static final RecordedThread GC_THREAD = new RecordedThread("GC Thread", -10L, -10L);

    public CPUTimeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);

        Long id = context.getEventTypeId(EventConstant.THREAD_CPU_LOAD);
        if (id != null) {
            threadCPULoadEventId = id;
        }
    }

    CpuTaskData getThreadData(RecordedThread thread) {
        return data.computeIfAbsent(thread.getJavaThreadId(), i -> new CpuTaskData(thread));
    }

    private void updatePeriod(String value) {
        period = TimeUtil.parseTimespan(value);
    }

    @Override
    void visitUnsignedIntFlag(RecordedEvent event) {
        String name = event.getString("name");
        if ("ConcGCThreads".equals(name)) {
            concurrentGCThreads = event.getInt("value");
        } else if ("ParallelGCThreads".equals(name)) {
            parallelGCThreads = event.getInt("value");
        }
    }

    @Override
    void visitGarbageCollection(RecordedEvent event) {
        String name = event.getString("name");
        long duration = event.getDuration().toNanos();

        if (GCUtil.isParallelGC(name)) {
            parallelGCWallTime += duration;
        } else if (GCUtil.isConcGC(name)) {
            concurrentGCWallTime += duration;
        } else if (GCUtil.isSerialGC(name)) {
            serialGCWallTime += duration;
        }
    }

    @Override
    void visitActiveSetting(RecordedEvent event) {
        if (event.getActiveSetting().eventId() == threadCPULoadEventId
                && EventConstant.PERIOD.equals(event.getString("name"))) {
            updatePeriod(event.getValue("value"));
        }

        if (EventConstant.EVENT.equals(event.getString("name")) && EventConstant.WALL.equals(event.getString("value"))) {
            this.isWallClockEvents = true;
        }

        if (this.context.isExecutionSampleEventTypeId(event.getActiveSetting().eventId())) {
            if (EventConstant.WALL.equals(event.getString("name"))) {
                this.isWallClockEvents = true;
            } else if (EventConstant.INTERVAL.equals(event.getString("name"))) {
                // async-profiler is "interval"
                this.intervalAsyncProfiler = Long.parseLong(event.getString("value"));
                this.profiledByJFR = false;
            } else if (EventConstant.PERIOD.equals(event.getString("name"))) {
                // JFR is "period"
                try {
                    this.intervalJFR = TimeUtil.parseTimespan(event.getString("value"));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    void visitCPUInformation(RecordedEvent event) {
        if (cpuCores == 0) {
            cpuCores = event.getInt("hwThreads");
        }
    }

    @Override
    void visitEnvVar(RecordedEvent event) {
        if ("CPU_COUNT".equals(event.getString("key"))) {
            cpuCores = Integer.parseInt(event.getString("value"));
        }
    }

    @Override
    void visitThreadStart(RecordedEvent event) {
        if (event.getThread() == null) {
            return;
        }
        CpuTaskData cpuTaskData = getThreadData(event.getThread());
        cpuTaskData.start = event.getStartTime();
    }

    @Override
    void visitThreadCPULoad(RecordedEvent event) {
        if (event.getThread() == null) {
            return;
        }
        CpuTaskData cpuTaskData = getThreadData(event.getThread());
        long nanos = period;
        if (!cpuTaskData.firstThreadCPULoadEventIsFired) {
            if (cpuTaskData.start != null) {
                Duration between = Duration.between(cpuTaskData.start, event.getStartTime());
                nanos = Math.min(nanos, between.toNanos());
            }
            cpuTaskData.firstThreadCPULoadEventIsFired = true;
        }
        cpuTaskData.user += (long) (event.getFloat("user") * nanos);
        cpuTaskData.system += (long) (event.getFloat("system") * nanos);
    }

    @Override
    void visitExecutionSample(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            stackTrace = StackTraceUtil.DUMMY_STACK_TRACE;
        }

        RecordedThread thread = event.getThread("eventThread");
        if (thread == null) {
            thread = event.getThread("sampledThread");
        }
        if (thread == null) {
            thread = DUMMY_THREAD;
        }
        CpuTaskData cpuTaskData = getThreadData(thread);

        if (cpuTaskData.getSamples() == null) {
            cpuTaskData.setSamples(new HashMap<>());
        }

        cpuTaskData.getSamples().compute(stackTrace, (k, count) -> count == null ? 1 : count + 1);
        cpuTaskData.sampleCount++;
    }

    private List<TaskCPUTime> buildThreadCPUTime() {
        List<TaskCPUTime> threadCPUTimes = new ArrayList<>();
        if (this.isWallClockEvents) {
            return threadCPUTimes;
        }
        for (CpuTaskData data : this.data.values()) {
            if (data.getSamples() == null) {
                continue;
            }
            JavaThreadCPUTime threadCPUTime = new JavaThreadCPUTime();
            threadCPUTime.setTask(context.getThread(data.getThread()));

            if (data.getSamples() != null) {
                if (this.profiledByJFR) {
                    if (intervalJFR <= 0) {
                        throw new RuntimeException("need profiling interval to calculate approximate CPU time");
                    }
                    long cpuTimeMax = (data.user + data.system) * cpuCores;
                    long sampleTime = data.sampleCount * intervalJFR;
                    if (cpuTimeMax == 0) {
                        threadCPUTime.setUser(sampleTime);
                    } else {
                        threadCPUTime.setUser(Math.min(sampleTime, cpuTimeMax));
                    }
                    threadCPUTime.setSystem(0);
                } else {
                    if (intervalAsyncProfiler <= 0) {
                        intervalAsyncProfiler = detectAsyncProfilerInterval();
                    }
                    threadCPUTime.setUser(data.sampleCount * intervalAsyncProfiler);
                    threadCPUTime.setSystem(0);
                }

                threadCPUTime.setSamples(data.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum)
                ));
            }

            threadCPUTimes.add(threadCPUTime);
        }

        if (this.profiledByJFR) {
            long gcTime = buildGCCpuTime();
            if (gcTime > 0) {
                JavaThreadCPUTime gc = new JavaThreadCPUTime();
                gc.setTask(context.getThread(GC_THREAD));
                gc.setUser(gcTime);
                Map<StackTrace, Long> gcSamples = new HashMap<>();
                gcSamples.put(StackTraceUtil.build(StackTraceUtil.newDummyStackTrace("", "JVM", "GC"), context.getSymbols()), 1L);
                gc.setSamples(gcSamples);
                threadCPUTimes.add(gc);
            }
        }

        threadCPUTimes.sort((o1, o2) -> {
            long delta = o2.totalCPUTime() - o1.totalCPUTime();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });
        return threadCPUTimes;
    }

    private long buildGCCpuTime() {
        if (parallelGCThreads < 0 || concurrentGCThreads < 0) {
            log.warn("invalid ParallelGCThreads or ConcurrentGCThreads, GC cpu time can not be calculated");
            return -1;
        } else {
            return parallelGCThreads * parallelGCWallTime + concurrentGCThreads * concurrentGCWallTime + serialGCWallTime;
        }
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCPUTime> cpuResult = new DimensionResult<>();
        List<TaskCPUTime> list = buildThreadCPUTime();
        cpuResult.setList(list);
        result.setCpuTime(cpuResult);
    }

    private static long detectAsyncProfilerInterval() {
        long interval = 0;
        String intervalStr = System.getProperty("asyncProfilerCpuIntervalMs");
        if (intervalStr != null && !intervalStr.isEmpty()) {
            try {
                interval = Long.parseLong(intervalStr) * 1000 * 1000;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        if (interval <= 0) {
            log.info("use default cpu interval 10ms");
            interval = ASYNC_PROFILER_DEFAULT_INTERVAL;
        }
        return interval;
    }
}

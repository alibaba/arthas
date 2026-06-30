
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;


import java.util.List;

@Setter
@Getter
public class AnalysisResult {
    private long processingTimeMillis;

    private DimensionResult<TaskCPUTime> cpuTime;

    private DimensionResult<TaskCount> cpuSample;

    private DimensionResult<TaskSum> wallClock;

    private DimensionResult<TaskAllocations> allocations;

    private DimensionResult<TaskAllocatedMemory> allocatedMemory;

    private DimensionResult<TaskCount> nativeExecutionSamples;

    private DimensionResult<TaskSum> fileIOTime;

    private DimensionResult<TaskSum> fileReadSize;

    private DimensionResult<TaskSum> fileWriteSize;

    private DimensionResult<TaskSum> socketReadSize;

    private DimensionResult<TaskSum> socketReadTime;

    private DimensionResult<TaskSum> socketWriteSize;

    private DimensionResult<TaskSum> socketWriteTime;

    private DimensionResult<TaskSum> synchronization;

    private DimensionResult<TaskSum> threadPark;

    private DimensionResult<TaskCount> classLoadCount;

    private DimensionResult<TaskSum> classLoadWallTime;

    private DimensionResult<TaskSum> threadSleepTime;

    private List<Problem> problems;
}

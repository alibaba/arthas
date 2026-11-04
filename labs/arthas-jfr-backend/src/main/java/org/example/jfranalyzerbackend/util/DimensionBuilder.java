
package org.example.jfranalyzerbackend.util;


import org.example.jfranalyzerbackend.entity.ProfileDimension;

public class DimensionBuilder {
    public static final int CPU = ProfileDimension.CPU.getValue();
    public static final int CPU_SAMPLE = ProfileDimension.CPU_SAMPLE.getValue();
    public static final int WALL_CLOCK = ProfileDimension.WALL_CLOCK.getValue();
    public static final int NATIVE_EXECUTION_SAMPLES = ProfileDimension.NATIVE_EXECUTION_SAMPLES.getValue();
    public static final int ALLOC = ProfileDimension.ALLOC.getValue();
    public static final int MEM = ProfileDimension.MEM.getValue();
    public static final int FILE_IO_TIME = ProfileDimension.FILE_IO_TIME.getValue();
    public static final int FILE_WRITE_SIZE = ProfileDimension.FILE_WRITE_SIZE.getValue();
    public static final int FILE_READ_SIZE = ProfileDimension.FILE_READ_SIZE.getValue();
    public static final int SOCKET_READ_SIZE = ProfileDimension.SOCKET_READ_SIZE.getValue();
    public static final int SOCKET_READ_TIME = ProfileDimension.SOCKET_READ_TIME.getValue();
    public static final int SOCKET_WRITE_SIZE = ProfileDimension.SOCKET_WRITE_SIZE.getValue();
    public static final int SOCKET_WRITE_TIME = ProfileDimension.SOCKET_WRITE_TIME.getValue();
    public static final int SYNCHRONIZATION = ProfileDimension.SYNCHRONIZATION.getValue();
    public static final int THREAD_PARK = ProfileDimension.THREAD_PARK.getValue();
    public static final int CLASS_LOAD_COUNT = ProfileDimension.CLASS_LOAD_COUNT.getValue();
    public static final int CLASS_LOAD_WALL_TIME = ProfileDimension.CLASS_LOAD_WALL_TIME.getValue();
    public static final int THREAD_SLEEP = ProfileDimension.THREAD_SLEEP.getValue();

    public static final int ALL = CPU | CPU_SAMPLE | WALL_CLOCK | NATIVE_EXECUTION_SAMPLES
            | ALLOC | MEM | FILE_IO_TIME | FILE_WRITE_SIZE | FILE_READ_SIZE | SOCKET_READ_SIZE | SOCKET_WRITE_SIZE
            | SOCKET_READ_TIME | SOCKET_WRITE_TIME | SYNCHRONIZATION | THREAD_PARK
            | CLASS_LOAD_COUNT | CLASS_LOAD_WALL_TIME | THREAD_SLEEP;

    private int dimensions = 0;

    public static DimensionBuilder newInstance() {
        return new DimensionBuilder();
    }

    public DimensionBuilder enableCPU() {
        this.dimensions |= CPU;
        return this;
    }

    public DimensionBuilder enableCPUSample() {
        this.dimensions |= CPU_SAMPLE;
        return this;
    }

    public DimensionBuilder enableWallClock() {
        this.dimensions |= WALL_CLOCK;
        return this;
    }

    public DimensionBuilder enableNative() {
        this.dimensions |= NATIVE_EXECUTION_SAMPLES;
        return this;
    }

    public DimensionBuilder enableAllocCount() {
        this.dimensions |= ALLOC;
        return this;
    }

    public DimensionBuilder enableAllocSize() {
        this.dimensions |= MEM;
        return this;
    }

    public DimensionBuilder enableFileIOTime() {
        this.dimensions |= FILE_IO_TIME;
        return this;
    }

    public DimensionBuilder enableFileWriteSize() {
        this.dimensions |= FILE_WRITE_SIZE;
        return this;
    }

    public DimensionBuilder enableFileReadSize() {
        this.dimensions |= FILE_READ_SIZE;
        return this;
    }

    public DimensionBuilder enableSocketReadTime() {
        this.dimensions |= SOCKET_READ_TIME;
        return this;
    }

    public DimensionBuilder enableSocketReadSize() {
        this.dimensions |= SOCKET_READ_SIZE;
        return this;
    }

    public DimensionBuilder enableSocketWriteSize() {
        this.dimensions |= SOCKET_WRITE_SIZE;
        return this;
    }

    public DimensionBuilder enableSocketWriteTime() {
        this.dimensions |= SOCKET_WRITE_TIME;
        return this;
    }

    public DimensionBuilder enableThreadPark() {
        this.dimensions |= THREAD_PARK;
        return this;
    }

    public DimensionBuilder enableSynchronization() {
        this.dimensions |= SYNCHRONIZATION;
        return this;
    }

    public DimensionBuilder enableClassLoadTime() {
        this.dimensions |= CLASS_LOAD_WALL_TIME;
        return this;
    }

    public DimensionBuilder enableClassLoadCount() {
        this.dimensions |= CLASS_LOAD_COUNT;
        return this;
    }

    public DimensionBuilder enableThreadSleep() {
        this.dimensions |= THREAD_SLEEP;
        return this;
    }

    public DimensionBuilder enableALL() {
        this.dimensions = ALL;
        return this;
    }

    public int build() {
        return this.dimensions;
    }
}

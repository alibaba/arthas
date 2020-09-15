package com.taobao.arthas.core.command.model;

import java.lang.management.ThreadInfo;

/**
 * ThreadInfo with cpuUsage
 *
 * @author gongdewei 2020/4/26
 */
public class BusyThreadInfo {

    private long id;
    private String name;
    private double cpu;
    private long deltaTime;
    private long time;
    private ThreadInfo threadInfo;

    public BusyThreadInfo(long id, String name, double cpu, long deltaTime, long time, ThreadInfo threadInfo) {
        this.id = id;
        this.name = name;
        this.cpu = cpu;
        this.deltaTime = deltaTime;
        this.time = time;
        this.threadInfo = threadInfo;
    }

    public void setThreadInfo(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCpu() {
        return cpu;
    }

    public void setCpu(double cpu) {
        this.cpu = cpu;
    }

    public long getDeltaTime() {
        return deltaTime;
    }

    public void setDeltaTime(long deltaTime) {
        this.deltaTime = deltaTime;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

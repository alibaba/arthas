package com.taobao.arthas.core.command.model;

import java.lang.management.ThreadInfo;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DeadlockInfo {
    private List<ThreadInfo> threads = new LinkedList<>();
    private Map<Integer, ThreadInfo> ownerThreadPerLock;

    public Map<Integer, ThreadInfo> getOwnerThreadPerLock() {
        return ownerThreadPerLock;
    }

    public void setOwnerThreadPerLock(Map<Integer, ThreadInfo> ownerThreadPerLock) {
        this.ownerThreadPerLock = ownerThreadPerLock;
    }

    public List<ThreadInfo> getThreads() {
        return threads;
    }

    public void setThreads(List<ThreadInfo> threads) {
        this.threads = threads;
    }
}

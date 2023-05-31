package com.taobao.arthas.core.command.model;

import java.util.List;
import java.util.Map;

/**
 * Model of 'memory' command
 * @author hengyunabc 2022-03-01
 */
public class MemoryModel extends ResultModel {
    private Map<String, List<MemoryEntryVO>> memoryInfo;

    @Override
    public String getType() {
        return "memory";
    }

    public Map<String, List<MemoryEntryVO>> getMemoryInfo() {
        return memoryInfo;
    }

    public void setMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfo) {
        this.memoryInfo = memoryInfo;
    }
}

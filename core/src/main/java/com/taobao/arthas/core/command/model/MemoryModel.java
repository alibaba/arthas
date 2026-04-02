package com.taobao.arthas.core.command.model;

import java.util.List;
import java.util.Map;

/**
 * 'memory'命令的数据模型
 *
 * 用于封装JVM内存信息的命令执行结果，继承自ResultModel基类
 *
 * @author hengyunabc 2022-03-01
 */
public class MemoryModel extends ResultModel {

    /**
     * 内存信息映射表
     * Key: 内存区域类型（如heap、nonheap、buffer_pool等）
     * Value: 该类型下所有内存区域的信息列表
     */
    private Map<String, List<MemoryEntryVO>> memoryInfo;

    /**
     * 获取命令类型
     * 返回"memory"表示这是memory命令的结果模型
     *
     * @return 命令类型标识符
     */
    @Override
    public String getType() {
        return "memory";
    }

    /**
     * 获取内存信息映射表
     *
     * @return 按内存类型分组的内存信息列表
     */
    public Map<String, List<MemoryEntryVO>> getMemoryInfo() {
        return memoryInfo;
    }

    /**
     * 设置内存信息映射表
     *
     * @param memoryInfo 按内存类型分组的内存信息列表
     */
    public void setMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfo) {
        this.memoryInfo = memoryInfo;
    }
}

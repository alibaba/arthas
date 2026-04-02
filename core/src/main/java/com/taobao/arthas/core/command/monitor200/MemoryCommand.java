package com.taobao.arthas.core.command.monitor200;

import static com.taobao.arthas.core.command.model.MemoryEntryVO.TYPE_BUFFER_POOL;
import static com.taobao.arthas.core.command.model.MemoryEntryVO.TYPE_HEAP;
import static com.taobao.arthas.core.command.model.MemoryEntryVO.TYPE_NON_HEAP;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MemoryEntryVO;
import com.taobao.arthas.core.command.model.MemoryModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * JVM内存信息查看命令
 * 用于显示JVM的堆内存、非堆内存和缓冲池内存的使用情况
 *
 * @author hengyunabc 2022-03-01
 */
@Name("memory")
@Summary("Display jvm memory info.")
@Description(Constants.EXAMPLE + "  memory\n" + Constants.WIKI + Constants.WIKI_HOME + "memory")
public class MemoryCommand extends AnnotatedCommand {
    /**
     * 处理命令执行
     * 获取JVM内存信息并返回结果
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        // 创建内存模型并获取内存信息
        MemoryModel result = new MemoryModel();
        result.setMemoryInfo(memoryInfo());
        process.appendResult(result);
        process.end();
    }

    /**
     * 获取JVM内存信息
     * 包括堆内存、非堆内存和缓冲池内存的详细信息
     * @return 按类型分组的内存信息映射表
     */
    static Map<String, List<MemoryEntryVO>> memoryInfo() {
        // 获取所有内存池的MXBean
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        Map<String, List<MemoryEntryVO>> memoryInfoMap = new LinkedHashMap<String, List<MemoryEntryVO>>();

        // 处理堆内存信息
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        List<MemoryEntryVO> heapMemEntries = new ArrayList<MemoryEntryVO>();
        // 添加堆内存总览
        heapMemEntries.add(createMemoryEntryVO(TYPE_HEAP, TYPE_HEAP, heapMemoryUsage));
        // 遍历所有内存池，添加堆内存相关的内存池
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = getUsage(poolMXBean);
                if (usage != null) {
                    // 美化内存池名称
                    String poolName = StringUtils.beautifyName(poolMXBean.getName());
                    heapMemEntries.add(createMemoryEntryVO(TYPE_HEAP, poolName, usage));
                }
            }
        }
        memoryInfoMap.put(TYPE_HEAP, heapMemEntries);

        // 处理非堆内存信息
        MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        List<MemoryEntryVO> nonheapMemEntries = new ArrayList<MemoryEntryVO>();
        // 添加非堆内存总览
        nonheapMemEntries.add(createMemoryEntryVO(TYPE_NON_HEAP, TYPE_NON_HEAP, nonHeapMemoryUsage));
        // 遍历所有内存池，添加非堆内存相关的内存池
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.NON_HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = getUsage(poolMXBean);
                if (usage != null) {
                    // 美化内存池名称
                    String poolName = StringUtils.beautifyName(poolMXBean.getName());
                    nonheapMemEntries.add(createMemoryEntryVO(TYPE_NON_HEAP, poolName, usage));
                }
            }
        }
        memoryInfoMap.put(TYPE_NON_HEAP, nonheapMemEntries);

        // 添加缓冲池内存信息（如直接缓冲区）
        addBufferPoolMemoryInfo(memoryInfoMap);
        return memoryInfoMap;
    }

    /**
     * 获取内存池的使用情况
     * 处理可能的JVM内部错误
     * @param memoryPoolMXBean 内存池MXBean
     * @return 内存使用情况，如果获取失败则返回null
     */
    private static MemoryUsage getUsage(MemoryPoolMXBean memoryPoolMXBean) {
        try {
            return memoryPoolMXBean.getUsage();
        } catch (InternalError e) {
            // 防御性编程：某些特定的JVM选项可能会抛出InternalError
            // 根据Javadoc，MemoryPoolMXBean.getUsage()应该返回null而不是抛出InternalError
            // 这看起来是JVM的bug
            return null;
        }
    }

    /**
     * 添加缓冲池内存信息
     * 收集直接缓冲区和映射缓冲区的使用情况
     * @param memoryInfoMap 内存信息映射表
     */
    private static void addBufferPoolMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfoMap) {
        try {
            List<MemoryEntryVO> bufferPoolMemEntries = new ArrayList<MemoryEntryVO>();
            // 通过反射获取BufferPoolMXBean类，因为某些Java版本可能不支持
            @SuppressWarnings("rawtypes")
            Class bufferPoolMXBeanClass = Class.forName("java.lang.management.BufferPoolMXBean");
            @SuppressWarnings("unchecked")
            List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(bufferPoolMXBeanClass);
            // 遍历所有缓冲池MXBean
            for (BufferPoolMXBean mbean : bufferPoolMXBeans) {
                long used = mbean.getMemoryUsed();
                long total = mbean.getTotalCapacity();
                // 创建缓冲池内存条目（max值设为Long.MIN_VALUE表示不适用）
                bufferPoolMemEntries
                        .add(new MemoryEntryVO(TYPE_BUFFER_POOL, mbean.getName(), used, total, Long.MIN_VALUE));
            }
            memoryInfoMap.put(TYPE_BUFFER_POOL, bufferPoolMemEntries);
        } catch (ClassNotFoundException e) {
            // 如果BufferPoolMXBean类不存在，则忽略（例如在较旧的Java版本中）
            // ignore
        }
    }

    /**
     * 创建内存条目值对象
     * @param type 内存类型（堆、非堆、缓冲池）
     * @param name 内存区域名称
     * @param memoryUsage 内存使用情况
     * @return 内存条目值对象
     */
    private static MemoryEntryVO createMemoryEntryVO(String type, String name, MemoryUsage memoryUsage) {
        return new MemoryEntryVO(type, name, memoryUsage.getUsed(), memoryUsage.getCommitted(), memoryUsage.getMax());
    }
}
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
 * @author hengyunabc 2022-03-01
 */
@Name("memory")
@Summary("Display jvm memory info.")
@Description(Constants.EXAMPLE + "  memory\n" + Constants.WIKI + Constants.WIKI_HOME + "memory")
public class MemoryCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        MemoryModel result = new MemoryModel();
        result.setMemoryInfo(memoryInfo());
        process.appendResult(result);
        process.end();
    }

    static Map<String, List<MemoryEntryVO>> memoryInfo() {
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        Map<String, List<MemoryEntryVO>> memoryInfoMap = new LinkedHashMap<String, List<MemoryEntryVO>>();

        // heap
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        List<MemoryEntryVO> heapMemEntries = new ArrayList<MemoryEntryVO>();
        heapMemEntries.add(createMemoryEntryVO(TYPE_HEAP, TYPE_HEAP, heapMemoryUsage));
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = getUsage(poolMXBean);
                if (usage != null) {
                    String poolName = StringUtils.beautifyName(poolMXBean.getName());
                    heapMemEntries.add(createMemoryEntryVO(TYPE_HEAP, poolName, usage));
                }
            }
        }
        memoryInfoMap.put(TYPE_HEAP, heapMemEntries);

        // non-heap
        MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        List<MemoryEntryVO> nonheapMemEntries = new ArrayList<MemoryEntryVO>();
        nonheapMemEntries.add(createMemoryEntryVO(TYPE_NON_HEAP, TYPE_NON_HEAP, nonHeapMemoryUsage));
        for (MemoryPoolMXBean poolMXBean : memoryPoolMXBeans) {
            if (MemoryType.NON_HEAP.equals(poolMXBean.getType())) {
                MemoryUsage usage = getUsage(poolMXBean);
                if (usage != null) {
                    String poolName = StringUtils.beautifyName(poolMXBean.getName());
                    nonheapMemEntries.add(createMemoryEntryVO(TYPE_NON_HEAP, poolName, usage));
                }
            }
        }
        memoryInfoMap.put(TYPE_NON_HEAP, nonheapMemEntries);

        addBufferPoolMemoryInfo(memoryInfoMap);
        return memoryInfoMap;
    }

    private static MemoryUsage getUsage(MemoryPoolMXBean memoryPoolMXBean) {
        try {
            return memoryPoolMXBean.getUsage();
        } catch (InternalError e) {
            // Defensive for potential InternalError with some specific JVM options. Based on its Javadoc,
            // MemoryPoolMXBean.getUsage() should return null, not throwing InternalError, so it seems to be a JVM bug.
            return null;
        }
    }

    private static void addBufferPoolMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfoMap) {
        try {
            List<MemoryEntryVO> bufferPoolMemEntries = new ArrayList<MemoryEntryVO>();
            @SuppressWarnings("rawtypes")
            Class bufferPoolMXBeanClass = Class.forName("java.lang.management.BufferPoolMXBean");
            @SuppressWarnings("unchecked")
            List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(bufferPoolMXBeanClass);
            for (BufferPoolMXBean mbean : bufferPoolMXBeans) {
                long used = mbean.getMemoryUsed();
                long total = mbean.getTotalCapacity();
                bufferPoolMemEntries
                        .add(new MemoryEntryVO(TYPE_BUFFER_POOL, mbean.getName(), used, total, Long.MIN_VALUE));
            }
            memoryInfoMap.put(TYPE_BUFFER_POOL, bufferPoolMemEntries);
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    private static MemoryEntryVO createMemoryEntryVO(String type, String name, MemoryUsage memoryUsage) {
        return new MemoryEntryVO(type, name, memoryUsage.getUsed(), memoryUsage.getCommitted(), memoryUsage.getMax());
    }
}
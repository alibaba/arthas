package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.model.ThreadVO;
import sun.management.HotspotThreadMBean;
import sun.management.ManagementFactoryHelper;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线程CPU采样器
 * <p>
 * 用于定期采样线程的CPU使用情况，支持普通Java线程和JVM内部线程的CPU时间统计。
 * 可以计算线程的CPU使用率和增量时间。
 * </p>
 *
 * @author gongdewei 2020/4/23
 */
public class ThreadSampler {

    // JDK标准的线程管理Bean，用于获取线程的CPU时间
    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    // HotSpot VM特有的线程管理Bean，用于获取JVM内部线程的CPU时间
    private static HotspotThreadMBean hotspotThreadMBean;
    // HotSpot ThreadMBean是否可用的标志，首次获取失败后会设置为false，避免重复尝试
    private static boolean hotspotThreadMBeanEnable = true;

    // 存储上次采样时各线程的CPU时间（纳秒），用于计算增量
    private Map<ThreadVO, Long> lastCpuTimes = new HashMap<ThreadVO, Long>();

    // 上次采样的时间戳（纳秒）
    private long lastSampleTimeNanos;
    // 是否包含JVM内部线程的标志
    private boolean includeInternalThreads = true;


    /**
     * 对线程集合进行CPU采样
     * <p>
     * 首次采样时，会记录所有线程的初始CPU时间，并添加JVM内部线程（如果启用）。
     * 后续采样时，会计算两次采样之间的CPU时间增量，并计算CPU使用率。
     * </p>
     *
     * @param originThreads 原始线程集合
     * @return 采样后的线程列表，按CPU时间降序排列
     */
    public List<ThreadVO> sample(Collection<ThreadVO> originThreads) {

        // 创建新的线程列表副本，避免修改原集合
        List<ThreadVO> threads = new ArrayList<ThreadVO>(originThreads);

        // 首次采样：记录所有线程的初始CPU时间
        if (lastCpuTimes.isEmpty()) {
            // 记录采样时间戳
            lastSampleTimeNanos = System.nanoTime();
            // 遍历所有普通Java线程
            for (ThreadVO thread : threads) {
                if (thread.getId() > 0) {
                    // 获取线程的累计CPU时间（纳秒）
                    long cpu = threadMXBean.getThreadCpuTime(thread.getId());
                    // 保存到上次采样记录中
                    lastCpuTimes.put(thread, cpu);
                    // 转换为毫秒并设置到线程对象中
                    thread.setTime(cpu / 1000000);
                }
            }

            // 添加JVM内部线程（如编译器线程、GC线程等）
            Map<String, Long> internalThreadCpuTimes = getInternalThreadCpuTimes();
            if (internalThreadCpuTimes != null) {
                for (Map.Entry<String, Long> entry : internalThreadCpuTimes.entrySet()) {
                    String key = entry.getKey();
                    // 为内部线程创建线程对象
                    ThreadVO thread = createThreadVO(key);
                    // 设置CPU时间（毫秒）
                    thread.setTime(entry.getValue() / 1000000);
                    // 添加到线程列表
                    threads.add(thread);
                    // 记录到上次采样记录中
                    lastCpuTimes.put(thread, entry.getValue());
                }
            }

            // 按CPU时间降序排序（时间长的排前面）
            Collections.sort(threads, new Comparator<ThreadVO>() {
                @Override
                public int compare(ThreadVO o1, ThreadVO o2) {
                    long l1 = o1.getTime();
                    long l2 = o2.getTime();
                    if (l1 < l2) {
                        return 1;
                    } else if (l1 > l2) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
            return threads;
        }

        // 重新采样：计算两次采样之间的CPU时间增量
        long newSampleTimeNanos = System.nanoTime();
        // 存储当前采样时各线程的CPU时间
        Map<ThreadVO, Long> newCpuTimes = new HashMap<ThreadVO, Long>(threads.size());
        // 采集所有普通Java线程的CPU时间
        for (ThreadVO thread : threads) {
            if (thread.getId() > 0) {
                long cpu = threadMXBean.getThreadCpuTime(thread.getId());
                newCpuTimes.put(thread, cpu);
            }
        }
        // 采集JVM内部线程的CPU时间
        Map<String, Long> newInternalThreadCpuTimes = getInternalThreadCpuTimes();
        if (newInternalThreadCpuTimes != null) {
            for (Map.Entry<String, Long> entry : newInternalThreadCpuTimes.entrySet()) {
                ThreadVO threadVO = createThreadVO(entry.getKey());
                threads.add(threadVO);
                newCpuTimes.put(threadVO, entry.getValue());
            }
        }

        // 计算CPU时间增量（本次采样时间 - 上次采样时间）
        final Map<ThreadVO, Long> deltas = new HashMap<ThreadVO, Long>(threads.size());
        for (ThreadVO thread : newCpuTimes.keySet()) {
            // 获取该线程上次的CPU时间
            Long t = lastCpuTimes.get(thread);
            if (t == null) {
                // 新线程，上次时间记为0
                t = 0L;
            }
            long time1 = t;
            long time2 = newCpuTimes.get(thread);
            // 处理-1的情况（线程已终止或CPU时间不支持）
            if (time1 == -1) {
                time1 = time2;
            } else if (time2 == -1) {
                time2 = time1;
            }
            // 计算时间增量
            long delta = time2 - time1;
            deltas.put(thread, delta);
        }

        // 计算采样间隔（纳秒）
        long sampleIntervalNanos = newSampleTimeNanos - lastSampleTimeNanos;

        // 计算CPU使用率（百分比）
        final HashMap<ThreadVO, Double> cpuUsages = new HashMap<ThreadVO, Double>(threads.size());
        for (ThreadVO thread : threads) {
            // CPU使用率 = (CPU增量 / 采样间隔) * 100
            // 使用Math.rint进行四舍五入，保留2位小数
            double cpu = sampleIntervalNanos == 0 ? 0 : (Math.rint(deltas.get(thread) * 10000.0 / sampleIntervalNanos) / 100.0);
            cpuUsages.put(thread, cpu);
        }

        // 按CPU时间增量降序排序（增量大的排前面）
        Collections.sort(threads, new Comparator<ThreadVO>() {
            @Override
            public int compare(ThreadVO o1, ThreadVO o2) {
                long l1 = deltas.get(o1);
                long l2 = deltas.get(o2);
                if (l1 < l2) {
                    return 1;
                } else if (l1 > l2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        // 更新线程对象的各项属性
        for (ThreadVO thread : threads) {
            // 将纳秒转换为毫秒
            long timeMills = newCpuTimes.get(thread) / 1000000;
            // 计算增量时间的毫秒数
            long deltaTime = deltas.get(thread) / 1000000;
            double cpu = cpuUsages.get(thread);

            // 设置CPU使用率
            thread.setCpu(cpu);
            // 设置累计CPU时间（毫秒）
            thread.setTime(timeMills);
            // 设置增量CPU时间（毫秒）
            thread.setDeltaTime(deltaTime);
        }
        // 更新上次采样记录
        lastCpuTimes = newCpuTimes;
        lastSampleTimeNanos = newSampleTimeNanos;

        return threads;
    }

    /**
     * 获取JVM内部线程的CPU时间
     * <p>
     * JVM内部线程包括编译器线程、GC线程等，这些线程不对应Java的Thread对象。
     * 使用HotSpot特有的HotspotThreadMBean来获取这些线程的CPU时间。
     * </p>
     *
     * @return 内部线程名称到CPU时间的映射，如果获取失败则返回null
     */
    private Map<String, Long> getInternalThreadCpuTimes() {
        // 只有在HotSpot ThreadMBean可用且启用内部线程时才尝试获取
        if (hotspotThreadMBeanEnable && includeInternalThreads) {
            try {
                // 延迟初始化HotspotThreadMBean
                if (hotspotThreadMBean == null) {
                    hotspotThreadMBean = ManagementFactoryHelper.getHotspotThreadMBean();
                }
                // 返回内部线程的CPU时间映射
                return hotspotThreadMBean.getInternalThreadCpuTimes();
            } catch (Throwable e) {
                // 获取失败（可能不是HotSpot VM），标记为不可用，避免重复尝试
                hotspotThreadMBeanEnable = false;
            }
        }
        return null;
    }

    /**
     * 为JVM内部线程创建ThreadVO对象
     * <p>
     * 内部线程没有对应的Java Thread对象，因此需要手动创建ThreadVO对象。
     * 使用特殊值（-1）来标识这些内部线程。
     * </p>
     *
     * @param name 内部线程的名称
     * @return 创建的ThreadVO对象
     */
    private ThreadVO createThreadVO(String name) {
        ThreadVO threadVO = new ThreadVO();
        // 使用-1标识内部线程（普通Java线程的ID都大于0）
        threadVO.setId(-1);
        // 设置线程名称
        threadVO.setName(name);
        // 优先级设置为-1（无意义）
        threadVO.setPriority(-1);
        // 标记为守护线程
        threadVO.setDaemon(true);
        // 中断状态设置为false
        threadVO.setInterrupted(false);
        return threadVO;
    }

    /**
     * 暂停指定毫秒数
     * <p>
     * 用于在两次采样之间等待，避免过于频繁的采样。
     * </p>
     *
     * @param mills 暂停的毫秒数
     */
    public void pause(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            // 忽略中断异常
        }
    }

    /**
     * 获取是否包含JVM内部线程
     *
     * @return 如果包含内部线程返回true，否则返回false
     */
    public boolean isIncludeInternalThreads() {
        return includeInternalThreads;
    }

    /**
     * 设置是否包含JVM内部线程
     *
     * @param includeInternalThreads true表示包含内部线程，false表示不包含
     */
    public void setIncludeInternalThreads(boolean includeInternalThreads) {
        this.includeInternalThreads = includeInternalThreads;
    }

}

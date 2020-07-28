package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.model.ThreadVO;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Thread cpu sampler
 *
 * @author gongdewei 2020/4/23
 */
public class ThreadSampler {

    private long sampleInterval = 100;

    public List<ThreadVO> sample(Collection<Thread> originThreads) {

        List<Thread> threads = new ArrayList<Thread>(originThreads);
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        // Sample CPU
        Map<Long, Long> times1 = new HashMap<Long, Long>();
        for (Thread thread : threads) {
            long cpu = threadMXBean.getThreadCpuTime(thread.getId());
            times1.put(thread.getId(), cpu);
        }

        try {
            Thread.sleep(sampleInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Resample
        Map<Long, Long> times2 = new HashMap<Long, Long>(threads.size());
        for (Thread thread : threads) {
            long cpu = threadMXBean.getThreadCpuTime(thread.getId());
            times2.put(thread.getId(), cpu);
        }

        // Compute delta map and total time
        long total = 0;
        Map<Long, Long> deltas = new HashMap<Long, Long>(threads.size());
        for (Long id : times2.keySet()) {
            long time1 = times2.get(id);
            long time2 = times1.get(id);
            if (time1 == -1) {
                time1 = time2;
            } else if (time2 == -1) {
                time2 = time1;
            }
            long delta = time2 - time1;
            deltas.put(id, delta);
            total += delta;
        }

        // Compute cpu
        final HashMap<Thread, Long> cpus = new HashMap<Thread, Long>(threads.size());
        for (Thread thread : threads) {
            long cpu = total == 0 ? 0 : Math.round((deltas.get(thread.getId()) * 100) / total);
            cpus.put(thread, cpu);
        }

        // Sort by CPU time : should be a rendering hint...
        Collections.sort(threads, new Comparator<Thread>() {
            public int compare(Thread o1, Thread o2) {
                long l1 = cpus.get(o1);
                long l2 = cpus.get(o2);
                if (l1 < l2) {
                    return 1;
                } else if (l1 > l2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        List<ThreadVO> threadVOList = new ArrayList<ThreadVO>(threads.size());
        for (Thread thread : threads) {
            ThreadGroup group = thread.getThreadGroup();
            long seconds = times2.get(thread.getId()) / 1000000000;
            //long min = seconds / 60;
            //String time = min + ":" + (seconds % 60);
            long cpu = cpus.get(thread);

            ThreadVO threadVO = new ThreadVO();
            threadVO.setId(thread.getId());
            threadVO.setName(thread.getName());
            threadVO.setGroup(group == null ? "" : group.getName());
            threadVO.setPriority(thread.getPriority());
            threadVO.setState(thread.getState());
            threadVO.setCpu(cpu);
            threadVO.setTime(seconds);
            threadVO.setInterrupted(thread.isInterrupted());
            threadVO.setDaemon(thread.isDaemon());
            threadVOList.add(threadVO);
        }
        return threadVOList;
    }

    public long getSampleInterval() {
        return sampleInterval;
    }

    public void setSampleInterval(long sampleInterval) {
        this.sampleInterval = sampleInterval;
    }
}

package com.taobao.arthas.core.util;

import com.taobao.arthas.core.command.model.BlockingLockInfo;
import com.taobao.arthas.core.command.model.StackModel;
import com.taobao.arthas.core.command.model.ThreadNode;
import com.taobao.arthas.core.view.Ansi;

import java.arthas.SpyAPI;
import java.lang.management.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 
 * @author hengyunabc 2015年12月7日 下午2:29:28
 *
 */
abstract public class ThreadUtil {

    private static final BlockingLockInfo EMPTY_INFO = new BlockingLockInfo();

    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private static boolean detectedEagleEye = false;
    public static boolean foundEagleEye = false;

    public static ThreadGroup getRoot() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = group.getParent()) != null) {
            group = parent;
        }
        return group;
    }

    /**
     * 获取所有线程Map，以线程Name-ID为key
     * 
     * @return
     */
    public static Map<String, Thread> getThreads() {
        ThreadGroup root = getRoot();
        Thread[] threads = new Thread[root.activeCount()];
        while (root.enumerate(threads, true) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        SortedMap<String, Thread> map = new TreeMap<String, Thread>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        for (Thread thread : threads) {
            if (thread != null) {
                map.put(thread.getName() + "-" + thread.getId(), thread);
            }
        }
        return map;
    }

    /**
     * 获取所有线程List
     * 
     * @return
     */
    public static List<Thread> getThreadList() {
        List<Thread> result = new ArrayList<Thread>();
        ThreadGroup root = getRoot();
        Thread[] threads = new Thread[root.activeCount()];
        while (root.enumerate(threads, true) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        for (Thread thread : threads) {
            if (thread != null) {
                result.add(thread);
            }
        }
        return result;
    }

    /**
     * get the top N busy thread
     * @param sampleInterval the interval between two samples
     * @param topN the number of thread
     * @return a Map representing <ThreadID, cpuUsage>
     */
    public static Map<Long, Long> getTopNThreads(int sampleInterval, int topN) {
        List<Thread> threads = getThreadList();

        // Sample CPU
        Map<Long, Long> times1 = new HashMap<Long, Long>();
        for (Thread thread : threads) {
            long cpu = threadMXBean.getThreadCpuTime(thread.getId());
            times1.put(thread.getId(), cpu);
        }

        try {
            // Sleep for some time
            Thread.sleep(sampleInterval);
        }
        catch (InterruptedException e) {
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

        // use LinkedHashMap to preserve insert order
        Map<Long, Long> topNThreads = new LinkedHashMap<Long, Long>();

        List<Thread> topThreads = topN > 0 && topN <= threads.size()
                ? threads.subList(0, topN) : threads;

        for (Thread thread: topThreads) {
            // Compute cpu usage
            topNThreads.put(thread.getId(), cpus.get(thread));
        }

        return topNThreads;
    }


    /**
     * Find the thread and lock that is blocking the most other threads.
     *
     * Time complexity of this algorithm: O(number of thread)
     * Space complexity of this algorithm: O(number of locks)
     *
     * @return the BlockingLockInfo object, or an empty object if not found.
     */
    public static BlockingLockInfo findMostBlockingLock() {
        ThreadInfo[] infos = threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(),
                threadMXBean.isSynchronizerUsageSupported());

        // a map of <LockInfo.getIdentityHashCode, number of thread blocking on this>
        Map<Integer, Integer> blockCountPerLock = new HashMap<Integer, Integer>();
        // a map of <LockInfo.getIdentityHashCode, the thread info that holding this lock
        Map<Integer, ThreadInfo> ownerThreadPerLock = new HashMap<Integer, ThreadInfo>();

        for (ThreadInfo info: infos) {
            if (info == null) {
                continue;
            }

            LockInfo lockInfo = info.getLockInfo();
            if (lockInfo != null) {
                // the current thread is blocked waiting on some condition
                if (blockCountPerLock.get(lockInfo.getIdentityHashCode()) == null) {
                    blockCountPerLock.put(lockInfo.getIdentityHashCode(), 0);
                }
                int blockedCount = blockCountPerLock.get(lockInfo.getIdentityHashCode());
                blockCountPerLock.put(lockInfo.getIdentityHashCode(), blockedCount + 1);
            }

            for (MonitorInfo monitorInfo: info.getLockedMonitors()) {
                // the object monitor currently held by this thread
                if (ownerThreadPerLock.get(monitorInfo.getIdentityHashCode()) == null) {
                    ownerThreadPerLock.put(monitorInfo.getIdentityHashCode(), info);
                }
            }

            for (LockInfo lockedSync: info.getLockedSynchronizers()) {
                // the ownable synchronizer currently held by this thread
                if (ownerThreadPerLock.get(lockedSync.getIdentityHashCode()) == null) {
                    ownerThreadPerLock.put(lockedSync.getIdentityHashCode(), info);
                }
            }
        }

        // find the thread that is holding the lock that blocking the largest number of threads.
        int mostBlockingLock = 0; // System.identityHashCode(null) == 0
        int maxBlockingCount = 0;
        for (Map.Entry<Integer, Integer> entry: blockCountPerLock.entrySet()) {
            if (entry.getValue() > maxBlockingCount && ownerThreadPerLock.get(entry.getKey()) != null) {
                // the lock is explicitly held by anther thread.
                maxBlockingCount = entry.getValue();
                mostBlockingLock = entry.getKey();
            }
        }

        if (mostBlockingLock == 0) {
            // nothing found
            return EMPTY_INFO;
        }

        BlockingLockInfo blockingLockInfo = new BlockingLockInfo();
        blockingLockInfo.setThreadInfo(ownerThreadPerLock.get(mostBlockingLock));
        blockingLockInfo.setLockIdentityHashCode(mostBlockingLock);
        blockingLockInfo.setBlockingThreadCount(blockCountPerLock.get(mostBlockingLock));
        return blockingLockInfo;
    }


    public static String getFullStacktrace(ThreadInfo threadInfo, long cpuUsage) {
        return getFullStacktrace(threadInfo, cpuUsage, 0, 0);
    }


    public static String getFullStacktrace(BlockingLockInfo blockingLockInfo) {
        return getFullStacktrace(blockingLockInfo.getThreadInfo(), -1, blockingLockInfo.getLockIdentityHashCode(),
                blockingLockInfo.getBlockingThreadCount());
    }


    /**
     * 完全从 ThreadInfo 中 copy 过来
     * @param threadInfo the thread info object
     * @param cpuUsage will be ignore if cpuUsage < 0 or cpuUsage > 100
     * @param lockIdentityHashCode 阻塞了其他线程的锁的identityHashCode
     * @param blockingThreadCount 阻塞了其他线程的数量
     * @return the string representation of the thread stack
     */
    public static String getFullStacktrace(ThreadInfo threadInfo, long cpuUsage, int lockIdentityHashCode,
                                           int blockingThreadCount) {
        StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" + " Id="
                + threadInfo.getThreadId());

        if (cpuUsage >= 0 && cpuUsage <= 100) {
            sb.append(" cpuUsage=").append(cpuUsage).append("%");
        }

        sb.append(" ").append(threadInfo.getThreadState());

        if (threadInfo.getLockName() != null) {
            sb.append(" on ").append(threadInfo.getLockName());
        }
        if (threadInfo.getLockOwnerName() != null) {
            sb.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\" Id=").append(threadInfo.getLockOwnerId());
        }
        if (threadInfo.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (threadInfo.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');
        int i = 0;
        for (; i < threadInfo.getStackTrace().length; i++) {
            StackTraceElement ste = threadInfo.getStackTrace()[i];
            sb.append("\tat ").append(ste.toString());
            sb.append('\n');
            if (i == 0 && threadInfo.getLockInfo() != null) {
                Thread.State ts = threadInfo.getThreadState();
                switch (ts) {
                    case BLOCKED:
                        sb.append("\t-  blocked on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                        break;
                    case WAITING:
                        sb.append("\t-  waiting on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                        break;
                    case TIMED_WAITING:
                        sb.append("\t-  waiting on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                        break;
                    default:
                }
            }

            for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked ").append(mi);
                    if (mi.getIdentityHashCode() == lockIdentityHashCode) {
                        Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);
                        highlighted.a(" <---- but blocks ").a(blockingThreadCount).a(" other threads!");
                        sb.append(highlighted.reset().toString());
                    }
                    sb.append('\n');
                }
            }
        }
        if (i < threadInfo.getStackTrace().length) {
            sb.append("\t...");
            sb.append('\n');
        }

        LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ").append(locks.length);
            sb.append('\n');
            for (LockInfo li : locks) {
                sb.append("\t- ").append(li);
                if (li.getIdentityHashCode() == lockIdentityHashCode) {
                    sb.append(" <---- but blocks ").append(blockingThreadCount);
                    sb.append(" other threads!");
                }
                sb.append('\n');
            }
        }
        sb.append('\n');
        return sb.toString().replace("\t", "    ");
    }

    /**
     * </pre>
     * java.lang.Thread.getStackTrace(Thread.java:1559),
     * com.taobao.arthas.core.util.ThreadUtil.getThreadStack(ThreadUtil.java:349),
     * com.taobao.arthas.core.command.monitor200.StackAdviceListener.before(StackAdviceListener.java:33),
     * com.taobao.arthas.core.advisor.AdviceListenerAdapter.before(AdviceListenerAdapter.java:49),
     * com.taobao.arthas.core.advisor.SpyImpl.atEnter(SpyImpl.java:42),
     * java.arthas.SpyAPI.atEnter(SpyAPI.java:40),
     * demo.MathGame.print(MathGame.java), demo.MathGame.run(MathGame.java:25),
     * demo.MathGame.main(MathGame.java:16)
     * </pre>
     */
    private static int MAGIC_STACK_DEPTH = 0;

    private static int findTheSpyAPIDepth(StackTraceElement[] stackTraceElementArray) {
        if (MAGIC_STACK_DEPTH > 0) {
            return MAGIC_STACK_DEPTH;
        }
        if (MAGIC_STACK_DEPTH > stackTraceElementArray.length) {
            return 0;
        }
        for (int i = 0; i < stackTraceElementArray.length; ++i) {
            if (SpyAPI.class.getName().equals(stackTraceElementArray[i].getClassName())) {
                MAGIC_STACK_DEPTH = i + 1;
                break;
            }
        }
        return MAGIC_STACK_DEPTH;
    }

    /**
     * 获取方法执行堆栈信息
     *
     * @return 方法堆栈信息
     */
    public static StackModel getThreadStackModel(ClassLoader loader, Thread currentThread) {
        StackModel stackModel = new StackModel();
        stackModel.setThreadName(currentThread.getName());
        stackModel.setThreadId(Long.toHexString(currentThread.getId()));
        stackModel.setDaemon(currentThread.isDaemon());
        stackModel.setPriority(currentThread.getPriority());
        stackModel.setClassloader(getTCCL(currentThread));

        getEagleeyeTraceInfo(loader, currentThread, stackModel);


        //stack
        StackTraceElement[] stackTraceElementArray = currentThread.getStackTrace();
        int magicStackDepth = findTheSpyAPIDepth(stackTraceElementArray);
        StackTraceElement[] actualStackFrames = new StackTraceElement[stackTraceElementArray.length - magicStackDepth];
        System.arraycopy(stackTraceElementArray, magicStackDepth , actualStackFrames, 0, actualStackFrames.length);
        stackModel.setStackTrace(actualStackFrames);
        return stackModel;
    }

    public static ThreadNode getThreadNode(ClassLoader loader, Thread currentThread) {
        ThreadNode threadNode = new ThreadNode();
        threadNode.setThreadId(currentThread.getId());
        threadNode.setThreadName(currentThread.getName());
        threadNode.setDaemon(currentThread.isDaemon());
        threadNode.setPriority(currentThread.getPriority());
        threadNode.setClassloader(getTCCL(currentThread));

        //trace_id
        StackModel stackModel = new StackModel();
        getEagleeyeTraceInfo(loader, currentThread, stackModel);
        threadNode.setTraceId(stackModel.getTraceId());
        threadNode.setRpcId(stackModel.getRpcId());
        return threadNode;
    }

    public static String getThreadTitle(StackModel stackModel) {
        StringBuilder sb = new StringBuilder("thread_name=");
        sb.append(stackModel.getThreadName())
                .append(";id=").append(stackModel.getThreadId())
                .append(";is_daemon=").append(stackModel.isDaemon())
                .append(";priority=").append(stackModel.getPriority())
                .append(";TCCL=").append(stackModel.getClassloader());
        if (stackModel.getTraceId() != null) {
            sb.append(";trace_id=").append(stackModel.getTraceId());
        }
        if (stackModel.getRpcId() != null) {
            sb.append(";rpc_id=").append(stackModel.getRpcId());
        }
        return sb.toString();
    }

    private static String getTCCL(Thread currentThread) {
        if (null == currentThread.getContextClassLoader()) {
            return "null";
        } else {
            String classloaderClassName = currentThread.getContextClassLoader().getClass().getName();
            StringBuilder sb = new StringBuilder(classloaderClassName.length()+10);
            sb.append(classloaderClassName)
                    .append("@")
                    .append(Integer.toHexString(currentThread.getContextClassLoader().hashCode()));
            return  sb.toString();
        }
    }

    private static void getEagleeyeTraceInfo(ClassLoader loader, Thread currentThread, StackModel stackModel) {
        if(loader == null) {
            return;
        }
        Class<?> eagleEyeClass = null;
        if (!detectedEagleEye) {
            try {
                eagleEyeClass = loader.loadClass("com.taobao.eagleeye.EagleEye");
                foundEagleEye = true;
            } catch (Throwable e) {
                // ignore
            }
            detectedEagleEye = true;
        }

        if (!foundEagleEye) {
            return;
        }

        try {
            if (eagleEyeClass == null) {
                eagleEyeClass = loader.loadClass("com.taobao.eagleeye.EagleEye");
            }
            Method getTraceIdMethod = eagleEyeClass.getMethod("getTraceId");
            String traceId = (String) getTraceIdMethod.invoke(null);
            stackModel.setTraceId(traceId);
            Method getRpcIdMethod = eagleEyeClass.getMethod("getRpcId");
            String rpcId = (String) getRpcIdMethod.invoke(null);
            stackModel.setRpcId(rpcId);
        } catch (Throwable e) {
            // ignore
        }
    }

}

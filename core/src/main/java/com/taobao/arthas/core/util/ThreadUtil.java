package com.taobao.arthas.core.util;

import com.taobao.arthas.core.command.model.BlockingLockInfo;
import com.taobao.arthas.core.command.model.BusyThreadInfo;
import com.taobao.arthas.core.command.model.StackModel;
import com.taobao.arthas.core.command.model.ThreadNode;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.view.Ansi;

import java.arthas.SpyAPI;
import java.lang.management.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 线程工具类
 * <p>
 * 提供线程相关的工具方法，包括：
 * <ul>
 * <li>获取线程信息和线程列表</li>
 * <li>查找阻塞其他线程最多的锁</li>
 * <li>生成线程堆栈信息</li>
 * <li>获取线程堆栈模型和节点信息</li>
 * <li>集成EagleEye链路追踪信息</li>
 * </ul>
 * </p>
 *
 * @author hengyunabc 2015年12月7日 下午2:29:28
 */
abstract public class ThreadUtil {

    /**
     * 空的阻塞锁信息对象，用于当没有找到阻塞锁时返回
     */
    private static final BlockingLockInfo EMPTY_INFO = new BlockingLockInfo();

    /**
     * 线程管理Bean，用于获取线程相关信息
     */
    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 是否已经检测过EagleEye类
     * EagleEye是阿里巴巴的链路追踪系统
     */
    private static boolean detectedEagleEye = false;

    /**
     * 是否发现了EagleEye类
     */
    public static boolean foundEagleEye = false;

    /**
     * 获取根线程组
     * <p>
     * 通过不断向上获取父线程组，直到没有父线程组为止，
     * 从而获取到线程组的根节点。根线程组包含所有线程。
     * </p>
     *
     * @return 根线程组
     */
    public static ThreadGroup getRoot() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = group.getParent()) != null) {
            group = parent;
        }
        return group;
    }

    /**
     * 获取所有线程
     */
    public static List<ThreadVO> getThreads() {
        ThreadGroup root = getRoot();
        Thread[] threads = new Thread[root.activeCount()];
        while (root.enumerate(threads, true) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        List<ThreadVO> list = new ArrayList<ThreadVO>(threads.length);
        for (Thread thread : threads) {
            if (thread != null) {
                ThreadVO threadVO = createThreadVO(thread);
                list.add(threadVO);
            }
        }
        return list;
    }

    /**
     * 创建线程视图对象
     * <p>
     * 将Thread对象转换为ThreadVO对象，包含线程的详细信息。
     * </p>
     *
     * @param thread 线程对象
     * @return 线程视图对象
     */
    private static ThreadVO createThreadVO(Thread thread) {
        ThreadGroup group = thread.getThreadGroup();
        ThreadVO threadVO = new ThreadVO();
        threadVO.setId(thread.getId());
        threadVO.setName(thread.getName());
        threadVO.setGroup(group == null ? "" : group.getName());
        threadVO.setPriority(thread.getPriority());
        threadVO.setState(thread.getState());
        threadVO.setInterrupted(thread.isInterrupted());
        threadVO.setDaemon(thread.isDaemon());
        return threadVO;
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
     * 查找阻塞最多其他线程的线程和锁
     * <p>
     * 该算法用于找出当前JVM中阻塞了最多其他线程的那个锁以及持有该锁的线程。
     * 这对于诊断线程死锁和性能问题非常有用。
     * </p>
     *
     * 时间复杂度：O(线程数量)
     * 空间复杂度：O(锁数量)
     *
     * @return BlockingLockInfo对象，包含阻塞锁的信息；如果未找到则返回空对象
     */
    public static BlockingLockInfo findMostBlockingLock() {
        // 获取所有线程的详细信息，包括对象监视器和同步器的使用情况
        ThreadInfo[] infos = threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(),
                threadMXBean.isSynchronizerUsageSupported());

        // 锁的哈希码 -> 阻塞在该锁上的线程数量
        // 用于统计每个锁阻塞了多少线程
        Map<Integer, Integer> blockCountPerLock = new HashMap<Integer, Integer>();
        // 锁的哈希码 -> 持有该锁的线程信息
        // 用于记录哪个线程持有了哪个锁
        Map<Integer, ThreadInfo> ownerThreadPerLock = new HashMap<Integer, ThreadInfo>();

        // 遍历所有线程信息，统计锁的使用情况
        for (ThreadInfo info: infos) {
            if (info == null) {
                continue;
            }

            LockInfo lockInfo = info.getLockInfo();
            if (lockInfo != null) {
                // 当前线程被阻塞，正在等待某个锁或条件
                // 统计该锁被多少线程等待
                if (blockCountPerLock.get(lockInfo.getIdentityHashCode()) == null) {
                    blockCountPerLock.put(lockInfo.getIdentityHashCode(), 0);
                }
                int blockedCount = blockCountPerLock.get(lockInfo.getIdentityHashCode());
                blockCountPerLock.put(lockInfo.getIdentityHashCode(), blockedCount + 1);
            }

            // 遍历当前线程持有的所有对象监视器（synchronized同步块）
            for (MonitorInfo monitorInfo: info.getLockedMonitors()) {
                // 记录该监视器由哪个线程持有
                if (ownerThreadPerLock.get(monitorInfo.getIdentityHashCode()) == null) {
                    ownerThreadPerLock.put(monitorInfo.getIdentityHashCode(), info);
                }
            }

            // 遍历当前线程持有的所有可拥有同步器（如ReentrantLock等）
            for (LockInfo lockedSync: info.getLockedSynchronizers()) {
                // 记录该同步器由哪个线程持有
                if (ownerThreadPerLock.get(lockedSync.getIdentityHashCode()) == null) {
                    ownerThreadPerLock.put(lockedSync.getIdentityHashCode(), info);
                }
            }
        }

        // 找出持有阻塞最多线程的锁的线程
        int mostBlockingLock = 0; // System.identityHashCode(null) == 0
        int maxBlockingCount = 0;
        for (Map.Entry<Integer, Integer> entry: blockCountPerLock.entrySet()) {
            if (entry.getValue() > maxBlockingCount && ownerThreadPerLock.get(entry.getKey()) != null) {
                // 该锁被另一个线程明确持有
                maxBlockingCount = entry.getValue();
                mostBlockingLock = entry.getKey();
            }
        }

        if (mostBlockingLock == 0) {
            // 没有找到阻塞锁
            return EMPTY_INFO;
        }

        // 构建并返回阻塞锁信息
        BlockingLockInfo blockingLockInfo = new BlockingLockInfo();
        blockingLockInfo.setThreadInfo(ownerThreadPerLock.get(mostBlockingLock));
        blockingLockInfo.setLockIdentityHashCode(mostBlockingLock);
        blockingLockInfo.setBlockingThreadCount(blockCountPerLock.get(mostBlockingLock));
        return blockingLockInfo;
    }


    /**
     * 获取线程的完整堆栈信息（简化版）
     * <p>
     * 不包含CPU使用率、时间等信息，也不标记阻塞锁信息
     * </p>
     *
     * @param threadInfo 线程信息对象
     * @return 格式化的堆栈字符串
     */
    public static String getFullStacktrace(ThreadInfo threadInfo) {
        return getFullStacktrace(threadInfo, -1, -1, -1, 0, 0);
    }

    /**
     * 获取阻塞锁的完整堆栈信息
     * <p>
     * 包含阻塞锁的标识和被阻塞的线程数量
     * </p>
     *
     * @param blockingLockInfo 阻塞锁信息对象
     * @return 格式化的堆栈字符串
     */
    public static String getFullStacktrace(BlockingLockInfo blockingLockInfo) {
        return getFullStacktrace(blockingLockInfo.getThreadInfo(), -1, -1, -1, blockingLockInfo.getLockIdentityHashCode(),
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
    public static String getFullStacktrace(ThreadInfo threadInfo, double cpuUsage, long deltaTime, long time, int lockIdentityHashCode,
                                           int blockingThreadCount) {
        // 构建线程信息的头部
        StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" + " Id="
                + threadInfo.getThreadId());

        // 添加CPU使用率信息（如果提供且在有效范围内）
        if (cpuUsage >= 0 && cpuUsage <= 100) {
            sb.append(" cpuUsage=").append(cpuUsage).append("%");
        }
        // 添加增量时间信息（如果提供）
        if (deltaTime >= 0 ) {
            sb.append(" deltaTime=").append(deltaTime).append("ms");
        }
        // 添加总时间信息（如果提供）
        if (time >= 0 ) {
            sb.append(" time=").append(time).append("ms");
        }

        // 添加线程状态
        sb.append(" ").append(threadInfo.getThreadState());

        // 添加锁信息（如果线程正在等待锁）
        if (threadInfo.getLockName() != null) {
            sb.append(" on ").append(threadInfo.getLockName());
        }
        // 添加锁持有者信息（如果锁被其他线程持有）
        if (threadInfo.getLockOwnerName() != null) {
            sb.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\" Id=").append(threadInfo.getLockOwnerId());
        }
        // 添加挂起状态
        if (threadInfo.isSuspended()) {
            sb.append(" (suspended)");
        }
        // 添加本地方法状态
        if (threadInfo.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');

        // 遍历堆栈跟踪信息
        int i = 0;
        for (StackTraceElement ste : threadInfo.getStackTrace()) {
            sb.append("\tat ").append(ste.toString());
            sb.append('\n');

            // 在第一行堆栈信息后，添加锁的等待/阻塞信息
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

            // 添加当前堆栈层级持有的监视器（synchronized同步块）
            for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked ").append(mi);
                    // 如果该锁是阻塞其他线程的锁，用红色高亮标记
                    if (mi.getIdentityHashCode() == lockIdentityHashCode) {
                        Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);
                        highlighted.a(" <---- but blocks ").a(blockingThreadCount).a(" other threads!");
                        sb.append(highlighted.reset().toString());
                    }
                    sb.append('\n');
                }
            }
            ++i;
        }

        // 如果堆栈信息被截断，添加省略号
        if (i < threadInfo.getStackTrace().length) {
            sb.append("\t...");
            sb.append('\n');
        }

        // 添加持有的可拥有同步器信息（如ReentrantLock等）
        LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ").append(locks.length);
            sb.append('\n');
            for (LockInfo li : locks) {
                sb.append("\t- ").append(li);
                // 如果该同步器是阻塞其他线程的锁，添加提示信息
                if (li.getIdentityHashCode() == lockIdentityHashCode) {
                    sb.append(" <---- but blocks ").append(blockingThreadCount);
                    sb.append(" other threads!");
                }
                sb.append('\n');
            }
        }
        sb.append('\n');

        // 将制表符替换为4个空格，使输出更美观
        return sb.toString().replace("\t", "    ");
    }

    /**
     * 获取繁忙线程的完整堆栈信息
     * <p>
     * BusyThreadInfo包含了CPU使用率等额外信息，用于显示繁忙线程的详细信息。
     * 可以高亮显示阻塞其他线程的锁。
     * </p>
     *
     * @param threadInfo 繁忙线程信息对象
     * @param lockIdentityHashCode 阻塞锁的哈希码
     * @param blockingThreadCount 被阻塞的线程数量
     * @return 格式化的堆栈字符串
     */
    public static String getFullStacktrace(BusyThreadInfo threadInfo, int lockIdentityHashCode, int blockingThreadCount) {
        if (threadInfo == null) {
            return "";
        }

        // 构建线程信息的头部
        StringBuilder sb = new StringBuilder("\"" + threadInfo.getName() + "\"");
        if (threadInfo.getId() > 0) {
            sb.append(" Id=").append(threadInfo.getId());
        } else {
            sb.append(" [Internal]");
        }

        // 添加CPU使用率信息
        double cpuUsage = threadInfo.getCpu();
        if (cpuUsage >= 0 && cpuUsage <= 100) {
            sb.append(" cpuUsage=").append(cpuUsage).append("%");
        }
        // 添加增量时间信息
        if (threadInfo.getDeltaTime() >= 0 ) {
            sb.append(" deltaTime=").append(threadInfo.getDeltaTime()).append("ms");
        }
        // 添加总时间信息
        if (threadInfo.getTime() >= 0 ) {
            sb.append(" time=").append(threadInfo.getTime()).append("ms");
        }

        // 如果线程状态为空，直接返回
        if (threadInfo.getState() == null) {
            sb.append("\n\n");
            return sb.toString();
        }

        // 添加线程状态
        sb.append(" ").append(threadInfo.getState());

        // 添加锁信息
        if (threadInfo.getLockName() != null) {
            sb.append(" on ").append(threadInfo.getLockName());
        }
        // 添加锁持有者信息
        if (threadInfo.getLockOwnerName() != null) {
            sb.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\" Id=").append(threadInfo.getLockOwnerId());
        }
        // 添加挂起状态
        if (threadInfo.isSuspended()) {
            sb.append(" (suspended)");
        }
        // 添加本地方法状态
        if (threadInfo.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');

        // 遍历堆栈跟踪信息
        int i = 0;
        for (; i < threadInfo.getStackTrace().length; i++) {
            StackTraceElement ste = threadInfo.getStackTrace()[i];
            sb.append("\tat ").append(ste.toString());
            sb.append('\n');

            // 在第一行堆栈信息后，添加锁的等待/阻塞信息
            if (i == 0 && threadInfo.getLockInfo() != null) {
                Thread.State ts = threadInfo.getState();
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

            // 添加当前堆栈层级持有的监视器
            for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked ").append(mi);
                    // 如果该锁是阻塞其他线程的锁，用红色高亮标记
                    if (mi.getIdentityHashCode() == lockIdentityHashCode) {
                        Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);
                        highlighted.a(" <---- but blocks ").a(blockingThreadCount).a(" other threads!");
                        sb.append(highlighted.reset().toString());
                    }
                    sb.append('\n');
                }
            }
        }

        // 如果堆栈信息被截断，添加省略号
        if (i < threadInfo.getStackTrace().length) {
            sb.append("\t...");
            sb.append('\n');
        }

        // 添加持有的可拥有同步器信息
        LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ").append(locks.length);
            sb.append('\n');
            for (LockInfo li : locks) {
                sb.append("\t- ").append(li);
                // 如果该同步器是阻塞其他线程的锁，添加提示信息
                if (li.getIdentityHashCode() == lockIdentityHashCode) {
                    sb.append(" <---- but blocks ").append(blockingThreadCount);
                    sb.append(" other threads!");
                }
                sb.append('\n');
            }
        }
        sb.append('\n');

        // 将制表符替换为4个空格
        return sb.toString().replace("\t", "    ");
    }

    /**
     * 魔法堆栈深度
     * <p>
     * 用于缓存SpyAPI在堆栈中的位置，避免每次都重新查找。
     * SpyAPI是Arthas的增强代码入口，获取堆栈时需要跳过这些框架代码。
     * </p>
     *
     * </pre>
     * 示例堆栈：
     * java.lang.Thread.getStackTrace(Thread.java:1559),
     * com.taobao.arthas.core.util.ThreadUtil.getThreadStack(ThreadUtil.java:349),
     * com.taobao.arthas.core.command.monitor200.StackAdviceListener.before(StackAdviceListener.java:33),
     * com.taobao.arthas.core.advisor.AdviceListenerAdapter.before(AdviceListenerAdapter.java:49),
     * com.taobao.arthas.core.advisor.SpyImpl.atEnter(SpyImpl.java:42),
     * java.arthas.SpyAPI.atEnter(SpyAPI.java:40),    <-- SpyAPI位置，需要跳过
     * demo.MathGame.print(MathGame.java),
     * demo.MathGame.run(MathGame.java:25),
     * demo.MathGame.main(MathGame.java:16)
     * </pre>
     */
    private static int MAGIC_STACK_DEPTH = 0;

    /**
     * 查找SpyAPI在堆栈中的位置
     * <p>
     * SpyAPI是Arthas增强代码的入口点，在获取用户代码的堆栈时，
     * 需要跳过Arthas框架相关的堆栈帧。
     * 该方法会找到SpyAPI的位置并缓存，提高后续查找的性能。
     * </p>
     *
     * @param stackTraceElementArray 堆栈跟踪元素数组
     * @return SpyAPI位置的下一个索引，即用户代码的起始位置
     */
    private static int findTheSpyAPIDepth(StackTraceElement[] stackTraceElementArray) {
        // 如果已经缓存了深度，直接返回
        if (MAGIC_STACK_DEPTH > 0) {
            return MAGIC_STACK_DEPTH;
        }
        // 边界检查
        if (MAGIC_STACK_DEPTH > stackTraceElementArray.length) {
            return 0;
        }

        // 遍历堆栈，查找SpyAPI的位置
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
    /**
     * 获取线程堆栈模型
     * <p>
     * 构建包含线程详细信息和堆栈跟踪的模型对象。
     * 会自动跳过Arthas框架的堆栈帧，只保留用户代码的堆栈。
     * 如果存在EagleEye，会附加链路追踪信息。
     * </p>
     *
     * @param loader 类加载器，用于加载EagleEye类
     * @param currentThread 当前线程
     * @return 线程堆栈模型
     */
    public static StackModel getThreadStackModel(ClassLoader loader, Thread currentThread) {
        StackModel stackModel = new StackModel();
        // 设置线程的基本信息
        stackModel.setThreadName(currentThread.getName());
        stackModel.setThreadId(Long.toString(currentThread.getId()));
        stackModel.setDaemon(currentThread.isDaemon());
        stackModel.setPriority(currentThread.getPriority());
        stackModel.setClassloader(getTCCL(currentThread));

        // 尝试获取EagleEye链路追踪信息
        getEagleeyeTraceInfo(loader, currentThread, stackModel);

        // 获取堆栈信息，并跳过Arthas框架相关的堆栈帧
        StackTraceElement[] stackTraceElementArray = currentThread.getStackTrace();
        int magicStackDepth = findTheSpyAPIDepth(stackTraceElementArray);
        StackTraceElement[] actualStackFrames = new StackTraceElement[stackTraceElementArray.length - magicStackDepth];
        System.arraycopy(stackTraceElementArray, magicStackDepth , actualStackFrames, 0, actualStackFrames.length);
        stackModel.setStackTrace(actualStackFrames);
        return stackModel;
    }

    /**
     * 获取线程节点
     * <p>
     * 构建一个简化的线程节点对象，包含线程的基本信息和链路追踪信息。
     * 主要用于线程树的展示。
     * </p>
     *
     * @param loader 类加载器，用于加载EagleEye类
     * @param currentThread 当前线程
     * @return 线程节点对象
     */
    public static ThreadNode getThreadNode(ClassLoader loader, Thread currentThread) {
        ThreadNode threadNode = new ThreadNode();
        // 设置线程的基本信息
        threadNode.setThreadId(currentThread.getId());
        threadNode.setThreadName(currentThread.getName());
        threadNode.setDaemon(currentThread.isDaemon());
        threadNode.setPriority(currentThread.getPriority());
        threadNode.setClassloader(getTCCL(currentThread));

        // 获取EagleEye链路追踪信息（traceId和rpcId）
        StackModel stackModel = new StackModel();
        getEagleeyeTraceInfo(loader, currentThread, stackModel);
        threadNode.setTraceId(stackModel.getTraceId());
        threadNode.setRpcId(stackModel.getRpcId());
        return threadNode;
    }

    /**
     * 获取线程标题字符串
     * <p>
     * 将线程模型的详细信息格式化为一个字符串，用于日志输出或展示。
     * </p>
     *
     * @param stackModel 线程堆栈模型
     * @return 格式化的线程标题字符串
     */
    public static String getThreadTitle(StackModel stackModel) {
        StringBuilder sb = new StringBuilder("thread_name=");
        sb.append(stackModel.getThreadName())
                .append(";id=").append(stackModel.getThreadId())
                .append(";is_daemon=").append(stackModel.isDaemon())
                .append(";priority=").append(stackModel.getPriority())
                .append(";TCCL=").append(stackModel.getClassloader());
        // 如果有EagleEye的traceId，添加到标题中
        if (stackModel.getTraceId() != null) {
            sb.append(";trace_id=").append(stackModel.getTraceId());
        }
        // 如果有EagleEye的rpcId，添加到标题中
        if (stackModel.getRpcId() != null) {
            sb.append(";rpc_id=").append(stackModel.getRpcId());
        }
        return sb.toString();
    }

    /**
     * 获取线程的上下文类加载器（TCCL）信息
     * <p>
     * 返回类加载器的类名和哈希码的字符串表示。
     * </p>
     *
     * @param currentThread 当前线程
     * @return 类加载器信息字符串，格式为 "类名@哈希码"，如果为null则返回"null"
     */
    private static String getTCCL(Thread currentThread) {
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        if (null == contextClassLoader) {
            return "null";
        } else {
            return contextClassLoader.getClass().getName() +
                    "@" +
                    Integer.toHexString(contextClassLoader.hashCode());
        }
    }

    /**
     * 获取EagleEye链路追踪信息
     * <p>
     * EagleEye是阿里巴巴的分布式链路追踪系统。
     * 该方法通过反射调用EagleEye的API获取当前请求的traceId和rpcId。
     * 由于EagleEye可能不存在，使用了try-catch来处理异常。
     * </p>
     *
     * @param loader 类加载器，用于加载EagleEye类
     * @param currentThread 当前线程（未使用，保留用于可能的扩展）
     * @param stackModel 线程堆栈模型，用于存储获取到的链路追踪信息
     */
    private static void getEagleeyeTraceInfo(ClassLoader loader, Thread currentThread, StackModel stackModel) {
        if(loader == null) {
            return;
        }

        Class<?> eagleEyeClass = null;
        // 首次检测EagleEye是否存在
        if (!detectedEagleEye) {
            try {
                eagleEyeClass = loader.loadClass("com.taobao.eagleeye.EagleEye");
                foundEagleEye = true;
            } catch (Throwable e) {
                // EagleEye不存在，忽略异常
            }
            detectedEagleEye = true;
        }

        // 如果没有找到EagleEye，直接返回
        if (!foundEagleEye) {
            return;
        }

        try {
            // 再次加载EagleEye类（如果之前没有加载成功）
            if (eagleEyeClass == null) {
                eagleEyeClass = loader.loadClass("com.taobao.eagleeye.EagleEye");
            }
            // 通过反射调用getTraceId()方法
            Method getTraceIdMethod = eagleEyeClass.getMethod("getTraceId");
            String traceId = (String) getTraceIdMethod.invoke(null);
            stackModel.setTraceId(traceId);
            // 通过反射调用getRpcId()方法
            Method getRpcIdMethod = eagleEyeClass.getMethod("getRpcId");
            String rpcId = (String) getRpcIdMethod.invoke(null);
            stackModel.setRpcId(rpcId);
        } catch (Throwable e) {
            // 反射调用失败，忽略异常
        }
    }

}

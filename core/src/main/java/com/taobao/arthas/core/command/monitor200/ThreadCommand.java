package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.BlockingLockInfo;
import com.taobao.arthas.core.command.model.BusyThreadInfo;
import com.taobao.arthas.core.command.model.ThreadModel;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.ArrayUtils;
import com.taobao.arthas.core.util.CommandUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 线程查看命令
 * <p>
 * 用于查看和排查Java线程相关问题，支持多种功能：
 * 1. 查看所有线程列表及其状态统计
 * 2. 查看指定线程的详细信息和调用栈
 * 3. 查找CPU使用率最高的N个线程
 * 4. 查找持有锁并阻塞其他线程最多的线程
 * 5. 按线程状态过滤显示
 * <p>
 * 常用场景：
 * - 排查CPU占用高的问题
 * - 排查线程死锁问题
 * - 查看线程状态分布
 * - 分析线程调用栈
 *
 * @author hengyunabc 2015年12月7日 下午2:06:21
 */
@Name("thread")
@Summary("Display thread info, thread stack")
@Description(Constants.EXAMPLE +
        "  thread\n" +
        "  thread 51\n" +
        "  thread -n -1\n" +
        "  thread -n 5\n" +
        "  thread -b\n" +
        "  thread -i 2000\n" +
        "  thread --state BLOCKED\n" +
        Constants.WIKI + Constants.WIKI_HOME + "thread")
public class ThreadCommand extends AnnotatedCommand {
    /** 线程状态集合，包含所有可能的线程状态名称 */
    private static Set<String> states = null;
    /** JVM线程管理Bean，用于获取线程信息 */
    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /** 线程ID，用于查看指定线程的详细信息 */
    private long id = -1;
    /** 要显示的CPU使用率最高的线程数量，null表示不启用此功能 */
    private Integer topNBusy = null;
    /** 是否查找阻塞其他线程最多的线程 */
    private boolean findMostBlockingThread = false;
    /** CPU采样间隔（毫秒），默认200毫秒 */
    private int sampleInterval = 200;
    /** 线程状态过滤条件，只显示指定状态的线程 */
    private String state;

    /** 是否获取锁定的监视器信息 */
    private boolean lockedMonitors = false;
    /** 是否获取锁定的同步器信息 */
    private boolean lockedSynchronizers = false;
    /** 是否显示所有线程，false表示分页显示 */
    private boolean all = false;

    static {
        // 初始化线程状态集合，包含所有Thread.State枚举值
        states = new HashSet<String>(State.values().length);
        for (State state : State.values()) {
            states.add(state.name());
        }
    }

    /**
     * 设置要查看的线程ID
     * 指定后只显示该线程的详细信息和调用栈
     *
     * @param id 线程ID
     */
    @Argument(index = 0, required = false, argName = "id")
    @Description("Show thread stack")
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 设置是否显示所有线程
     * 默认分页显示，设置此标志后显示所有线程
     *
     * @param all true表示显示所有线程
     */
    @Option(longName = "all", flag = true)
    @Description("Display all thread results instead of the first page")
    public void setAll(boolean all) {
        this.all = all;
    }

    /**
     * 设置要显示的CPU使用率最高的线程数量
     * -1表示显示所有线程，按CPU使用率排序
     *
     * @param topNBusy 线程数量
     */
    @Option(shortName = "n", longName = "top-n-threads")
    @Description("The number of thread(s) to show, ordered by cpu utilization, -1 to show all.")
    public void setTopNBusy(Integer topNBusy) {
        this.topNBusy = topNBusy;
    }

    /**
     * 设置是否查找阻塞其他线程最多的线程
     * 用于排查锁竞争和死锁问题
     *
     * @param findMostBlockingThread true表示查找阻塞线程
     */
    @Option(shortName = "b", longName = "include-blocking-thread", flag = true)
    @Description("Find the thread who is holding a lock that blocks the most number of threads.")
    public void setFindMostBlockingThread(boolean findMostBlockingThread) {
        this.findMostBlockingThread = findMostBlockingThread;
    }

    /**
     * 设置CPU采样间隔（毫秒）
     * 两次采样之间的时间间隔，用于计算CPU使用率
     *
     * @param sampleInterval 采样间隔（毫秒）
     */
    @Option(shortName = "i", longName = "sample-interval")
    @Description("Specify the sampling interval (in ms) when calculating cpu usage.")
    public void setSampleInterval(int sampleInterval) {
        this.sampleInterval = sampleInterval;
    }

    /**
     * 设置线程状态过滤条件
     * 只显示指定状态的线程，可选值：NEW, RUNNABLE, TIMED_WAITING, WAITING, BLOCKED, TERMINATED
     *
     * @param state 线程状态
     */
    @Option(longName = "state")
    @Description("Display the thread filter by the state. NEW, RUNNABLE, TIMED_WAITING, WAITING, BLOCKED, TERMINATED is optional.")
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 设置是否获取锁定的监视器信息
     * 监视器是Java中实现synchronized的一种机制
     *
     * @param lockedMonitors true表示获取锁定监视器信息
     */
    @Option(longName = "lockedMonitors", flag = true)
    @Description("Find the thread info with lockedMonitors flag, default value is false.")
    public void setLockedMonitors(boolean lockedMonitors) {
        this.lockedMonitors = lockedMonitors;
    }

    /**
     * 设置是否获取锁定的同步器信息
     * 同步器是Java并发包中实现锁的一种机制（如ReentrantLock）
     *
     * @param lockedSynchronizers true表示获取锁定同步器信息
     */
    @Option(longName = "lockedSynchronizers", flag = true)
    @Description("Find the thread info with lockedSynchronizers flag, default value is false.")
    public void setLockedSynchronizers(boolean lockedSynchronizers) {
        this.lockedSynchronizers = lockedSynchronizers;
    }

    /**
     * 命令处理入口
     * 根据不同的参数选择不同的处理逻辑
     *
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        ExitStatus exitStatus;
        if (id > 0) {
            // 查看指定线程的详细信息
            exitStatus = processThread(process);
        } else if (topNBusy != null) {
            // 查看CPU使用率最高的N个线程
            exitStatus = processTopBusyThreads(process);
        } else if (findMostBlockingThread) {
            // 查找阻塞其他线程最多的线程
            exitStatus = processBlockingThread(process);
        } else {
            // 查看所有线程的列表和状态统计
            exitStatus = processAllThreads(process);
        }
        // 结束命令处理
        CommandUtils.end(process, exitStatus);
    }

    /**
     * 处理所有线程的显示
     * 显示线程列表、状态统计和CPU使用率
     *
     * @param process 命令处理进程
     * @return 处理状态
     */
    private ExitStatus processAllThreads(CommandProcess process) {
        // 获取所有线程信息
        List<ThreadVO> threads = ThreadUtil.getThreads();

        // 统计各种线程状态的数量
        Map<State, Integer> stateCountMap = new LinkedHashMap<State, Integer>();
        for (State s : State.values()) {
            stateCountMap.put(s, 0);
        }

        // 遍历所有线程，统计各状态的线程数
        for (ThreadVO thread : threads) {
            State threadState = thread.getState();
            Integer count = stateCountMap.get(threadState);
            stateCountMap.put(threadState, count + 1);
        }

        // 是否包含内部线程（JVM内部线程）
        boolean includeInternalThreads = true;
        Collection<ThreadVO> resultThreads = new ArrayList<ThreadVO>();
        // 如果指定了状态过滤条件
        if (!StringUtils.isEmpty(this.state)) {
            this.state = this.state.toUpperCase();
            if (states.contains(this.state)) {
                // 只统计指定状态的线程，不包含内部线程
                includeInternalThreads = false;
                for (ThreadVO thread : threads) {
                    if (thread.getState() != null && state.equals(thread.getState().name())) {
                        resultThreads.add(thread);
                    }
                }
            } else {
                // 状态参数无效
                return ExitStatus.failure(1, "Illegal argument, state should be one of " + states);
            }
        } else {
            // 显示所有线程
            resultThreads = threads;
        }

        // 计算线程CPU使用率
        // 第一次采样：记录当前CPU时间
        ThreadSampler threadSampler = new ThreadSampler();
        threadSampler.setIncludeInternalThreads(includeInternalThreads);
        threadSampler.sample(resultThreads);
        // 等待指定的采样间隔
        threadSampler.pause(sampleInterval);
        // 第二次采样：计算CPU使用率
        List<ThreadVO> threadStats = threadSampler.sample(resultThreads);

        // 输出结果
        process.appendResult(new ThreadModel(threadStats, stateCountMap, all));
        return ExitStatus.success();
    }

    /**
     * 查找并显示阻塞其他线程最多的线程
     * 该线程持有的锁被最多其他线程等待
     *
     * @param process 命令处理进程
     * @return 处理状态
     */
    private ExitStatus processBlockingThread(CommandProcess process) {
        // 查找持有锁并阻塞最多线程的锁信息
        BlockingLockInfo blockingLockInfo = ThreadUtil.findMostBlockingLock();
        if (blockingLockInfo.getThreadInfo() == null) {
            // 没有找到阻塞线程
            return ExitStatus.failure(1, "No most blocking thread found!");
        }
        // 输出阻塞线程信息
        process.appendResult(new ThreadModel(blockingLockInfo));
        return ExitStatus.success();
    }

    /**
     * 查找并显示CPU使用率最高的N个线程
     * 通过两次采样计算线程的CPU使用率
     *
     * @param process 命令处理进程
     * @return 处理状态
     */
    private ExitStatus processTopBusyThreads(CommandProcess process) {
        // 创建线程采样器
        ThreadSampler threadSampler = new ThreadSampler();
        // 第一次采样：记录当前CPU时间
        threadSampler.sample(ThreadUtil.getThreads());
        // 等待指定的采样间隔
        threadSampler.pause(sampleInterval);
        // 第二次采样：计算CPU使用率
        List<ThreadVO> threadStats = threadSampler.sample(ThreadUtil.getThreads());

        // 确定要显示的线程数量
        int limit = Math.min(threadStats.size(), topNBusy);

        List<ThreadVO> topNThreads = null;
        if (limit > 0) {
            // 获取前N个CPU使用率最高的线程
            topNThreads = threadStats.subList(0, limit);
        } else {
            // -1 表示显示所有线程
            topNThreads = threadStats;
        }

        // 提取线程ID列表
        List<Long> tids = new ArrayList<Long>(topNThreads.size());
        for (ThreadVO thread : topNThreads) {
            if (thread.getId() > 0) {
                tids.add(thread.getId());
            }
        }

        // 获取线程的详细信息（包括调用栈、锁信息等）
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ArrayUtils.toPrimitive(tids.toArray(new Long[0])), lockedMonitors, lockedSynchronizers);
        if (tids.size()> 0 && threadInfos == null) {
            return ExitStatus.failure(1, "get top busy threads failed");
        }

        // 合并线程VO和ThreadInfo，添加CPU使用率信息
        List<BusyThreadInfo> busyThreadInfos = new ArrayList<BusyThreadInfo>(topNThreads.size());
        for (ThreadVO thread : topNThreads) {
            // 根据线程ID查找对应的ThreadInfo
            ThreadInfo threadInfo = findThreadInfoById(threadInfos, thread.getId());
            if (threadInfo != null) {
                BusyThreadInfo busyThread = new BusyThreadInfo(thread, threadInfo);
                busyThreadInfos.add(busyThread);
            }
        }
        // 输出结果
        process.appendResult(new ThreadModel(busyThreadInfos));
        return ExitStatus.success();
    }

    /**
     * 根据线程ID在ThreadInfo数组中查找对应的ThreadInfo
     *
     * @param threadInfos ThreadInfo数组
     * @param id 线程ID
     * @return 找到的ThreadInfo，未找到返回null
     */
    private ThreadInfo findThreadInfoById(ThreadInfo[] threadInfos, long id) {
        for (int i = 0; i < threadInfos.length; i++) {
            ThreadInfo threadInfo = threadInfos[i];
            if (threadInfo != null && threadInfo.getThreadId() == id) {
                return threadInfo;
            }
        }
        return null;
    }

    /**
     * 处理指定线程的详细信息显示
     * 显示指定线程的完整信息，包括调用栈、锁信息等
     *
     * @param process 命令处理进程
     * @return 处理状态
     */
    private ExitStatus processThread(CommandProcess process) {
        // 根据线程ID获取线程信息
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(new long[]{id}, lockedMonitors, lockedSynchronizers);
        if (threadInfos == null || threadInfos.length < 1 || threadInfos[0] == null) {
            // 线程不存在
            return ExitStatus.failure(1, "thread do not exist! id: " + id);
        }

        // 输出线程信息
        process.appendResult(new ThreadModel(threadInfos[0]));
        return ExitStatus.success();
    }
}

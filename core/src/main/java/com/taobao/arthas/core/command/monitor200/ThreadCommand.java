package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.command.model.ThreadInfoExt;
import com.taobao.arthas.core.command.model.ThreadModel;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArrayUtils;
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
import java.util.*;

/**
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
    private static Set<String> states = null;
    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private long id = -1;
    private Integer topNBusy = null;
    private boolean findMostBlockingThread = false;
    private int sampleInterval = 100;
    private String state;

    {
        states = new HashSet<String>(State.values().length);
        for (State state : State.values()) {
            states.add(state.name());
        }
    }

    @Argument(index = 0, required = false, argName = "id")
    @Description("Show thread stack")
    public void setId(long id) {
        this.id = id;
    }

    @Option(shortName = "n", longName = "top-n-threads")
    @Description("The number of thread(s) to show, ordered by cpu utilization, -1 to show all.")
    public void setTopNBusy(Integer topNBusy) {
        this.topNBusy = topNBusy;
    }

    @Option(shortName = "b", longName = "include-blocking-thread", flag = true)
    @Description("Find the thread who is holding a lock that blocks the most number of threads.")
    public void setFindMostBlockingThread(boolean findMostBlockingThread) {
        this.findMostBlockingThread = findMostBlockingThread;
    }

    @Option(shortName = "i", longName = "sample-interval")
    @Description("Specify the sampling interval (in ms) when calculating cpu usage.")
    public void setSampleInterval(int sampleInterval) {
        this.sampleInterval = sampleInterval;
    }

    @Option(longName = "state")
    @Description("Display the thead filter by the state. NEW, RUNNABLE, TIMED_WAITING, WAITING, BLOCKED, TERMINATED is optional.")
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public void process(CommandProcess process) {
        StatusModel statusModel = new StatusModel(-1, "unknown error");
        try {
            if (id > 0) {
                statusModel = processThread(process);
            } else if (topNBusy != null) {
                statusModel = processTopBusyThreads(process);
            } else if (findMostBlockingThread) {
                statusModel = processBlockingThread(process);
            } else {
                statusModel = processAllThreads(process);
            }
        } finally {
            process.end(statusModel.getStatusCode(), statusModel.getMessage());
        }
    }

    private StatusModel processAllThreads(CommandProcess process) {
        Map<String, Thread> threads = ThreadUtil.getThreads();

        // 统计各种线程状态
        Map<State, Integer> stateCountMap = new LinkedHashMap<State, Integer>();
        for (State s : State.values()) {
            stateCountMap.put(s, 0);
        }

        for (Thread thread : threads.values()) {
            State threadState = thread.getState();
            Integer count = stateCountMap.get(threadState);
            stateCountMap.put(threadState, count + 1);
        }

        Collection<Thread> resultThreads = new ArrayList<Thread>();
        if (!StringUtils.isEmpty(this.state)){
            this.state = this.state.toUpperCase();
            if(states.contains(this.state)) {
                for (Thread thread : threads.values()) {
                    if (state.equals(thread.getState().name())) {
                        resultThreads.add(thread);
                    }
                }
            }else{
                return new StatusModel(1, "Illegal argument, state should be one of " + states);
            }
        } else {
            resultThreads = threads.values();
        }

        //thread stats
        ThreadSampler threadSampler = new ThreadSampler();
        threadSampler.setSampleInterval(sampleInterval);
        List<ThreadVO> threadStats = threadSampler.sample(resultThreads);

        process.appendResult(new ThreadModel(threadStats, stateCountMap));
        return new StatusModel(0);
    }

    private StatusModel processBlockingThread(CommandProcess process) {
        ThreadUtil.BlockingLockInfo blockingLockInfo = ThreadUtil.findMostBlockingLock();
        if (blockingLockInfo.threadInfo == null) {
            return new StatusModel(1, "No most blocking thread found!");
        }
        process.appendResult(new ThreadModel(blockingLockInfo));
        return new StatusModel(0);    }

    private StatusModel processTopBusyThreads(CommandProcess process) {
        Map<Long, Long> topNThreads = ThreadUtil.getTopNThreads(sampleInterval, topNBusy);
        Long[] tids = topNThreads.keySet().toArray(new Long[0]);
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ArrayUtils.toPrimitive(tids), true, true);
        if (threadInfos == null) {
            return new StatusModel(1, "get top busy threads failed");
        }

        //convert to ThreadInfoExt with cpuUsage
        ThreadInfoExt[] threadInfoExts = new ThreadInfoExt[threadInfos.length];
        for (int i = 0; i < threadInfos.length; i++) {
            ThreadInfo threadInfo = threadInfos[i];
            threadInfoExts[i] = new ThreadInfoExt(threadInfo, topNThreads.get(threadInfo.getThreadId()));
        }
        process.appendResult(new ThreadModel(threadInfoExts));
        return new StatusModel(0);
    }

    private StatusModel processThread(CommandProcess process) {
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(new long[]{id}, true, true);
        if (threadInfos == null || threadInfos.length < 1 || threadInfos[0] == null) {
            return new StatusModel(1, "thread do not exist! id: " + id);
        }

        process.appendResult(new ThreadModel(threadInfos[0]));
        return new StatusModel(0);
    }
}
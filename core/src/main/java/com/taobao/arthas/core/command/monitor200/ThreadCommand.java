package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArrayUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.arthas.core.util.affect.Affect;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.renderers.ThreadRenderer;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.util.RenderUtil;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    private boolean lockedMonitors = false;
    private boolean lockedSynchronizers = false;

    static {
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

    @Option(longName = "lockedMonitors", flag = true)
    @Description("Find the thread info with lockedMonitors flag, default value is false.")
    public void setLockedMonitors(boolean lockedMonitors) {
        this.lockedMonitors = lockedMonitors;
    }

    @Option(longName = "lockedSynchronizers", flag = true)
    @Description("Find the thread info with lockedSynchronizers flag, default value is false.")
    public void setLockedSynchronizers(boolean lockedSynchronizers) {
        this.lockedSynchronizers = lockedSynchronizers;
    }

    @Override
    public void process(CommandProcess process) {
        Affect affect = new RowAffect();
        int status = 0;
        try {
            if (id > 0) {
                status = processThread(process);
            } else if (topNBusy != null) {
                status = processTopBusyThreads(process);
            } else if (findMostBlockingThread) {
                status = processBlockingThread(process);
            } else {
                status = processAllThreads(process);
            }
        } finally {
            process.write(affect + "\n");
            process.end(status);
        }
    }

    private int processAllThreads(CommandProcess process) {
        int status = 0;
        Map<String, Thread> threads = ThreadUtil.getThreads();

        // 统计各种线程状态
        StringBuilder threadStat = new StringBuilder();
        Map<State, Integer> stateCountMap = new HashMap<State, Integer>();
        for (State s : State.values()) {
            stateCountMap.put(s, 0);
        }

        for (Thread thread : threads.values()) {
            State threadState = thread.getState();
            Integer count = stateCountMap.get(threadState);
            stateCountMap.put(threadState, count + 1);
        }

        threadStat.append("Threads Total: ").append(threads.values().size());

        for (State s : State.values()) {
            Integer count = stateCountMap.get(s);
            threadStat.append(", ").append(s.name()).append(": ").append(count);
        }

        String stat = RenderUtil.render(new LabelElement(threadStat), process.width());

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
                process.write("Illegal argument, state should be one of " + states + "\n");
                status = 1;
                return status;
            }
        } else {
            resultThreads = threads.values();
        }
        String content = RenderUtil.render(resultThreads.iterator(),
                new ThreadRenderer(sampleInterval), process.width());
        process.write(stat + content);
        return status;
    }

    private int processBlockingThread(CommandProcess process) {
        int status = 0;
        ThreadUtil.BlockingLockInfo blockingLockInfo = ThreadUtil.findMostBlockingLock();

        if (blockingLockInfo.threadInfo == null) {
            process.write("No most blocking thread found!\n");
            status = 1;
        } else {
            String stacktrace = ThreadUtil.getFullStacktrace(blockingLockInfo);
            process.write(stacktrace);
        }
        return status;
    }

    private int processTopBusyThreads(CommandProcess process) {
        int status = 0;
        Map<Long, Long> topNThreads = ThreadUtil.getTopNThreads(sampleInterval, topNBusy);
        Long[] tids = topNThreads.keySet().toArray(new Long[0]);
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ArrayUtils.toPrimitive(tids), lockedMonitors, lockedSynchronizers);
        if (threadInfos == null) {
            process.write("thread do not exist! id: " + id + "\n");
            status = 1;
        } else {
            for (ThreadInfo info : threadInfos) {
                String stacktrace = ThreadUtil.getFullStacktrace(info, topNThreads.get(info.getThreadId()));
                process.write(stacktrace + "\n");
            }
        }
        return status;
    }

    private int processThread(CommandProcess process) {
        int status = 0;
        String content;
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(new long[]{id}, lockedMonitors, lockedSynchronizers);
        if (threadInfos == null || threadInfos[0] == null) {
            content = "thread do not exist! id: " + id + "\n";
            status = 1;
        } else {
            // no cpu usage info
            content = ThreadUtil.getFullStacktrace(threadInfos[0], -1);
        }
        process.write(content);
        return status;
    }
}
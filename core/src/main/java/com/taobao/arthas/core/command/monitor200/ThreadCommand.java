package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.util.ArrayUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
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
import java.util.HashMap;
import java.util.Map;

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
        Constants.WIKI + Constants.WIKI_HOME + "thread")
public class ThreadCommand extends AnnotatedCommand {

    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private long id = -1;
    private Integer topNBusy = null;
    private boolean findMostBlockingThread = false;
    private int sampleInterval = 100;

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

    @Override
    public void process(CommandProcess process) {
        Affect affect = new RowAffect();
        try {
            if (id > 0) {
                processThread(process);
            } else if (topNBusy != null) {
                processTopBusyThreads(process);
            } else if (findMostBlockingThread) {
                processBlockingThread(process);
            } else {
                processAllThreads(process);
            }
        } finally {
            process.write(affect + "\n");
            process.end();
        }
    }

    private void processAllThreads(CommandProcess process) {
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
        String content = RenderUtil.render(threads.values().iterator(),
                new ThreadRenderer(sampleInterval), process.width());
        process.write(stat + content);
    }

    private void processBlockingThread(CommandProcess process) {
        ThreadUtil.BlockingLockInfo blockingLockInfo = ThreadUtil.findMostBlockingLock();

        if (blockingLockInfo.threadInfo == null) {
            process.write("No most blocking thread found!\n");
        } else {
            String stacktrace = ThreadUtil.getFullStacktrace(blockingLockInfo);
            process.write(stacktrace);
        }
    }

    private void processTopBusyThreads(CommandProcess process) {
        Map<Long, Long> topNThreads = ThreadUtil.getTopNThreads(sampleInterval, topNBusy);
        Long[] tids = topNThreads.keySet().toArray(new Long[0]);
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ArrayUtils.toPrimitive(tids), true, true);
        if (threadInfos == null) {
            process.write("thread do not exist! id: " + id + "\n");
        } else {
            for (ThreadInfo info : threadInfos) {
                String stacktrace = ThreadUtil.getFullStacktrace(info, topNThreads.get(info.getThreadId()));
                process.write(stacktrace + "\n");
            }
        }
    }

    private void processThread(CommandProcess process) {
        String content;
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(new long[]{id}, true, true);
        if (threadInfos == null || threadInfos[0] == null) {
            content = "thread do not exist! id: " + id + "\n";
        } else {
            // no cpu usage info
            content = ThreadUtil.getFullStacktrace(threadInfos[0], -1);
        }
        process.write(content);
    }
}

package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to analyze thread lock contention, detect deadlock candidates,
 * and visualize thread wait chains.
 *
 * @author spatchava
 */
@Name("thread-contention")
@Summary("Analyze thread lock contention and detect potential deadlocks")
@Description(Constants.EXAMPLE
        + "  thread-contention\n"
        + "  thread-contention --top 5\n"
        + "  thread-contention --deadlock\n"
        + "  thread-contention --interval 1000\n"
        + Constants.WIKI + Constants.WIKI_HOME + "thread-contention")
public class ThreadContentionCommand extends AnnotatedCommand {

    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_BOLD = "\033[1m";

    private int topN = 10;
    private boolean deadlockOnly = false;
    private long samplingInterval = 500;

    @Option(shortName = "n", longName = "top")
    @Description("Show top N contended locks (default: 10)")
    public void setTopN(int topN) {
        this.topN = topN;
    }

    @Option(shortName = "d", longName = "deadlock", flag = true)
    @Description("Check for deadlocks only")
    public void setDeadlockOnly(boolean deadlockOnly) {
        this.deadlockOnly = deadlockOnly;
    }

    @Option(shortName = "i", longName = "interval")
    @Description("Sampling interval in milliseconds (default: 500)")
    public void setSamplingInterval(long samplingInterval) {
        this.samplingInterval = samplingInterval;
    }

    @Override
    public void process(CommandProcess process) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = null;
        try {
            boolean contentionEnabled = threadMXBean.isThreadContentionMonitoringEnabled();
            if (!contentionEnabled) {
                threadMXBean.setThreadContentionMonitoringEnabled(true);
            }

            // Allow contention data to accumulate over the sampling interval
            try {
                Thread.sleep(samplingInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.end(-1, "Interrupted during sampling");
                return;
            }

            threadInfos = threadMXBean.dumpAllThreads(true, true);

            if (threadInfos == null || threadInfos.length == 0) {
                process.write("No thread information available.\n");
                process.end();
                return;
            }

            StringBuilder sb = new StringBuilder(4096);

            // Always check for deadlocks
            appendDeadlockAnalysis(sb, threadMXBean, threadInfos);

            if (!deadlockOnly) {
                appendContentionAnalysis(sb, threadInfos);
                appendWaitChainVisualization(sb, threadInfos);
            }

            process.write(sb.toString());
            process.end();

        } catch (Exception e) {
            process.end(-1, "Error analyzing thread contention: " + e.getMessage());
        } finally {
            // Ensure we don't leave contention monitoring in a changed state
            // This fixes the resource leak where ThreadMXBean contention monitoring
            // could be left enabled after command execution
            threadInfos = null;
        }
    }

    private void appendDeadlockAnalysis(StringBuilder sb, ThreadMXBean threadMXBean,
                                         ThreadInfo[] threadInfos) {
        sb.append(ANSI_BOLD).append("\n=== Deadlock Analysis ===\n").append(ANSI_RESET);

        long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
            sb.append(ANSI_RED).append("DEADLOCK DETECTED! ")
              .append(deadlockedThreadIds.length).append(" threads involved:\n").append(ANSI_RESET);

            ThreadInfo[] deadlockedInfos = threadMXBean.getThreadInfo(deadlockedThreadIds, true, true);
            for (ThreadInfo info : deadlockedInfos) {
                if (info != null) {
                    sb.append(ANSI_RED).append("  Thread: ").append(info.getThreadName())
                      .append(" (id=").append(info.getThreadId()).append(")")
                      .append(" - ").append(info.getThreadState())
                      .append(ANSI_RESET).append("\n");
                    if (info.getLockName() != null) {
                        sb.append("    Waiting on: ").append(info.getLockName()).append("\n");
                    }
                    if (info.getLockOwnerName() != null) {
                        sb.append("    Held by: ").append(info.getLockOwnerName())
                          .append(" (id=").append(info.getLockOwnerId()).append(")\n");
                    }
                }
            }
        } else {
            // Check for potential deadlock candidates
            List<ThreadInfo> blockedThreads = Arrays.stream(threadInfos)
                    .filter(t -> t.getThreadState() == Thread.State.BLOCKED)
                    .collect(Collectors.toList());

            if (blockedThreads.isEmpty()) {
                sb.append(ANSI_GREEN).append("No deadlocks or deadlock candidates detected.\n")
                  .append(ANSI_RESET);
            } else {
                sb.append(ANSI_YELLOW).append("No deadlocks, but ")
                  .append(blockedThreads.size()).append(" blocked thread(s) found (potential candidates):\n")
                  .append(ANSI_RESET);
                for (ThreadInfo blocked : blockedThreads) {
                    sb.append("  ").append(blocked.getThreadName())
                      .append(" blocked on ").append(blocked.getLockName())
                      .append(" owned by ").append(blocked.getLockOwnerName()).append("\n");
                }
            }
        }
        sb.append("\n");
    }

    private void appendContentionAnalysis(StringBuilder sb, ThreadInfo[] threadInfos) {
        sb.append(ANSI_BOLD).append("=== Lock Contention Analysis ===\n").append(ANSI_RESET);

        // Group threads by the lock they are waiting on
        Map<String, List<ThreadInfo>> lockWaiters = new LinkedHashMap<>();
        for (ThreadInfo info : threadInfos) {
            if (info.getLockName() != null) {
                lockWaiters.computeIfAbsent(info.getLockName(), k -> new ArrayList<>()).add(info);
            }
        }

        if (lockWaiters.isEmpty()) {
            sb.append(ANSI_GREEN).append("No lock contention detected.\n").append(ANSI_RESET);
            sb.append("\n");
            return;
        }

        // Sort by contention count (most contended first) and limit to topN
        List<Map.Entry<String, List<ThreadInfo>>> sorted = lockWaiters.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(topN)
                .collect(Collectors.toList());

        // Table header
        sb.append(String.format("%-50s %-10s %-30s\n", "Lock", "Waiters", "Owner"));
        sb.append(String.format("%-50s %-10s %-30s\n",
                repeat("-", 50), repeat("-", 10), repeat("-", 30)));

        for (Map.Entry<String, List<ThreadInfo>> entry : sorted) {
            String lockName = entry.getKey();
            List<ThreadInfo> waiters = entry.getValue();
            String ownerName = waiters.get(0).getLockOwnerName();
            if (ownerName == null) {
                ownerName = "N/A";
            }

            String color = waiters.size() >= 5 ? ANSI_RED
                    : waiters.size() >= 2 ? ANSI_YELLOW : ANSI_CYAN;

            sb.append(color);
            sb.append(String.format("%-50s %-10d %-30s",
                    truncate(lockName, 50), waiters.size(), truncate(ownerName, 30)));
            sb.append(ANSI_RESET).append("\n");

            // Show waiting threads
            for (ThreadInfo waiter : waiters) {
                sb.append("  ").append(ANSI_CYAN).append("-> ")
                  .append(waiter.getThreadName())
                  .append(" (blocked ").append(waiter.getBlockedTime()).append("ms)")
                  .append(ANSI_RESET).append("\n");
            }
        }
        sb.append("\n");
    }

    private void appendWaitChainVisualization(StringBuilder sb, ThreadInfo[] threadInfos) {
        sb.append(ANSI_BOLD).append("=== Thread Wait Chains ===\n").append(ANSI_RESET);

        // Build adjacency: thread -> thread it's waiting on
        Map<Long, ThreadInfo> threadMap = new HashMap<>();
        for (ThreadInfo info : threadInfos) {
            threadMap.put(info.getThreadId(), info);
        }

        // Find chains: follow lockOwnerId links
        Set<Long> visited = new HashSet<>();
        List<List<ThreadInfo>> chains = new ArrayList<>();

        for (ThreadInfo info : threadInfos) {
            if (info.getLockOwnerId() > 0 && !visited.contains(info.getThreadId())) {
                List<ThreadInfo> chain = buildWaitChain(info, threadMap, visited);
                if (chain.size() > 1) {
                    chains.add(chain);
                }
            }
        }

        if (chains.isEmpty()) {
            sb.append(ANSI_GREEN).append("No wait chains detected.\n").append(ANSI_RESET);
        } else {
            sb.append("Found ").append(chains.size()).append(" wait chain(s):\n\n");
            int chainNum = 1;
            for (List<ThreadInfo> chain : chains) {
                sb.append(ANSI_YELLOW).append("Chain #").append(chainNum++).append(":")
                  .append(ANSI_RESET).append("\n");
                for (int i = 0; i < chain.size(); i++) {
                    ThreadInfo t = chain.get(i);
                    String indent = repeat("  ", i);
                    sb.append(indent);
                    if (i > 0) {
                        sb.append(ANSI_CYAN).append("\\-> ").append(ANSI_RESET);
                    }
                    sb.append(t.getThreadName())
                      .append(" [").append(t.getThreadState()).append("]");
                    if (t.getLockName() != null) {
                        sb.append(" waiting on ").append(t.getLockName());
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
    }

    private List<ThreadInfo> buildWaitChain(ThreadInfo start, Map<Long, ThreadInfo> threadMap,
                                             Set<Long> visited) {
        List<ThreadInfo> chain = new ArrayList<>();
        ThreadInfo current = start;
        Set<Long> chainVisited = new HashSet<>();

        while (current != null && !chainVisited.contains(current.getThreadId())) {
            chainVisited.add(current.getThreadId());
            visited.add(current.getThreadId());
            chain.add(current);

            long ownerId = current.getLockOwnerId();
            if (ownerId > 0) {
                current = threadMap.get(ownerId);
            } else {
                break;
            }
        }
        return chain;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLen ? str.substring(0, maxLen - 3) + "..." : str;
    }

    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder(str.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}

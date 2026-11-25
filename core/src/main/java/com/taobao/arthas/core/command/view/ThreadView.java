package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.BusyThreadInfo;
import com.taobao.arthas.core.command.model.DeadlockInfo;
import com.taobao.arthas.core.command.model.ThreadModel;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.util.RenderUtil;

import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;


/**
 * View of 'thread' command
 *
 * @author gongdewei 2020/4/26
 */
public class ThreadView extends ResultView<ThreadModel> {

    @Override
    public void draw(CommandProcess process, ThreadModel result) {
        if (result.getThreadInfo() != null) {
            // no cpu usage info
            String content = ThreadUtil.getFullStacktrace(result.getThreadInfo());
            process.write(content);
        } else if (result.getBusyThreads() != null) {
            List<BusyThreadInfo> threadInfos = result.getBusyThreads();
            for (BusyThreadInfo info : threadInfos) {
                String stacktrace = ThreadUtil.getFullStacktrace(info, -1, -1);
                process.write(stacktrace).write("\n");
            }
        } else if (result.getBlockingLockInfo() != null) {
            String stacktrace = ThreadUtil.getFullStacktrace(result.getBlockingLockInfo());
            process.write(stacktrace);

        } else if (result.getThreadStateCount() != null) {
            Map<Thread.State, Integer> threadStateCount = result.getThreadStateCount();
            List<ThreadVO> threadStats = result.getThreadStats();

            //sum total thread count
            int total = 0;
            for (Integer value : threadStateCount.values()) {
                total += value;
            }

            int internalThreadCount = 0;
            for (ThreadVO thread : threadStats) {
                if (thread.getId() <= 0) {
                    internalThreadCount += 1;
                }
            }
            total += internalThreadCount;

            StringBuilder threadStat = new StringBuilder();
            threadStat.append("Threads Total: ").append(total);

            for (Thread.State s : Thread.State.values()) {
                Integer count = threadStateCount.get(s);
                threadStat.append(", ").append(s.name()).append(": ").append(count);
            }
            if (internalThreadCount > 0) {
                threadStat.append(", Internal threads: ").append(internalThreadCount);
            }
            String stat = RenderUtil.render(new LabelElement(threadStat), process.width());

            //thread stats
            int height;
            if (result.isAll()) {
                height = threadStats.size() + 1;
            } else {
                height = Math.max(5, process.height() - 2);
                //remove blank lines
                height = Math.min(height, threadStats.size() + 2);
            }
            String content = ViewRenderUtil.drawThreadInfo(threadStats, process.width(), height);
            process.write(stat + content);
        } else if(result.getDeadlockInfo() != null){
            StringBuilder sb = new StringBuilder();

            DeadlockInfo deadlockInfo = result.getDeadlockInfo();
            Map<Integer, ThreadInfo> ownerThreadPerLock = deadlockInfo.getOwnerThreadPerLock();

            for(ThreadInfo thread : deadlockInfo.getThreads()){
                LockInfo waitingToLockInfo;
                ThreadInfo currentThread = thread;
                sb.append("Found one Java-level deadlock:\n");
                sb.append("=============================\n");
                do{
                    sb.append(currentThread.getThreadName() + "(" + currentThread.getThreadId() + "):\n");
                    waitingToLockInfo = currentThread.getLockInfo();
                    if(waitingToLockInfo != null){
                        sb.append("  waiting to lock info @" + waitingToLockInfo + ",\n");
                        sb.append("  which is held by ");

                        currentThread = ownerThreadPerLock.get(waitingToLockInfo.getIdentityHashCode());
                        sb.append(currentThread.getThreadName() + "\n");
                    }
                }while (!currentThread.equals(thread));
                sb.append("\n");
            }

            int numberOfDeadlocks = deadlockInfo.getThreads().size();
            switch (numberOfDeadlocks) {
                case 0:
                    sb.append("No deadlocks found.\n");
                    break;
                case 1:
                    sb.append("Found a total of 1 deadlock.\n");
                    break;
                default:
                    sb.append("Found a total of ").append(numberOfDeadlocks).append(" deadlocks.\n");
                    break;
            }
            sb.append("\n");
            process.write(sb.toString());
        }
    }
}

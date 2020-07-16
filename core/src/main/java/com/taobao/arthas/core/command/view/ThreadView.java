package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ThreadCpuInfo;
import com.taobao.arthas.core.command.model.ThreadModel;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.util.RenderUtil;

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
            String content = ThreadUtil.getFullStacktrace(result.getThreadInfo(), -1);
            process.write(content);
        } else if (result.getBusyThreads() != null) {
            ThreadCpuInfo[] threadInfos = result.getBusyThreads();
            for (ThreadCpuInfo info : threadInfos) {
                String stacktrace = ThreadUtil.getFullStacktrace(info.threadInfo(), info.getCpuUsage());
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
            StringBuilder threadStat = new StringBuilder();
            threadStat.append("Threads Total: ").append(total);

            for (Thread.State s : Thread.State.values()) {
                Integer count = threadStateCount.get(s);
                threadStat.append(", ").append(s.name()).append(": ").append(count);
            }
            String stat = RenderUtil.render(new LabelElement(threadStat), process.width());

            //thread stats
            int height = Math.max(5, process.height() - 2);
            //remove blank lines
            height = Math.min(height, threadStats.size() + 2);
            String content = ViewRenderUtil.drawThreadInfo(threadStats, process.width(), height);
            process.write(stat + content);
        }
    }
}

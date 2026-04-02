package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.BusyThreadInfo;
import com.taobao.arthas.core.command.model.ThreadModel;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;
import java.util.Map;


/**
 * 线程命令的视图类
 * 负责将线程相关信息（单线程、繁忙线程、阻塞锁、线程统计等）渲染为易读的文本格式
 *
 * @author gongdewei 2020/4/26
 */
public class ThreadView extends ResultView<ThreadModel> {

    /**
     * 绘制线程信息视图
     * 根据不同的命令参数类型，显示不同的线程信息
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 线程模型对象，包含各种线程信息
     */
    @Override
    public void draw(CommandProcess process, ThreadModel result) {
        // 场景1: 显示单个线程的详细信息（无CPU使用率信息）
        if (result.getThreadInfo() != null) {
            // 获取线程的完整堆栈跟踪信息
            String content = ThreadUtil.getFullStacktrace(result.getThreadInfo());
            process.write(content);
        // 场景2: 显示繁忙线程列表
        } else if (result.getBusyThreads() != null) {
            List<BusyThreadInfo> threadInfos = result.getBusyThreads();
            // 遍历每个繁忙线程，输出其堆栈信息
            for (BusyThreadInfo info : threadInfos) {
                String stacktrace = ThreadUtil.getFullStacktrace(info, -1, -1);
                process.write(stacktrace).write("\n");
            }
        // 场景3: 显示阻塞锁信息
        } else if (result.getBlockingLockInfo() != null) {
            // 获取阻塞锁相关的完整堆栈信息
            String stacktrace = ThreadUtil.getFullStacktrace(result.getBlockingLockInfo());
            process.write(stacktrace);

        // 场景4: 显示线程统计信息（线程状态汇总）
        } else if (result.getThreadStateCount() != null) {
            Map<Thread.State, Integer> threadStateCount = result.getThreadStateCount();
            List<ThreadVO> threadStats = result.getThreadStats();

            // 统计线程总数
            int total = 0;
            for (Integer value : threadStateCount.values()) {
                total += value;
            }

            // 统计内部线程数量（ID <= 0的线程为内部线程）
            int internalThreadCount = 0;
            for (ThreadVO thread : threadStats) {
                if (thread.getId() <= 0) {
                    internalThreadCount += 1;
                }
            }
            total += internalThreadCount;

            // 构建线程统计摘要信息
            StringBuilder threadStat = new StringBuilder();
            threadStat.append("Threads Total: ").append(total);

            // 添加各状态的线程数量（NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED）
            for (Thread.State s : Thread.State.values()) {
                Integer count = threadStateCount.get(s);
                threadStat.append(", ").append(s.name()).append(": ").append(count);
            }
            // 如果存在内部线程，单独显示其数量
            if (internalThreadCount > 0) {
                threadStat.append(", Internal threads: ").append(internalThreadCount);
            }
            // 渲染统计摘要为标签元素
            String stat = RenderUtil.render(new LabelElement(threadStat), process.width());

            // 计算线程列表显示的高度
            int height;
            if (result.isAll()) {
                // 如果显示全部线程，高度为线程数量+1（表头）
                height = threadStats.size() + 1;
            } else {
                // 否则使用终端高度-2，最少显示5行
                height = Math.max(5, process.height() - 2);
                // 移除空行，高度不超过线程数量+2
                height = Math.min(height, threadStats.size() + 2);
            }
            // 绘制线程详细信息表格
            String content = ViewRenderUtil.drawThreadInfo(threadStats, process.width(), height);
            process.write(stat + content);
        }
    }
}

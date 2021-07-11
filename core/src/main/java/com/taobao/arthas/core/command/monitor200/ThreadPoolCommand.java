package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 监控线程池的主要数据
 * 包括线程池execute方法调用栈信息（用来判断是那个业务线程池）、配置的核心线程数、配置的最大线程数、当前繁忙线程数、队列堆积数
 *
 *
 * @author HJ
 * @date 2021-07-08
 **/
@Name("threadpool")
@Summary("Display thread pool info")
@Description(Constants.EXAMPLE +
        "  threadpool -n 5"+  // 输出5个线程池信息，优先按繁忙线程数排序，其次按最大线程数排序
        "  threadpool -sd 3"+ // 输出3行调用栈信息，用于判断是哪个业务线程池
        "  threadpool -i 2000" // 采集2000毫秒内，有提交过任务的线程池的信息

)
public class ThreadPoolCommand extends EnhancerCommand {

    /**
     * 采样时间，默认1秒，即：记录1秒内有提交任务动作的线程池
     */
    private Integer sampleInterval = 1000;
    /**
     * 默认打印2个栈信息，这样有助于从堆栈里判断是哪个地方的线程池
     */
    private Integer stackTraceDepth = 2;
    /**
     * 默认展示所有线程池，按繁忙线程数排序
     */
    private Integer topNActiveThreadCount = -1;

    @Option(shortName = "i", longName = "sample-interval")
    @Description("Specify the sampling interval (in ms) ")
    public void setSampleInterval(int sampleInterval) {
        this.sampleInterval = sampleInterval;
    }


    @Option(shortName = "sd", longName = "stack-depth")
    @Description("Display the stack info of specified depth")
    public void setStackTraceDepth(int stackTraceDepth) {
        this.stackTraceDepth = stackTraceDepth;
    }


    @Option(shortName = "n", longName = "top-n-threadpools")
    @Description("The number of thread pool(s) to show, ordered by activeThreadCount, Show all by default")
    public void setTopNBusy(Integer topNActiveThreadCount) {
        this.topNActiveThreadCount = topNActiveThreadCount;
    }

    public ThreadPoolCommand() {
    }

    @Override
    protected Matcher<String> getClassNameMatcher() {
        // 指定ThreadPoolExecutor类
        return SearchUtils.classNameMatcher("java.util.concurrent.ThreadPoolExecutor", false);
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        return null;
    }

    @Override
    protected Matcher<String> getMethodNameMatcher() {
        // 指定任务提交的方法
        return SearchUtils.classNameMatcher("execute", false);
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new ThreadPoolAdviceListener(this, process);
    }

    Integer getSampleInterval() {
        return sampleInterval;
    }

    Integer getStackTraceDepth() {
        return stackTraceDepth;
    }

    Integer getTopNActiveThreadCount() {
        return topNActiveThreadCount;
    }
}

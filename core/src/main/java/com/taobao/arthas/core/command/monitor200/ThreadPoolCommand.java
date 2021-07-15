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
        "  threadpool -n 5"+  // 输出5个线程池信息，优先按繁忙线程数排序，其次按队列堆积数排序，最后按最大线程数排序
        "  threadpool -sd 3"+ // 输出3行调用栈信息，用于判断是哪个业务线程池
        "  threadpool -d 2000"+ // 指令监控时长2000ms，在2000ms内会按照指定的频率采集线程池的【当前繁忙线程数】和【队列堆积数】数据，最后输出平均值
        "  threadpool -i 100" // 采样间隔100毫秒，每隔100毫秒会采集线程池的【当前繁忙线程数】和【队列堆积数】数据，最后输出平均值

)
public class ThreadPoolCommand extends EnhancerCommand {

    /**
     * 命令执行时长，默认1000毫秒
     */
    private Integer duration = 1000;
    /**
     * 采样时长，在执行时间内，每隔指定时间采样线程池信息，最后输出平均值，默认200毫秒
     */
    private Integer sampleInterval = 200;
    /**
     * 默认打印2行栈信息，这样有助于从堆栈里判断是哪个地方的线程池
     */
    private Integer stackTraceDepth = 2;
    /**
     * 默认展示所有线程池，按繁忙线程数排序
     */
    private Integer topNActiveThreadCount = -1;

    @Option(shortName = "i", longName = "sample-interval")
    @Description("Specify the sampling interval (in ms) ,default value is 200")
    public void setSampleInterval(int sampleInterval) {
        if (sampleInterval <= 0) {
            throw new IllegalArgumentException("i must be positive");
        }
        this.sampleInterval = sampleInterval;
    }

    @Option(shortName = "d", longName = "duration")
    @Description("run threadpool for <duration> ms,default value is 1000")
    public void setDuration(int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("d must be positive");
        }
        this.duration = duration;
    }


    @Option(shortName = "sd", longName = "stack-depth")
    @Description("Display the stack info of specified depth,default value is 2")
    public void setStackTraceDepth(int stackTraceDepth) {
        this.stackTraceDepth = stackTraceDepth;
    }


    @Option(shortName = "n", longName = "top-n-threadpools")
    @Description("The number of thread pool(s) to show, ordered by activeThreadCount, Show all by default")
    public void setTopNActiveThreadCount(Integer topNActiveThreadCount) {
        if (topNActiveThreadCount <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        this.topNActiveThreadCount = topNActiveThreadCount;
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

    public Integer getDuration() {
        return duration;
    }
}

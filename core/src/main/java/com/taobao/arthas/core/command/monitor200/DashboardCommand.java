package com.taobao.arthas.core.command.monitor200;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.DashboardModel;
import com.taobao.arthas.core.command.model.GcInfoVO;
import com.taobao.arthas.core.command.model.RuntimeInfoVO;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.command.model.TomcatInfoVO;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.NetUtils;
import com.taobao.arthas.core.util.NetUtils.Response;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.arthas.core.util.metrics.SumRateCounter;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Dashboard命令
 * 实时展示目标JVM的线程、内存、GC、VM运行时和Tomcat等信息
 * 通过定时任务周期性采集数据并展示
 *
 * @author hengyunabc 2015年11月19日 上午11:57:21
 */
@Name("dashboard")
@Summary("Overview of target jvm's thread, memory, gc, vm, tomcat info.")
@Description(Constants.EXAMPLE +
        "  dashboard\n" +
        "  dashboard -n 10\n" +
        "  dashboard -i 2000\n" +
        Constants.WIKI + Constants.WIKI_HOME + "dashboard")
public class DashboardCommand extends AnnotatedCommand {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(DashboardCommand.class);

    // Tomcat请求计数器，用于计算QPS
    private SumRateCounter tomcatRequestCounter = new SumRateCounter();

    // Tomcat错误计数器，用于计算错误率
    private SumRateCounter tomcatErrorCounter = new SumRateCounter();

    // Tomcat接收字节数计数器，用于计算接收速率
    private SumRateCounter tomcatReceivedBytesCounter = new SumRateCounter();

    // Tomcat发送字节数计数器，用于计算发送速率
    private SumRateCounter tomcatSentBytesCounter = new SumRateCounter();

    // 执行次数限制，默认为Integer.MAX_VALUE（无限制）
    private int numOfExecutions = Integer.MAX_VALUE;

    // 执行间隔（毫秒），默认为5秒
    private long interval = 5000;

    // 执行计数器，记录已执行的次数
    private final AtomicLong count = new AtomicLong(0);

    // 定时器，用于周期性执行仪表盘任务
    private volatile Timer timer;

    /**
     * 设置执行次数限制
     *
     * @param numOfExecutions 执行次数
     */
    @Option(shortName = "n", longName = "number-of-execution")
    @Description("The number of times this command will be executed.")
    public void setNumOfExecutions(int numOfExecutions) {
        this.numOfExecutions = numOfExecutions;
    }

    /**
     * 设置执行间隔
     *
     * @param interval 执行间隔（毫秒）
     */
    @Option(shortName = "i", longName = "interval")
    @Description("The interval (in ms) between two executions, default is 5000 ms.")
    public void setInterval(long interval) {
        this.interval = interval;
    }


    /**
     * 处理命令执行
     * 初始化定时器和各种处理器，开始周期性采集和展示数据
     *
     * @param process 命令进程
     */
    @Override
    public void process(final CommandProcess process) {
        // 获取当前会话
        Session session = process.session();

        // 创建定时器，命名为"Timer-for-arthas-dashboard-<sessionId>"，设置为守护线程
        timer = new Timer("Timer-for-arthas-dashboard-" + session.getSessionId(), true);

        // 设置中断处理器，支持Ctrl+C中断
        process.interruptHandler(new DashboardInterruptHandler(process, timer));

        /*
         * 通过handle回调，在suspend和end时停止timer，resume时重启timer
         * 这样可以在进程暂停时停止数据采集，恢复时重新开始采集
         */
        // 停止处理器：在暂停或结束时调用
        Handler<Void> stopHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                stop();
            }
        };

        // 重启处理器：在恢复时调用
        Handler<Void> restartHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                restart(process);
            }
        };

        // 注册各种处理器
        process.suspendHandler(stopHandler);
        process.resumeHandler(restartHandler);
        process.endHandler(stopHandler);

        // 设置Q退出支持，输入q可以退出
        process.stdinHandler(new QExitHandler(process));

        // 启动定时器，立即开始执行，按指定间隔周期性执行
        timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval());
    }

    /**
     * 停止定时器
     * 取消并清理定时器任务
     */
    public synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * 重启定时器
     * 如果定时器已停止，则创建新的定时器并开始执行
     *
     * @param process 命令进程
     */
    public synchronized void restart(CommandProcess process) {
        // 只有在定时器为null时才重新创建
        if (timer == null) {
            Session session = process.session();
            timer = new Timer("Timer-for-arthas-dashboard-" + session.getSessionId(), true);
            timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval());
        }
    }

    /**
     * 获取执行次数限制
     *
     * @return 执行次数
     */
    public int getNumOfExecutions() {
        return numOfExecutions;
    }

    /**
     * 获取执行间隔
     *
     * @return 执行间隔（毫秒）
     */
    public long getInterval() {
        return interval;
    }

    /**
     * 添加运行时信息到仪表盘模型
     * 包括操作系统信息、Java版本、系统负载、处理器数量、JVM运行时间等
     *
     * @param dashboardModel 仪表盘模型
     */
    private static void addRuntimeInfo(DashboardModel dashboardModel) {
        RuntimeInfoVO runtimeInfo = new RuntimeInfoVO();
        // 设置操作系统名称
        runtimeInfo.setOsName(System.getProperty("os.name"));
        // 设置操作系统版本
        runtimeInfo.setOsVersion(System.getProperty("os.version"));
        // 设置Java版本
        runtimeInfo.setJavaVersion(System.getProperty("java.version"));
        // 设置Java安装路径
        runtimeInfo.setJavaHome(System.getProperty("java.home"));
        // 设置系统平均负载
        runtimeInfo.setSystemLoadAverage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        // 设置可用处理器数量
        runtimeInfo.setProcessors(Runtime.getRuntime().availableProcessors());
        // 设置JVM运行时间（秒）
        runtimeInfo.setUptime(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        // 设置当前时间戳
        runtimeInfo.setTimestamp(System.currentTimeMillis());
        // 将运行时信息设置到仪表盘模型
        dashboardModel.setRuntimeInfo(runtimeInfo);
    }

    /**
     * 添加GC信息到仪表盘模型
     * 获取所有垃圾回收器的统计信息，包括收集次数和收集时间
     *
     * @param dashboardModel 仪表盘模型
     */
    private static void addGcInfo(DashboardModel dashboardModel) {
        // 创建GC信息列表
        List<GcInfoVO> gcInfos = new ArrayList<GcInfoVO>();
        dashboardModel.setGcInfos(gcInfos);

        // 获取所有垃圾回收器MXBean
        List<GarbageCollectorMXBean> garbageCollectorMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcMXBean : garbageCollectorMxBeans) {
            String name = gcMXBean.getName();
            // 美化GC名称并创建GC信息对象，包含收集次数和收集时间
            gcInfos.add(new GcInfoVO(StringUtils.beautifyName(name), gcMXBean.getCollectionCount(), gcMXBean.getCollectionTime()));
        }
    }

    /**
     * 添加Tomcat信息到仪表盘模型
     * 通过Tomcat管理接口获取连接器统计信息和线程池信息
     *
     * @param dashboardModel 仪表盘模型
     */
    private void addTomcatInfo(DashboardModel dashboardModel) {
        // 首先检查Tomcat管理接口是否可用（默认端口8006）
        // 如果请求tomcat信息失败，则不显示tomcat信息
        if (!NetUtils.request("http://localhost:8006").isSuccess()) {
            return;
        }

        // 创建Tomcat信息对象
        TomcatInfoVO tomcatInfoVO = new TomcatInfoVO();
        dashboardModel.setTomcatInfo(tomcatInfoVO);

        // 定义Tomcat管理接口的路径
        String threadPoolPath = "http://localhost:8006/connector/threadpool";
        String connectorStatPath = "http://localhost:8006/connector/stats";

        // 获取连接器统计信息
        Response connectorStatResponse = NetUtils.request(connectorStatPath);
        if (connectorStatResponse.isSuccess()) {
            List<TomcatInfoVO.ConnectorStats> connectorStats = new ArrayList<TomcatInfoVO.ConnectorStats>();
            // 解析JSON响应
            List<JSONObject> tomcatConnectorStats = JSON.parseArray(connectorStatResponse.getContent(), JSONObject.class);
            for (JSONObject stat : tomcatConnectorStats) {
                // 提取连接器名称
                String connectorName = stat.getString("name").replace("\"", "");
                // 提取接收字节数
                long bytesReceived = stat.getLongValue("bytesReceived");
                // 提取发送字节数
                long bytesSent = stat.getLongValue("bytesSent");
                // 提取处理时间
                long processingTime = stat.getLongValue("processingTime");
                // 提取请求总数
                long requestCount = stat.getLongValue("requestCount");
                // 提取错误总数
                long errorCount = stat.getLongValue("errorCount");

                // 更新计数器，用于计算速率
                tomcatRequestCounter.update(requestCount);
                tomcatErrorCounter.update(errorCount);
                tomcatReceivedBytesCounter.update(bytesReceived);
                tomcatSentBytesCounter.update(bytesSent);

                // 计算QPS（每秒请求数）
                double qps = tomcatRequestCounter.rate();
                // 计算平均响应时间（RT）
                double rt = processingTime / (double) requestCount;
                // 计算错误率
                double errorRate = tomcatErrorCounter.rate();
                // 计算接收字节速率
                long receivedBytesRate = Double.valueOf(tomcatReceivedBytesCounter.rate()).longValue();
                // 计算发送字节速率
                long sentBytesRate = Double.valueOf(tomcatSentBytesCounter.rate()).longValue();

                // 创建连接器统计对象并设置各项指标
                TomcatInfoVO.ConnectorStats connectorStat = new TomcatInfoVO.ConnectorStats();
                connectorStat.setName(connectorName);
                connectorStat.setQps(qps);
                connectorStat.setRt(rt);
                connectorStat.setError(errorRate);
                connectorStat.setReceived(receivedBytesRate);
                connectorStat.setSent(sentBytesRate);
                connectorStats.add(connectorStat);
            }
            // 将连接器统计信息设置到Tomcat信息对象
            tomcatInfoVO.setConnectorStats(connectorStats);
        }

        // 获取线程池信息
        Response threadPoolResponse = NetUtils.request(threadPoolPath);
        if (threadPoolResponse.isSuccess()) {
            List<TomcatInfoVO.ThreadPool> threadPools = new ArrayList<TomcatInfoVO.ThreadPool>();
            // 解析JSON响应
            List<JSONObject> threadPoolInfos = JSON.parseArray(threadPoolResponse.getContent(), JSONObject.class);
            for (JSONObject info : threadPoolInfos) {
                // 提取线程池名称
                String name = info.getString("name").replace("\"", "");
                // 提取繁忙线程数
                long busy = info.getLongValue("threadBusy");
                // 提取线程总数
                long total = info.getLongValue("threadCount");
                // 创建线程池信息对象
                threadPools.add(new TomcatInfoVO.ThreadPool(name, busy, total));
            }
            // 将线程池信息设置到Tomcat信息对象
            tomcatInfoVO.setThreadPools(threadPools);
        }
    }

    /**
     * 仪表盘定时任务
     * 周期性采集JVM的各项指标并输出到控制台
     */
    private class DashboardTimerTask extends TimerTask {
        // 命令进程，用于输出结果
        private CommandProcess process;
        // 线程采样器，用于采集线程信息
        private ThreadSampler threadSampler;

        /**
         * 构造函数
         *
         * @param process 命令进程
         */
        public DashboardTimerTask(CommandProcess process) {
            this.process = process;
            this.threadSampler = new ThreadSampler();
        }

        /**
         * 定时任务执行方法
         * 采集各项指标并输出
         */
        @Override
        public void run() {
            try {
                // 检查是否达到执行次数限制
                if (count.get() >= getNumOfExecutions()) {
                    // 停止定时器
                    timer.cancel();
                    timer.purge();
                    // 结束进程并返回成功状态
                    process.end(0, "Process ends after " + getNumOfExecutions() + " time(s).");
                    return;
                }

                // 创建仪表盘模型对象
                DashboardModel dashboardModel = new DashboardModel();

                // 采集线程信息
                List<ThreadVO> threads = ThreadUtil.getThreads();
                dashboardModel.setThreads(threadSampler.sample(threads));

                // 采集内存信息
                dashboardModel.setMemoryInfo(MemoryCommand.memoryInfo());

                // 采集GC信息
                addGcInfo(dashboardModel);

                // 采集运行时信息
                addRuntimeInfo(dashboardModel);

                // 采集Tomcat信息（如果可用）
                try {
                    addTomcatInfo(dashboardModel);
                } catch (Throwable e) {
                    // 如果获取Tomcat信息失败，只记录错误，不影响其他信息的展示
                    logger.error("try to read tomcat info error", e);
                }

                // 将仪表盘模型添加到进程输出
                process.appendResult(dashboardModel);

                // 增加执行计数
                count.getAndIncrement();
                process.times().incrementAndGet();
            } catch (Throwable e) {
                // 如果发生异常，记录错误并结束进程
                String msg = "process dashboard failed: " + e.getMessage();
                logger.error(msg, e);
                process.end(-1, msg);
            }
        }
    }

}

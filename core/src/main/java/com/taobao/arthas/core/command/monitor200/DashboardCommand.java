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

    private static final Logger logger = LoggerFactory.getLogger(DashboardCommand.class);

    private SumRateCounter tomcatRequestCounter = new SumRateCounter();
    private SumRateCounter tomcatErrorCounter = new SumRateCounter();
    private SumRateCounter tomcatReceivedBytesCounter = new SumRateCounter();
    private SumRateCounter tomcatSentBytesCounter = new SumRateCounter();

    private int numOfExecutions = Integer.MAX_VALUE;

    private long interval = 5000;

    private final AtomicLong count = new AtomicLong(0);
    private volatile Timer timer;

    @Option(shortName = "n", longName = "number-of-execution")
    @Description("The number of times this command will be executed.")
    public void setNumOfExecutions(int numOfExecutions) {
        this.numOfExecutions = numOfExecutions;
    }

    @Option(shortName = "i", longName = "interval")
    @Description("The interval (in ms) between two executions, default is 5000 ms.")
    public void setInterval(long interval) {
        this.interval = interval;
    }


    @Override
    public void process(final CommandProcess process) {

        Session session = process.session();
        timer = new Timer("Timer-for-arthas-dashboard-" + session.getSessionId(), true);

        // ctrl-C support
        process.interruptHandler(new DashboardInterruptHandler(process, timer));

        /*
         * 通过handle回调，在suspend和end时停止timer，resume时重启timer
         */
        Handler<Void> stopHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                stop();
            }
        };

        Handler<Void> restartHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                restart(process);
            }
        };
        process.suspendHandler(stopHandler);
        process.resumeHandler(restartHandler);
        process.endHandler(stopHandler);

        // q exit support
        process.stdinHandler(new QExitHandler(process));

        // start the timer
        timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval());
    }

    public synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public synchronized void restart(CommandProcess process) {
        if (timer == null) {
            Session session = process.session();
            timer = new Timer("Timer-for-arthas-dashboard-" + session.getSessionId(), true);
            timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval());
        }
    }

    public int getNumOfExecutions() {
        return numOfExecutions;
    }

    public long getInterval() {
        return interval;
    }

    private static void addRuntimeInfo(DashboardModel dashboardModel) {
        RuntimeInfoVO runtimeInfo = new RuntimeInfoVO();
        runtimeInfo.setOsName(System.getProperty("os.name"));
        runtimeInfo.setOsVersion(System.getProperty("os.version"));
        runtimeInfo.setJavaVersion(System.getProperty("java.version"));
        runtimeInfo.setJavaHome(System.getProperty("java.home"));
        runtimeInfo.setSystemLoadAverage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        runtimeInfo.setProcessors(Runtime.getRuntime().availableProcessors());
        runtimeInfo.setUptime(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        runtimeInfo.setTimestamp(System.currentTimeMillis());
        dashboardModel.setRuntimeInfo(runtimeInfo);
    }

    private static void addGcInfo(DashboardModel dashboardModel) {
        List<GcInfoVO> gcInfos = new ArrayList<GcInfoVO>();
        dashboardModel.setGcInfos(gcInfos);

        List<GarbageCollectorMXBean> garbageCollectorMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcMXBean : garbageCollectorMxBeans) {
            String name = gcMXBean.getName();
            gcInfos.add(new GcInfoVO(StringUtils.beautifyName(name), gcMXBean.getCollectionCount(), gcMXBean.getCollectionTime()));
        }
    }

    private void addTomcatInfo(DashboardModel dashboardModel) {
        // 如果请求tomcat信息失败，则不显示tomcat信息
        if (!NetUtils.request("http://localhost:8006").isSuccess()) {
            return;
        }

        TomcatInfoVO tomcatInfoVO = new TomcatInfoVO();
        dashboardModel.setTomcatInfo(tomcatInfoVO);
        String threadPoolPath = "http://localhost:8006/connector/threadpool";
        String connectorStatPath = "http://localhost:8006/connector/stats";
        Response connectorStatResponse = NetUtils.request(connectorStatPath);
        if (connectorStatResponse.isSuccess()) {
            List<TomcatInfoVO.ConnectorStats> connectorStats = new ArrayList<TomcatInfoVO.ConnectorStats>();
            List<JSONObject> tomcatConnectorStats = JSON.parseArray(connectorStatResponse.getContent(), JSONObject.class);
            for (JSONObject stat : tomcatConnectorStats) {
                String connectorName = stat.getString("name").replace("\"", "");
                long bytesReceived = stat.getLongValue("bytesReceived");
                long bytesSent = stat.getLongValue("bytesSent");
                long processingTime = stat.getLongValue("processingTime");
                long requestCount = stat.getLongValue("requestCount");
                long errorCount = stat.getLongValue("errorCount");

                tomcatRequestCounter.update(requestCount);
                tomcatErrorCounter.update(errorCount);
                tomcatReceivedBytesCounter.update(bytesReceived);
                tomcatSentBytesCounter.update(bytesSent);

                double qps = tomcatRequestCounter.rate();
                double rt = processingTime / (double) requestCount;
                double errorRate = tomcatErrorCounter.rate();
                long receivedBytesRate = Double.valueOf(tomcatReceivedBytesCounter.rate()).longValue();
                long sentBytesRate = Double.valueOf(tomcatSentBytesCounter.rate()).longValue();

                TomcatInfoVO.ConnectorStats connectorStat = new TomcatInfoVO.ConnectorStats();
                connectorStat.setName(connectorName);
                connectorStat.setQps(qps);
                connectorStat.setRt(rt);
                connectorStat.setError(errorRate);
                connectorStat.setReceived(receivedBytesRate);
                connectorStat.setSent(sentBytesRate);
                connectorStats.add(connectorStat);
            }
            tomcatInfoVO.setConnectorStats(connectorStats);
        }

        Response threadPoolResponse = NetUtils.request(threadPoolPath);
        if (threadPoolResponse.isSuccess()) {
            List<TomcatInfoVO.ThreadPool> threadPools = new ArrayList<TomcatInfoVO.ThreadPool>();
            List<JSONObject> threadPoolInfos = JSON.parseArray(threadPoolResponse.getContent(), JSONObject.class);
            for (JSONObject info : threadPoolInfos) {
                String name = info.getString("name").replace("\"", "");
                long busy = info.getLongValue("threadBusy");
                long total = info.getLongValue("threadCount");
                threadPools.add(new TomcatInfoVO.ThreadPool(name, busy, total));
            }
            tomcatInfoVO.setThreadPools(threadPools);
        }
    }

    private class DashboardTimerTask extends TimerTask {
        private CommandProcess process;
        private ThreadSampler threadSampler;

        public DashboardTimerTask(CommandProcess process) {
            this.process = process;
            this.threadSampler = new ThreadSampler();
        }

        @Override
        public void run() {
            try {
                if (count.get() >= getNumOfExecutions()) {
                    // stop the timer
                    timer.cancel();
                    timer.purge();
                    process.end(0, "Process ends after " + getNumOfExecutions() + " time(s).");
                    return;
                }

                DashboardModel dashboardModel = new DashboardModel();

                //thread sample
                List<ThreadVO> threads = ThreadUtil.getThreads();
                dashboardModel.setThreads(threadSampler.sample(threads));

                //memory
                dashboardModel.setMemoryInfo(MemoryCommand.memoryInfo());

                //gc
                addGcInfo(dashboardModel);

                //runtime
                addRuntimeInfo(dashboardModel);

                //tomcat
                try {
                    addTomcatInfo(dashboardModel);
                } catch (Throwable e) {
                    logger.error("try to read tomcat info error", e);
                }

                process.appendResult(dashboardModel);

                count.getAndIncrement();
                process.times().incrementAndGet();
            } catch (Throwable e) {
                String msg = "process dashboard failed: " + e.getMessage();
                logger.error(msg, e);
                process.end(-1, msg);
            }
        }
    }

}

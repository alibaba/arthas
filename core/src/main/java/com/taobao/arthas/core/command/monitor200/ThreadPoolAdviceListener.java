package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.command.model.ThreadPoolModel;
import com.taobao.arthas.core.command.model.ThreadPoolVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArthasCheckUtils;
import com.taobao.arthas.core.util.LogUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author HJ
 * @date 2021-07-08
 **/
public class ThreadPoolAdviceListener extends AdviceListenerAdapter {

    private static final String STEP_FIRST_CHAR = "`-";
    private static final String STEP_EMPTY_BOARD = "    ";


    // 输出定时任务
    private Timer timer;

    private CommandProcess process;

    private ConcurrentHashMap<ThreadPoolExecutor, ThreadPoolVO> threadPoolDataMap = new ConcurrentHashMap<ThreadPoolExecutor, ThreadPoolVO>();

    private ThreadPoolCommand threadPoolCommand;

    ThreadPoolAdviceListener(ThreadPoolCommand threadPoolCommand, CommandProcess process) {
        this.process = process;
        this.threadPoolCommand = threadPoolCommand;
    }

    @Override
    public synchronized void create() {

        if (timer == null) {
            timer = new Timer("Timer-for-arthas-threadpool-" + process.session().getSessionId(), true);
            timer.schedule(new ThreadPoolTimer(), 0);
        }
        // 由于是jvm自带的类，所以开启该标识
        GlobalOptions.isUnsafe = true;
    }

    @Override
    public synchronized void destroy() {
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
        // 结束时，将标识改为false
        GlobalOptions.isUnsafe = false;
    }


    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) {
        if (target instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tp = (ThreadPoolExecutor) target;
            if (threadPoolDataMap.get(tp) == null) {
                // 获取栈信息，拼接栈记录
                StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
                StringBuilder stackSb = new StringBuilder();
                int stackTraceDepth = threadPoolCommand.getStackTraceDepth();
                // i从1开始是为了跳过getStackTrace的调用栈信息
                StringBuilder prefix = new StringBuilder(STEP_FIRST_CHAR);
                for (int i = 1; i < stacks.length; i++) {
                    StackTraceElement ste = stacks[i];
                    // 过滤arthas增强类的调用栈
                    if (shouldSkip(ste)) {
                        continue;
                    }
                    stackSb.append(prefix)
                            .append(ste.getClassName())
                            .append(".")
                            .append(ste.getMethodName())
                            .append("(")
                            .append(ste.getFileName())
                            .append(":")
                            .append(ste.getLineNumber())
                            .append(")");
                    if (--stackTraceDepth == 0) {
                        break;
                    }
                    prefix.insert(0, STEP_EMPTY_BOARD);
                    stackSb.append('\n');
                }
                ThreadPoolVO vo = new ThreadPoolVO(stackSb.toString());
                // ConcurrentHashMap的get方法没有做同步，所以put前再check一次，如果已经存在，则不用调用带同步锁机制的put方法
                if (threadPoolDataMap.get(tp) == null) {
                    // 存到map中，其他信息由ThreadPoolTimer去采集
                    threadPoolDataMap.put(tp, vo);
                }
            }
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) {
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) {
    }


    private class ThreadPoolTimer extends TimerTask {

        @Override
        public void run() {
            try {
                // 记录采样数据
                Map<ThreadPoolExecutor, SampleVO> sampleMap = new HashMap<ThreadPoolExecutor, SampleVO>();
                // 命令执行总时间
                int maxDurationMillis = threadPoolCommand.getDuration();
                // 采集频率，不能大于命令总时长
                int sampleInterval = Math.min(threadPoolCommand.getSampleInterval(), maxDurationMillis);
                // 兜底，最多采集1024次，最少采集1次
                int maxSampleTimes = Math.min(1024, Math.max(maxDurationMillis / sampleInterval, 1));
                // 最终的输出list
                List<ThreadPoolVO> threadPools = new ArrayList<ThreadPoolVO>(threadPoolDataMap.size());
                // 按照固定的采集频率获取已经捕获的线程池的数据
                while (maxSampleTimes > 0) {
                    // 此处先sleep，因为timer.schedule(new ThreadPoolTimer(), 0);延迟是0，所以可能此时threadPoolDataMap还没有采集到数据
                    Thread.sleep(sampleInterval);
                    for (Map.Entry<ThreadPoolExecutor, ThreadPoolVO> entry : threadPoolDataMap.entrySet()) {
                        ThreadPoolExecutor tpe = entry.getKey();
                        // 获取线程池信息
                        SampleVO sampleVO = sampleMap.get(tpe);
                        if (sampleVO == null) {
                            sampleVO = new SampleVO(tpe.getActiveCount(), tpe.getQueue().size(), 1);
                            sampleMap.put(tpe, sampleVO);
                        } else {
                            sampleVO.sample(tpe.getActiveCount(), tpe.getQueue().size());
                        }
                        // 最后一次采样，计算平均数据
                        if (maxSampleTimes == 1) {
                            ThreadPoolVO vo = entry.getValue();
                            // 核心线程数
                            vo.setCorePoolSize(tpe.getCorePoolSize());
                            // 最大线程数
                            vo.setMaximumPoolSize(tpe.getMaximumPoolSize());
                            // 繁忙线程数平均值
                            vo.setActiveThreadCount(sampleVO.activeThreadCountSampleAverage());
                            // 队列堆积平均值
                            vo.setCurrentSizeOfWorkQueue(sampleVO.currentSizeOfWorkQueueSampleAverage());
                            threadPools.add(vo);
                        }
                    }
                    // 采集次数-1
                    maxSampleTimes--;
                }
                // 按繁忙线程数从多到少排序
                Collections.sort(threadPools);
                // 如果指定了topN，则做截取
                if (threadPoolCommand.getTopNActiveThreadCount() > 0) {
                    threadPools = threadPools.subList(0, Math.min(threadPoolCommand.getTopNActiveThreadCount(), threadPools.size()));
                }
                // 输出结果
                ThreadPoolModel threadPoolModel = new ThreadPoolModel();
                threadPoolModel.setThreadPools(threadPools);
                process.appendResult(threadPoolModel);
                process.end();
            } catch (Throwable e) {
                process.end(1, e.getMessage() + ", visit " + LogUtil.loggingFile() + " for more detail");
            }
        }

    }

    // 采样结果
    private static class SampleVO {

        // 繁忙线程数采样总和
        int activeThreadCountSampleSum;
        // 队列堆积数采样总和
        int currentSizeOfWorkQueueSampleSum;
        // 采样次数
        int sampleTimes;

        SampleVO(int activeThreadCountSampleSum, int currentSizeOfWorkQueueSampleSum, int sampleTimes) {
            this.activeThreadCountSampleSum = activeThreadCountSampleSum;
            this.currentSizeOfWorkQueueSampleSum = currentSizeOfWorkQueueSampleSum;
            this.sampleTimes = sampleTimes;
        }

        int activeThreadCountSampleAverage() {
            return activeThreadCountSampleSum / sampleTimes;
        }

        int currentSizeOfWorkQueueSampleAverage() {
            return currentSizeOfWorkQueueSampleSum / sampleTimes;
        }

        void sample(int activeThreadCountSample, int currentSizeOfWorkQueueSample) {
            activeThreadCountSampleSum += activeThreadCountSample;
            currentSizeOfWorkQueueSampleSum += currentSizeOfWorkQueueSample;
            sampleTimes++;
        }
    }

    private boolean shouldSkip(StackTraceElement ste) {
        String className = ste.getClassName();
        try {
            // 跳过arthas自己的增强类
            if ("java.arthas.SpyAPI".equals(className)) {
                return true;
            }
            Class clazz = Class.forName(className);
            if (null != clazz && ArthasCheckUtils.isEquals(clazz.getClassLoader(), Enhancer.class.getClassLoader())) {
                return true;
            }
            // 跳过被增强的类和方法本身
            if (threadPoolCommand.getClassNameMatcher().matching(ste.getClassName()) && threadPoolCommand.getMethodNameMatcher().matching(ste.getMethodName())) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
}

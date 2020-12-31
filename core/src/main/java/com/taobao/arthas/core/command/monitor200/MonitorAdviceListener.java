package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.model.MonitorModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;

/**
 * 输出的内容格式为:<br/>
 * <style type="text/css">
 * table, th, td {
 * borders:1px solid #cccccc;
 * borders-collapse:collapse;
 * }
 * </style>
 * <table>
 * <tr>
 * <th>时间戳</th>
 * <th>统计周期(s)</th>
 * <th>类全路径</th>
 * <th>方法名</th>
 * <th>调用总次数</th>
 * <th>成功次数</th>
 * <th>失败次数</th>
 * <th>平均耗时(ms)</th>
 * <th>失败率</th>
 * </tr>
 * <tr>
 * <td>2012-11-07 05:00:01</td>
 * <td>120</td>
 * <td>com.taobao.item.ItemQueryServiceImpl</td>
 * <td>queryItemForDetail</td>
 * <td>1500</td>
 * <td>1000</td>
 * <td>500</td>
 * <td>15</td>
 * <td>30%</td>
 * </tr>
 * <tr>
 * <td>2012-11-07 05:00:01</td>
 * <td>120</td>
 * <td>com.taobao.item.ItemQueryServiceImpl</td>
 * <td>queryItemById</td>
 * <td>900</td>
 * <td>900</td>
 * <td>0</td>
 * <td>7</td>
 * <td>0%</td>
 * </tr>
 * </table>
 *
 * @author beiwei30 on 28/11/2016.
 */
class MonitorAdviceListener extends AdviceListenerAdapter {
    // 输出定时任务
    private Timer timer;
    private static final Logger logger = LoggerFactory.getLogger(MonitorAdviceListener.class);
    // 监控数据
    private ConcurrentHashMap<Key, AtomicReference<MonitorData>> monitorData = new ConcurrentHashMap<Key, AtomicReference<MonitorData>>();
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private final ThreadLocal<Boolean> conditionResult = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };
    private MonitorCommand command;
    private CommandProcess process;

    MonitorAdviceListener(MonitorCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public synchronized void create() {
        if (timer == null) {
            timer = new Timer("Timer-for-arthas-monitor-" + process.session().getSessionId(), true);
            timer.scheduleAtFixedRate(new MonitorTimer(monitorData, process, command.getNumberOfLimit()),
                    0, command.getCycle() * 1000);
        }
    }

    @Override
    public synchronized void destroy() {
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        threadLocalWatch.start();
        if (!StringUtils.isEmpty(this.command.getConditionExpress()) && command.isBefore()) {
            Advice advice = Advice.newForBefore(loader, clazz, method, target, args);
            long cost = threadLocalWatch.cost();
            this.conditionResult.set(isConditionMet(this.command.getConditionExpress(), advice, cost));
            //重新计算执行方法的耗时(排除执行condition-express耗时)
            threadLocalWatch.start();
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
                               Object[] args, Object returnObject) throws Throwable {
        finishing(clazz, method, false, Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject));
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
                              Object[] args, Throwable throwable) {
        finishing(clazz, method, true, Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    private void finishing(Class<?> clazz, ArthasMethod method, boolean isThrowing, Advice advice) {
        double cost = threadLocalWatch.costInMillis();

        if (command.isBefore()) {
            if (!this.conditionResult.get()) {
                return;
            }
        } else {
            try {
                //不满足condition-express的不纳入统计
                if (!isConditionMet(this.command.getConditionExpress(), advice, cost)) {
                    return;
                }
            } catch (ExpressException e) {
                //condition-express执行错误的不纳入统计
                logger.warn("monitor execute condition-express failed.", e);
                return;
            }
        }

        final Key key = new Key(clazz.getName(), method.getName());

        while (true) {
            AtomicReference<MonitorData> value = monitorData.get(key);
            if (null == value) {
                monitorData.putIfAbsent(key, new AtomicReference<MonitorData>(new MonitorData()));
                continue;
            }

            while (true) {
                MonitorData oData = value.get();
                MonitorData nData = new MonitorData();
                nData.setCost(oData.getCost() + cost);
                if (isThrowing) {
                    nData.setFailed(oData.getFailed() + 1);
                    nData.setSuccess(oData.getSuccess());
                } else {
                    nData.setFailed(oData.getFailed());
                    nData.setSuccess(oData.getSuccess() + 1);
                }
                nData.setTotal(oData.getTotal() + 1);
                if (value.compareAndSet(oData, nData)) {
                    break;
                }
            }
            break;
        }
    }

    private class MonitorTimer extends TimerTask {
        private Map<Key, AtomicReference<MonitorData>> monitorData;
        private CommandProcess process;
        private int limit;

        MonitorTimer(Map<Key, AtomicReference<MonitorData>> monitorData, CommandProcess process, int limit) {
            this.monitorData = monitorData;
            this.process = process;
            this.limit = limit;
        }

        @Override
        public void run() {
            if (monitorData.isEmpty()) {
                return;
            }
            // 超过次数上限，则不再输出，命令终止
            if (process.times().getAndIncrement() >= limit) {
                this.cancel();
                abortProcess(process, limit);
                return;
            }

            List<MonitorData> monitorDataList = new ArrayList<MonitorData>(monitorData.size());
            for (Map.Entry<Key, AtomicReference<MonitorData>> entry : monitorData.entrySet()) {
                final AtomicReference<MonitorData> value = entry.getValue();

                MonitorData data;
                while (true) {
                    data = value.get();
                    //swap monitor data to new instance
                    if (value.compareAndSet(data, new MonitorData())) {
                        break;
                    }
                }

                if (null != data) {
                    data.setClassName(entry.getKey().getClassName());
                    data.setMethodName(entry.getKey().getMethodName());
                    monitorDataList.add(data);
                }
            }
            process.appendResult(new MonitorModel(monitorDataList));
        }

    }

    /**
     * 数据监控用的Key
     *
     * @author vlinux
     */
    private static class Key {
        private final String className;
        private final String methodName;

        Key(String className, String behaviorName) {
            this.className = className;
            this.methodName = behaviorName;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public int hashCode() {
            return className.hashCode() + methodName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (null == obj
                    || !(obj instanceof Key)) {
                return false;
            }
            Key okey = (Key) obj;
            return isEquals(okey.className, className) && isEquals(okey.methodName, methodName);
        }

    }

}

package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.ReflectAdviceListenerAdapter;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;
import static com.taobao.text.ui.Element.label;

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
class MonitorAdviceListener extends ReflectAdviceListenerAdapter {
    // 输出定时任务
    private Timer timer;
    // 监控数据
    private ConcurrentHashMap<Key, AtomicReference<Data>> monitorData = new ConcurrentHashMap<Key, AtomicReference<Data>>();
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private MonitorCommand command;
    private CommandProcess process;

    MonitorAdviceListener(MonitorCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
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
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
                               Object[] args, Object returnObject) throws Throwable {
        finishing(clazz, method, false);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
                              Object[] args, Throwable throwable) {
        finishing(clazz, method, true);
    }

    private void finishing(Class<?> clazz, ArthasMethod method, boolean isThrowing) {
        double cost = threadLocalWatch.costInMillis();
        final Key key = new Key(clazz.getName(), method.getName());

        while (true) {
            AtomicReference<Data> value = monitorData.get(key);
            if (null == value) {
                monitorData.putIfAbsent(key, new AtomicReference<Data>(new Data()));
                continue;
            }

            while (true) {
                Data oData = value.get();
                Data nData = new Data();
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
        private Map<Key, AtomicReference<Data>> monitorData;
        private CommandProcess process;
        private int limit;

        MonitorTimer(Map<Key, AtomicReference<Data>> monitorData, CommandProcess process, int limit) {
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

            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            table.row(true, label("timestamp").style(Decoration.bold.bold()),
                    label("class").style(Decoration.bold.bold()),
                    label("method").style(Decoration.bold.bold()),
                    label("total").style(Decoration.bold.bold()),
                    label("success").style(Decoration.bold.bold()),
                    label("fail").style(Decoration.bold.bold()),
                    label("avg-rt(ms)").style(Decoration.bold.bold()),
                    label("fail-rate").style(Decoration.bold.bold()));

            for (Map.Entry<Key, AtomicReference<Data>> entry : monitorData.entrySet()) {
                final AtomicReference<Data> value = entry.getValue();

                Data data;
                while (true) {
                    data = value.get();
                    if (value.compareAndSet(data, new Data())) {
                        break;
                    }
                }

                if (null != data) {

                    final DecimalFormat df = new DecimalFormat("0.00");

                    table.row(
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                            entry.getKey().getClassName(),
                            entry.getKey().getMethodName(),
                            "" + data.getTotal(),
                            "" + data.getSuccess(),
                            "" + data.getFailed(),
                            df.format(div(data.getCost(), data.getTotal())),
                            df.format(100.0d * div(data.getFailed(), data.getTotal())) + "%"
                    );

                }
            }

            process.write(RenderUtil.render(table, process.width()) + "\n");
        }

        private double div(double a, double b) {
            if (b == 0) {
                return 0;
            }
            return a / b;
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

    /**
     * 数据监控用的value
     *
     * @author vlinux
     */
    private static class Data {
        private int total;
        private int success;
        private int failed;
        private double cost;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }
    }
}

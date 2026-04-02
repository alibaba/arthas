package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.model.MonitorModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;

/**
 * 监控建议监听器
 * 用于统计方法调用的次数、成功/失败次数、平均耗时等信息
 *
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
    /** 定时器，用于周期性输出监控数据 */
    private Timer timer;
    private static final Logger logger = LoggerFactory.getLogger(MonitorAdviceListener.class);
    /** 监控数据存储，key为类名+方法名，value为监控数据的原子引用 */
    private ConcurrentHashMap<Key, AtomicReference<MonitorData>> monitorData = new ConcurrentHashMap<Key, AtomicReference<MonitorData>>();
    /** 线程本地计时器，用于记录方法执行时间 */
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    /** 线程本地变量，存储条件表达式的执行结果（用于before条件判断） */
    private final ThreadLocal<Boolean> conditionResult = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };
    /** 关联的监控命令 */
    private MonitorCommand command;
    /** 命令处理进程 */
    private CommandProcess process;

    /**
     * 构造函数
     * @param command 监控命令
     * @param process 命令处理进程
     * @param verbose 是否输出详细信息
     */
    MonitorAdviceListener(MonitorCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    /**
     * 创建监听器
     * 初始化定时器，周期性地输出监控统计数据
     */
    @Override
    public synchronized void create() {
        if (timer == null) {
            // 创建守护线程定时器
            timer = new Timer("Timer-for-arthas-monitor-" + process.session().getSessionId(), true);
            // 按照指定的周期调度定时任务
            timer.scheduleAtFixedRate(new MonitorTimer(monitorData, process, command.getNumberOfLimit()),
                    0, command.getCycle() * 1000L);
        }
    }

    /**
     * 销毁监听器
     * 取消定时器并清理资源
     */
    @Override
    public synchronized void destroy() {
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 方法调用前的处理
     * 开始计时，如果配置了before条件表达式，则执行条件判断
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象
     * @param args 方法参数
     */
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计时
        threadLocalWatch.start();
        // 如果配置了条件表达式且需要在方法执行前判断
        if (!StringUtils.isEmpty(this.command.getConditionExpress()) && command.isBefore()) {
            Advice advice = Advice.newForBefore(loader, clazz, method, target, args);
            long cost = threadLocalWatch.cost();
            // 执行条件表达式并保存结果
            this.conditionResult.set(isConditionMet(this.command.getConditionExpress(), advice, cost));
            // 重新开始计时（排除执行条件表达式的耗时）
            threadLocalWatch.start();
        }
    }

    /**
     * 方法正常返回后的处理
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象
     * @param args 方法参数
     * @param returnObject 返回值
     */
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
                               Object[] args, Object returnObject) throws Throwable {
        finishing(clazz, method, false, Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject));
    }

    /**
     * 方法抛出异常后的处理
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象
     * @param args 方法参数
     * @param throwable 抛出的异常
     */
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
                              Object[] args, Throwable throwable) {
        finishing(clazz, method, true, Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    /**
     * 方法执行完成后的统计处理
     * 累计方法的调用次数、成功/失败次数和总耗时
     * @param clazz 目标类
     * @param method 目标方法
     * @param isThrowing 是否抛出异常
     * @param advice 通知对象
     */
    private void finishing(Class<?> clazz, ArthasMethod method, boolean isThrowing, Advice advice) {
        // 获取方法执行耗时（毫秒）
        double cost = threadLocalWatch.costInMillis();

        // 如果配置了before条件，使用之前保存的判断结果
        if (command.isBefore()) {
            if (!this.conditionResult.get()) {
                // 条件不满足，不纳入统计
                return;
            }
        } else {
            // 否则，在方法执行后判断条件
            try {
                // 不满足条件表达式的不纳入统计
                if (!isConditionMet(this.command.getConditionExpress(), advice, cost)) {
                    return;
                }
            } catch (ExpressException e) {
                // 条件表达式执行错误的不纳入统计
                logger.warn("monitor execute condition-express failed.", e);
                return;
            }
        }

        // 创建监控数据的key（类名+方法名）
        final Key key = new Key(clazz.getName(), method.getName());

        // 使用CAS（Compare And Swap）方式更新监控数据
        while (true) {
            AtomicReference<MonitorData> value = monitorData.get(key);
            if (null == value) {
                // 如果key不存在，先创建初始值
                monitorData.putIfAbsent(key, new AtomicReference<MonitorData>(new MonitorData()));
                continue;
            }

            // 使用无锁方式更新监控数据
            while (true) {
                MonitorData oData = value.get();
                MonitorData nData = new MonitorData();
                // 累加耗时
                nData.setCost(oData.getCost() + cost);
                // 更新时间戳
                nData.setTimestamp(LocalDateTime.now());
                if (isThrowing) {
                    // 方法抛出异常，失败次数加1
                    nData.setFailed(oData.getFailed() + 1);
                    nData.setSuccess(oData.getSuccess());
                } else {
                    // 方法正常返回，成功次数加1
                    nData.setFailed(oData.getFailed());
                    nData.setSuccess(oData.getSuccess() + 1);
                }
                // 调用总次数加1
                nData.setTotal(oData.getTotal() + 1);
                // CAS操作，如果成功则跳出循环
                if (value.compareAndSet(oData, nData)) {
                    break;
                }
            }
            break;
        }
    }

    /**
     * 监控定时任务
     * 周期性地收集并输出监控统计数据
     */
    private class MonitorTimer extends TimerTask {
        /** 监控数据映射 */
        private Map<Key, AtomicReference<MonitorData>> monitorData;
        /** 命令处理进程 */
        private CommandProcess process;
        /** 执行次数上限 */
        private int limit;

        /**
         * 构造函数
         * @param monitorData 监控数据映射
         * @param process 命令处理进程
         * @param limit 执行次数上限
         */
        MonitorTimer(Map<Key, AtomicReference<MonitorData>> monitorData, CommandProcess process, int limit) {
            this.monitorData = monitorData;
            this.process = process;
            this.limit = limit;
        }

        /**
         * 定时任务执行方法
         * 收集监控数据并输出
         */
        @Override
        public void run() {
            // 如果没有监控数据，直接返回
            if (monitorData.isEmpty()) {
                return;
            }
            // 超过次数上限，则不再输出，命令终止
            if (process.times().getAndIncrement() >= limit) {
                this.cancel();
                abortProcess(process, limit);
                return;
            }

            // 创建监控数据列表
            List<MonitorData> monitorDataList = new ArrayList<MonitorData>(monitorData.size());
            // 遍历所有监控数据
            for (Map.Entry<Key, AtomicReference<MonitorData>> entry : monitorData.entrySet()) {
                final AtomicReference<MonitorData> value = entry.getValue();

                MonitorData data;
                // 使用CAS方式交换监控数据
                while (true) {
                    data = value.get();
                    // 将监控数据交换为新实例（原子操作）
                    if (value.compareAndSet(data, new MonitorData())) {
                        break;
                    }
                }

                if (null != data) {
                    // 设置类名和方法名
                    data.setClassName(entry.getKey().getClassName());
                    data.setMethodName(entry.getKey().getMethodName());
                    monitorDataList.add(data);
                }
            }
            // 输出监控结果
            process.appendResult(new MonitorModel(monitorDataList));
        }

    }

    /**
     * 监控数据的键
     * 由类名和方法名组成，用于唯一标识一个监控目标
     *
     * @author vlinux
     */
    private static class Key {
        /** 类名 */
        private final String className;
        /** 方法名 */
        private final String methodName;

        /**
         * 构造函数
         * @param className 类名
         * @param behaviorName 方法名
         */
        Key(String className, String behaviorName) {
            this.className = className;
            this.methodName = behaviorName;
        }

        /**
         * 获取类名
         * @return 类名
         */
        public String getClassName() {
            return className;
        }

        /**
         * 获取方法名
         * @return 方法名
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * 计算哈希值
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return className.hashCode() + methodName.hashCode();
        }

        /**
         * 判断两个Key是否相等
         * @param obj 比较对象
         * @return true表示相等
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key okey = (Key) obj;
            return isEquals(okey.className, className) && isEquals(okey.methodName, methodName);
        }

    }

}

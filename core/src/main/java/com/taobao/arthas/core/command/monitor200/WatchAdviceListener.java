package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AccessPoint;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.time.LocalDateTime;

/**
 * Watch命令的监听器
 * 负责监听方法调用并在满足条件时输出观察结果
 *
 * @author beiwei30 on 29/11/2016.
 */
class WatchAdviceListener extends AdviceListenerAdapter {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(WatchAdviceListener.class);
    // 线程本地计时器，用于计算方法执行耗时
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    // Watch命令对象，包含命令配置信息
    private WatchCommand command;
    // 命令进程对象，用于输出结果
    private CommandProcess process;

    /**
     * 构造函数
     *
     * @param command Watch命令对象，包含命令配置
     * @param process 命令进程对象，用于输出结果
     * @param verbose 是否输出详细调试信息
     */
    public WatchAdviceListener(WatchCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    /**
     * 判断是否需要在方法结束时进行观察
     * 当命令配置为finish，或者没有配置任何观察点（before、exception、success）时，
     * 都会在方法结束时进行观察
     *
     * @return 如果需要在结束时观察返回true，否则返回false
     */
    private boolean isFinish() {
        return command.isFinish() || !command.isBefore() && !command.isException() && !command.isSuccess();
    }

    /**
     * 方法调用前的回调
     * 在目标方法执行前被调用
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象（静态方法为null）
     * @param args 方法参数
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        // 如果命令配置了观察方法调用前，则执行观察逻辑
        if (command.isBefore()) {
            watching(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    /**
     * 方法正常返回后的回调
     * 在目标方法成功执行并返回后被调用
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象（静态方法为null）
     * @param args 方法参数
     * @param returnObject 方法返回值
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        // 创建方法正常返回的通知对象
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        // 如果命令配置了观察方法成功返回，则执行观察逻辑
        if (command.isSuccess()) {
            watching(advice);
        }

        // 检查是否需要在方法结束时进行观察
        finishing(advice);
    }

    /**
     * 方法抛出异常后的回调
     * 在目标方法执行过程中抛出异常时被调用
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象（静态方法为null）
     * @param args 方法参数
     * @param throwable 抛出的异常
     */
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        // 创建方法抛出异常的通知对象
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        // 如果命令配置了观察方法异常，则执行观察逻辑
        if (command.isException()) {
            watching(advice);
        }

        // 检查是否需要在方法结束时进行观察
        finishing(advice);
    }

    /**
     * 处理方法结束时的观察逻辑
     * 当命令配置要求在方法结束时进行观察时调用
     *
     * @param advice 方法通知对象，包含方法的完整上下文信息
     */
    private void finishing(Advice advice) {
        // 判断是否需要在结束时观察
        if (isFinish()) {
            watching(advice);
        }
    }


    /**
     * 核心观察方法
     * 执行条件判断和表达式求值，输出观察结果
     *
     * @param advice 方法通知对象，包含方法的完整上下文信息
     */
    private void watching(Advice advice) {
        try {
            // 获取本次方法调用的耗时（毫秒）
            double cost = threadLocalWatch.costInMillis();

            // 判断条件表达式是否满足
            boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);

            // 如果启用了详细输出，打印条件表达式和判断结果
            if (this.isVerbose()) {
                process.write("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
            }

            // 只有当条件满足时才进行观察和输出
            if (conditionResult) {
                // TODO: concurrency issues for process.write
                // 注意：process.write存在并发问题，但暂未处理

                // 执行用户指定的表达式，获取观察值
                Object value = getExpressionResult(command.getExpress(), advice, cost);

                // 构建观察结果模型
                WatchModel model = new WatchModel();
                // 设置时间戳为当前时间
                model.setTs(LocalDateTime.now());
                // 设置方法执行耗时
                model.setCost(cost);
                // 设置观察值，并指定展开层级
                model.setValue(new ObjectVO(value, command.getExpand()));
                // 设置结果大小限制
                model.setSizeLimit(command.getSizeLimit());
                // 设置类名
                model.setClassName(advice.getClazz().getName());
                // 设置方法名
                model.setMethodName(advice.getMethod().getName());

                // 设置访问点（方法调用的阶段）
                if (advice.isBefore()) {
                    // 方法调用前
                    model.setAccessPoint(AccessPoint.ACCESS_BEFORE.getKey());
                } else if (advice.isAfterReturning()) {
                    // 方法正常返回后
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_RETUNING.getKey());
                } else if (advice.isAfterThrowing()) {
                    // 方法抛出异常后
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_THROWING.getKey());
                }

                // 将结果添加到输出中
                process.appendResult(model);

                // 增加执行次数计数
                process.times().incrementAndGet();

                // 检查是否超过了执行次数限制
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    // 如果超过限制，终止命令执行
                    abortProcess(process, command.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            // 捕获所有异常，记录警告日志
            logger.warn("watch failed.", e);
            // 向用户报告错误，并提供日志文件位置以获取更多详细信息
            process.end(-1, "watch failed, condition is: " + command.getConditionExpress() + ", express is: "
                    + command.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }
}

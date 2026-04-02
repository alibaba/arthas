package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.StackModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.arthas.core.util.ThreadUtil;

import java.time.LocalDateTime;

/**
 * 栈追踪命令监听器
 * <p>
 * 用于监听目标方法的调用，在方法执行完成后输出当前线程的调用栈信息。
 * 可以配置条件表达式，只有满足条件时才输出调用栈。
 * 支持限制输出次数，避免产生过多数据。
 *
 * @author beiwei30 on 29/11/2016.
 */
public class StackAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(StackAdviceListener.class);

    /** 线程本地计时器，用于记录方法调用的耗时 */
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    /** 栈命令对象，包含命令配置信息 */
    private StackCommand command;
    /** 命令处理进程，用于输出结果 */
    private CommandProcess process;

    /**
     * 构造函数
     *
     * @param command 栈命令对象
     * @param process 命令处理进程
     * @param verbose 是否输出详细日志
     */
    public StackAdviceListener(StackCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    /**
     * 方法调用前的回调
     * 在目标方法执行前调用，启动计时器记录方法调用开始时间
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象
     * @param args 方法参数
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    /**
     * 方法抛出异常后的回调
     * 在目标方法抛出异常时调用，输出当前线程的调用栈信息
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象
     * @param args 方法参数
     * @param throwable 抛出的异常
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        // 创建包含异常信息的Advice对象
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(advice);
    }

    /**
     * 方法正常返回后的回调
     * 在目标方法正常返回时调用，输出当前线程的调用栈信息
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象
     * @param args 方法参数
     * @param returnObject 返回值
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        // 创建包含返回值的Advice对象
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        finishing(advice);
    }

    /**
     * 方法执行完成后的处理逻辑
     * 计算方法调用耗时，检查条件表达式，如果满足条件则输出调用栈信息
     *
     * @param advice 包含方法调用上下文信息的Advice对象
     */
    private void finishing(Advice advice) {
        // 本次调用的耗时
        try {
            // 获取方法调用的耗时（毫秒）
            double cost = threadLocalWatch.costInMillis();
            // 检查条件表达式是否满足
            boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);
            // 如果是详细模式，输出条件表达式和结果
            if (this.isVerbose()) {
                process.write("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
            }
            // 如果条件满足，输出调用栈信息
            if (conditionResult) {
                // TODO: concurrency issues for process.write
                // 获取当前线程的调用栈模型
                StackModel stackModel = ThreadUtil.getThreadStackModel(advice.getLoader(), Thread.currentThread());
                // 设置时间戳
                stackModel.setTs(LocalDateTime.now());
                // 将结果添加到输出
                process.appendResult(stackModel);
                // 增加执行次数计数
                process.times().incrementAndGet();
                // 检查是否超过限制次数
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    // 超过限制，终止进程
                    abortProcess(process, command.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            // 发生异常，记录日志并终止进程
            logger.warn("stack failed.", e);
            process.end(-1, "stack failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                          + ", visit " + LogUtil.loggingFile() + " for more details.");
        }
    }
}

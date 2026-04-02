package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抽象的跟踪通知监听器
 * 用于跟踪方法调用的监听器基类，提供了方法调用跟踪的通用功能
 *
 * @author ralf0131 2017-01-06 16:02.
 */
public class AbstractTraceAdviceListener extends AdviceListenerAdapter {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(AbstractTraceAdviceListener.class);

    // 线程本地计时器，用于记录方法调用耗时
    protected final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    // 关联的跟踪命令
    protected TraceCommand command;

    // 命令进程，用于输出结果和控制流程
    protected CommandProcess process;

    // 标记进程是否已中止，使用原子布尔保证线程安全
    private final AtomicBoolean processAborted = new AtomicBoolean(false);

    // 线程本地的跟踪实体，用于存储每个线程的调用栈信息
    protected final ThreadLocal<TraceEntity> threadBoundEntity = new ThreadLocal<TraceEntity>();

    /**
     * 构造函数
     *
     * @param command 跟踪命令
     * @param process 命令进程
     */
    public AbstractTraceAdviceListener(TraceCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }

    /**
     * 获取或创建线程本地的跟踪实体
     * 每个线程都有自己独立的跟踪实体，用于记录该线程的方法调用栈
     *
     * @param loader 类加载器
     * @return 线程本地的跟踪实体
     */
    protected TraceEntity threadLocalTraceEntity(ClassLoader loader) {
        // 从ThreadLocal中获取当前线程的跟踪实体
        TraceEntity traceEntity = threadBoundEntity.get();
        // 如果当前线程还没有跟踪实体，则创建一个新的
        if (traceEntity == null) {
            traceEntity = new TraceEntity(loader);
            threadBoundEntity.set(traceEntity);
        }
        return traceEntity;
    }

    /**
     * 销毁监听器
     * 清理线程本地的跟踪实体，防止内存泄漏
     */
    @Override
    public void destroy() {
        threadBoundEntity.remove();
    }

    /**
     * 方法调用前的处理
     * 记录方法调用的开始信息，包括类名、方法名，并开始计时
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
        // 获取当前线程的跟踪实体
        TraceEntity traceEntity = threadLocalTraceEntity(loader);
        // 在调用树中记录方法调用的开始
        traceEntity.tree.begin(clazz.getName(), method.getName(), -1, false);
        // 增加调用深度
        traceEntity.deep++;
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    /**
     * 方法正常返回后的处理
     * 结束方法调用记录，并检查是否满足输出条件
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
        // 结束当前方法在调用树中的记录
        threadLocalTraceEntity(loader).tree.end();
        // 创建方法正常返回的通知对象
        final Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        // 执行完成处理逻辑
        finishing(loader, advice);
    }

    /**
     * 方法抛出异常后的处理
     * 结束方法调用记录，记录异常信息，并检查是否满足输出条件
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
        // 获取异常发生的行号，默认为-1
        int lineNumber = -1;
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length != 0) {
            // 获取异常栈顶元素的行号（即异常抛出的位置）
            lineNumber = stackTrace[0].getLineNumber();
        }

        // 结束当前方法在调用树中的记录，并记录异常信息
        threadLocalTraceEntity(loader).tree.end(throwable, lineNumber);
        // 创建方法抛出异常的通知对象
        final Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        // 执行完成处理逻辑
        finishing(loader, advice);
    }

    /**
     * 获取关联的跟踪命令
     *
     * @return 跟踪命令
     */
    public TraceCommand getCommand() {
        return command;
    }

    /**
     * 完成方法调用的处理
     * 检查调用深度，当返回到最外层调用时，判断是否满足输出条件并输出结果
     *
     * @param loader 类加载器
     * @param advice 通知对象，包含方法调用的上下文信息
     */
    private void finishing(ClassLoader loader, Advice advice) {
        // 获取当前线程的跟踪实体
        TraceEntity traceEntity = threadLocalTraceEntity(loader);

        // 减少调用深度，#1817 防止deep为负数
        if (traceEntity.deep >= 1) {
            traceEntity.deep--;
        }

        // 只有当调用深度为0时（即返回到最外层调用），才进行输出处理
        if (traceEntity.deep == 0) {
            // 获取本次完整调用的总耗时（毫秒）
            double cost = threadLocalWatch.costInMillis();
            try {
                // 判断是否满足条件表达式的输出条件
                boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);

                // 如果是详细模式，输出条件表达式的判断结果
                if (this.isVerbose()) {
                    process.write("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
                }

                // 如果满足输出条件
                if (conditionResult) {
                    // 增加输出次数计数
                    process.times().incrementAndGet();
                    // 将跟踪结果模型添加到进程输出中
                    // TODO: concurrency issues for process.write
                    process.appendResult(traceEntity.getModel());

                    // 检查是否达到数量限制
                    if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                        // 如果达到限制，中止进程
                        abortProcess(process, command.getNumberOfLimit());
                    }
                }
            } catch (Throwable e) {
                // 处理过程中发生异常，记录日志并结束进程
                logger.warn("trace failed.", e);
                process.end(1, "trace failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                              + ", visit " + LogUtil.loggingFile() + " for more details.");
            } finally {
                // 无论成功或失败，都清理线程本地的跟踪实体
                threadBoundEntity.remove();
            }
        }
    }

    /**
     * 中止进程
     * 使用原子操作确保只有一个线程能执行中止逻辑，避免重复中止
     *
     * @param process 命令进程
     * @param limit 限制数量
     */
    @Override
    protected void abortProcess(CommandProcess process, int limit) {
        // 使用CAS操作确保只有第一个线程能执行中止逻辑
        // Only proceed if this thread is the first one to set the flag to true
        if (processAborted.compareAndSet(false, true)) {
            // 调用父类的中止方法
            super.abortProcess(process, limit);
        }
    }
}

package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

/**
 * @author ralf0131 2017-01-06 16:02.
 */
public class AbstractTraceAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AbstractTraceAdviceListener.class);
    protected final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    protected TraceCommand command;
    protected CommandProcess process;

    protected final ThreadLocal<TraceEntity> threadBoundEntity = new ThreadLocal<TraceEntity>();

    /**
     * Constructor
     */
    public AbstractTraceAdviceListener(TraceCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }

    protected TraceEntity threadLocalTraceEntity(ClassLoader loader) {
        TraceEntity traceEntity = threadBoundEntity.get();
        if (traceEntity == null) {
            traceEntity = new TraceEntity(loader);
            threadBoundEntity.set(traceEntity);
        }
        return traceEntity;
    }

    @Override
    public void destroy() {
        //TODO 这里对ThreadLocal执行remove意义不大，不严谨，调用此方法的线程，跟执行before那些方法的线程可能不是一个线程，
        // 比如执行q指令、ctrl+c指令的线程，既然线程都不是之前set值的那个线程了，那么对ThreadLocal执行remove便没意义了
        threadBoundEntity.remove();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        TraceEntity traceEntity = threadLocalTraceEntity(loader);
        traceEntity.tree.begin(clazz.getName(), method.getName(), -1, false);
        traceEntity.deep++;
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        threadLocalTraceEntity(loader).tree.end();
        final Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        finishing(loader, advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        int lineNumber = -1;
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length != 0) {
            lineNumber = stackTrace[0].getLineNumber();
        }

        threadLocalTraceEntity(loader).tree.end(throwable, lineNumber);
        final Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(loader, advice);
    }

    public TraceCommand getCommand() {
        return command;
    }

    private void finishing(ClassLoader loader, Advice advice) {
        // 本次调用的耗时
        TraceEntity traceEntity = threadLocalTraceEntity(loader);
        if (traceEntity.deep >= 1) { // #1817 防止deep为负数
            traceEntity.deep--;
            //此次修复了有after没有before的问题后，deep目前不会减为负数了，
            //不过目前仍存在不配对的情况(即有before没有after，此情况目前是没问题的)，
            //TODO 建议后续重构before与after的机制，将其与条件匹配&结果输出等逻辑剥离出来，
            // 严格保证traceEntity.deep++与traceEntity.deep--等这些“进出栈”逻辑能配对执行，不管当前Process的状态如何，
            // 进行条件判断&输出结果时再根据Process的状态来决定是否跳过。
            // 可能也是因为不配对的问题，导致很多ThreadLocal不能定义为static，需要在AdviceListener销毁的时候一同废弃
        }
        if (traceEntity.deep == 0) {
            double cost = threadLocalWatch.costInMillis();
            try {
                boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);
                if (this.isVerbose()) {
                    process.write("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
                }
                if (conditionResult) {
                    // 满足输出条件
                    process.times().incrementAndGet();
                    // TODO: concurrency issues for process.write
                    process.appendResult(traceEntity.getModel());

                    // 是否到达数量限制
                    if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                        // TODO: concurrency issue to abort process
                        abortProcess(process, command.getNumberOfLimit());
                    }
                }
            } catch (Throwable e) {
                logger.warn("trace failed.", e);
                process.end(1, "trace failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                              + ", visit " + LogUtil.loggingFile() + " for more details.");
            } finally {
                threadBoundEntity.remove();
            }
        }
    }
}

package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.advisor.ReflectAdviceListenerAdapter;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.arthas.core.view.ObjectView;

/**
 * @author ralf0131 2017-01-06 16:02.
 */
public class AbstractTraceAdviceListener extends ReflectAdviceListenerAdapter {

    protected final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    protected TraceCommand command;
    protected CommandProcess process;

    protected final ThreadLocal<TraceEntity> threadBoundEntity = new ThreadLocal<TraceEntity>() {

        @Override
        protected TraceEntity initialValue() {
            return new TraceEntity();
        }
    };

    /**
     * Constructor
     */
    public AbstractTraceAdviceListener(TraceCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }

    @Override
    public void destroy() {
        threadBoundEntity.remove();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        threadBoundEntity.get().view.begin(clazz.getName() + ":" + method.getName() + "()");
        threadBoundEntity.get().deep++;
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        if (command.enableWatch() && command.isBefore()) {
            watching(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    private boolean isFinish() {
        return command.isFinish() || !command.isBefore() && !command.isException() && !command.isSuccess();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        threadBoundEntity.get().view.end();
        final Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        finishing(advice);
        if (command.enableWatch() && (command.isSuccess() || isFinish())) {
            // finishing() 中的 condition expression 存在修改 advice 状态可能性, 所以这里最好还是再 new 一个.
            watching(Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject));
        }
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        threadBoundEntity.get().view.begin("throw:" + throwable.getClass().getName() + "()").end().end();
        final Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(advice);
        if (command.enableWatch() && (command.isException() || isFinish())) {
            // finishing() 中的 condition expression 存在修改 advice 状态可能性, 所以这里最好还是再 new 一个.
            watching(Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
        }
    }

    public TraceCommand getCommand() {
        return command;
    }

    private boolean isNeedExpand() {
        Integer expand = command.getExpand();
        return null != expand && expand >= 0;
    }

    private void watching(Advice advice) {
        try {
            // 本次调用的耗时
            double cost = threadLocalWatch.costInMillis();
            if (isConditionMet(command.getConditionExpress(), advice, cost)) {
                // TODO: concurrency issues for process.write
                Object value = getExpressionResult(command.getExpress(), advice, cost);
                String result = StringUtils.objectToString(
                        isNeedExpand() ? new ObjectView(value, command.getExpand(), command.getSizeLimit()).draw() : value);
                process.write("ts=" + DateUtils.getCurrentDate() + "; [cost=" + cost + "ms] result=" + result + "\n");
            }
        } catch (Exception e) {
            LogUtil.getArthasLogger().warn("watch failed.", e);
            process.write("watch failed, condition is: " + command.getConditionExpress() + ", express is: "
                          + command.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.LOGGER_FILE
                          + " for more details.\n");
            process.end();
        }
        return;
    }

    private void finishing(Advice advice) {
        // 本次调用的耗时
        double cost = threadLocalWatch.costInMillis();
        if (--threadBoundEntity.get().deep == 0) {
            try {
                if (isConditionMet(command.getConditionExpress(), advice, cost)) {
                    // 满足输出条件
                    if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                        // TODO: concurrency issue to abort process
                        abortProcess(process, command.getNumberOfLimit());
                    } else {
                        process.times().incrementAndGet();
                        // TODO: concurrency issues for process.write
                        process.write(threadBoundEntity.get().view.draw() + "\n");
                    }
                }
            } catch (Throwable e) {
                LogUtil.getArthasLogger().warn("trace failed.", e);
                process.write("trace failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                              + ", visit " + LogUtil.LOGGER_FILE + " for more details.\n");
                process.end();
            } finally {
                threadBoundEntity.remove();
            }
        }
    }
}

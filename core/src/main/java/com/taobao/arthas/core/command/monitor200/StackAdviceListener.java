package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.ReflectAdviceListenerAdapter;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.middleware.logger.Logger;

/**
 * @author beiwei30 on 29/11/2016.
 */
public class StackAdviceListener extends ReflectAdviceListenerAdapter {
    private static final Logger logger = LogUtil.getArthasLogger();

    private final ThreadLocal<String> stackThreadLocal = new ThreadLocal<String>();
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private StackCommand command;
    private CommandProcess process;

    public StackAdviceListener(StackCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        stackThreadLocal.set(ThreadUtil.getThreadStack(Thread.currentThread()));
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(advice);
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        finishing(advice);
    }

    private void finishing(Advice advice) {
        // 本次调用的耗时
        try {
            double cost = threadLocalWatch.costInMillis();
            if (isConditionMet(command.getConditionExpress(), advice, cost)) {
                // TODO: concurrency issues for process.write
                process.write("ts=" + DateUtils.getCurrentDate() + ";" + stackThreadLocal.get() + "\n");
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    abortProcess(process, command.getNumberOfLimit());
                }
            }
        } catch (Exception e) {
            logger.warn("stack failed.", e);
            process.write("stack failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                          + ", visit " + LogUtil.LOGGER_FILE + " for more details.\n");
            process.end();
        }
    }
}

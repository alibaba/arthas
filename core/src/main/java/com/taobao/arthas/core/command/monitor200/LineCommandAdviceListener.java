package com.taobao.arthas.core.command.monitor200;

import java.time.LocalDateTime;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.LineModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.StackModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.arthas.core.util.ThreadUtil;

/**
 * line 命令的运行时监听器。
 */
public class LineCommandAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LineCommandAdviceListener.class);

    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private final LineCommand command;
    private final CommandProcess process;

    public LineCommandAdviceListener(LineCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
            Object returnObject) throws Throwable {
        threadLocalWatch.costInMillis();
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
            Throwable throwable) throws Throwable {
        threadLocalWatch.costInMillis();
    }

    @Override
    public void atLine(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
            int lineNumber, String[] argNames, Object[] localVars, String[] localVarNames) throws Throwable {
        Advice advice = Advice.newForLine(loader, clazz, method, target, args, lineNumber, argNames, localVars,
                localVarNames);
        try {
            double cost = threadLocalWatch.costInMillisWithoutPop();
            boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                process.write("Condition express: " + command.getConditionExpress() + " , result: "
                        + conditionResult + "\n");
            }
            if (conditionResult) {
                Object value = getExpressionResult(command.getExpress(), advice, cost);

                Thread currentThread = Thread.currentThread();
                LineModel model = new LineModel();
                model.setTs(LocalDateTime.now());
                model.setCost(cost);
                model.setValue(new ObjectVO(value, command.getExpand()));
                model.setSizeLimit(command.getSizeLimit());
                model.setClassName(advice.getClazz().getName());
                model.setMethodName(advice.getMethod().getName());
                model.setMethodDesc(advice.getMethod().getDescriptor());
                model.setLineNumber(lineNumber);
                model.setThreadName(currentThread.getName());
                model.setThreadId(currentThread.getId());
                if (command.isStack()) {
                    model.setStackTrace(limitedStackTrace(loader, currentThread, command.getStackDepth()));
                }

                process.appendResult(model);
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    abortProcess(process, command.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            logger.warn("line failed.", e);
            process.end(-1, "line failed, condition is: " + command.getConditionExpress() + ", express is: "
                    + command.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }

    private StackTraceElement[] limitedStackTrace(ClassLoader loader, Thread currentThread, int stackDepth) {
        StackModel stackModel = ThreadUtil.getThreadStackModel(loader, currentThread);
        StackTraceElement[] stackTrace = stackModel.getStackTrace();
        if (stackTrace == null || stackTrace.length <= stackDepth) {
            return stackTrace;
        }
        StackTraceElement[] result = new StackTraceElement[stackDepth];
        System.arraycopy(stackTrace, 0, result, 0, stackDepth);
        return result;
    }
}

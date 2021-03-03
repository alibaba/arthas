package com.taobao.arthas.core.command.monitor200.curl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

/**
 * @author zhaoyuening
 */
class GetCurlAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(GetCurlAdviceListener.class);


    private CommandProcess process;
    private GetCurlCommand command;
    // 用于统计时间消耗
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    public GetCurlAdviceListener(GetCurlCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }


    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        Advice advice = Advice.newForBefore(loader, clazz, method, target, args);
        if (command.isBefore()) {
            getCurl(clazz, advice);
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        if (command.isSuccess()) {
            getCurl(clazz, advice);
        }

        finishing(clazz, advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        if (command.isException()) {
            getCurl(clazz, advice);
        }

        finishing(clazz, advice);
    }

    private void getCurl(Class<?> clazz, Advice advice) {
        try {
            // 本次调用的耗时
            double cost = threadLocalWatch.costInMillis();

            // 检测是否符合约束
            boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);
            if (!conditionResult) {
                return;
            }

            // 捕获当前方法的 Curl
            GetCurlModel getCurlModel = new GetCurlModel(clazz, advice.getClazz().getName(), advice.getMethod().getName());
            process.appendResult(getCurlModel);

            // 监控次数达到结束
            process.times().incrementAndGet();
            if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                abortProcess(process, command.getNumberOfLimit());
            }
        } catch (Exception e) {
            logger.warn("getcurl failed.", e);
            process.end(-1, "GetCurl failed, condition is: "
                    + command.getConditionExpress() + ", " + e.getMessage()
                    + ", visit " + LogUtil.loggingFile() + " for more details.");
        }
    }

    private void finishing(Class<?> clazz, Advice advice) {
        if (command.isFinish() || !command.isBefore() && !command.isException() && !command.isSuccess()) {
            getCurl(clazz, advice);
        }
    }
}

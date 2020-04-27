package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.ReflectAdviceListenerAdapter;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.model.TimeFragmentVO;
import com.taobao.arthas.core.command.model.TimeTunnelModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.util.Arrays;
import java.util.Date;

/**
 * @author beiwei30 on 30/11/2016.
 */
public class TimeTunnelAdviceListener extends ReflectAdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TimeTunnelAdviceListener.class);
    private TimeTunnelCommand command;
    private CommandProcess process;

    // 第一次启动标记
    private volatile boolean isFirst = true;

    // 方法执行时间戳
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    public TimeTunnelAdviceListener(TimeTunnelCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        afterFinishing(Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject));
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        afterFinishing(Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    private void afterFinishing(Advice advice) {
        double cost = threadLocalWatch.costInMillis();
        TimeFragment timeTunnel = new TimeFragment(advice, new Date(), cost);

        boolean match = false;
        try {
            match = isConditionMet(command.getConditionExpress(), advice, cost);
        } catch (ExpressException e) {
            logger.warn("tt failed.", e);
            process.end(-1, "tt failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                          + ", visit " + LogUtil.loggingFile() + " for more details.");
        }

        if (!match) {
            return;
        }

        int index = command.putTimeTunnel(timeTunnel);

        TimeFragmentVO timeFragmentVO = TimeTunnelCommand.createTimeFragmentVO(index, timeTunnel);
        TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                .setTimeFragmentList(Arrays.asList(timeFragmentVO))
                .setFirst(isFirst);
        process.appendResult(timeTunnelModel);

        if (isFirst) {
            isFirst = false;
        }

        process.times().incrementAndGet();
        if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
            abortProcess(process, command.getNumberOfLimit());
        }
    }
}

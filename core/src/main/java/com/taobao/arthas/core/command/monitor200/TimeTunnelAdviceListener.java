package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.model.TimeFragmentVO;
import com.taobao.arthas.core.command.model.TimeTunnelModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * @author beiwei30 on 30/11/2016.
 * @author hengyunabc 2020-05-20
 */
public class TimeTunnelAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TimeTunnelAdviceListener.class);
    /**
     * 用 JDK 的 Object[] 做一个固定大小的 ring stack（只存业务对象），避免把 ArthasClassLoader 加载的 ObjectStack 放进
     * 业务线程的 ThreadLocalMap 里，导致 stop/detach 后 ArthasClassLoader 无法被 GC 回收。
     *
     * <pre>
     * 约定：
     * - store[0] 存储 int[1] 的 pos（0..cap）
     * - store[1..cap] 存储 args（Object[]）
     * </pre>
     */
    private static final int ARGS_STACK_SIZE = 512;
    private final ThreadLocal<Object[]> argsRef = ThreadLocal.withInitial(() -> {
        Object[] store = new Object[ARGS_STACK_SIZE + 1];
        store[0] = new int[1];
        return store;
    });

    private TimeTunnelCommand command;
    private CommandProcess process;

    // 第一次启动标记
    private volatile boolean isFirst = true;

    // 方法执行时间戳
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    public TimeTunnelAdviceListener(TimeTunnelCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        pushArgs(args);
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        //取出入参时的 args，因为在函数执行过程中 args可能被修改
        args = popArgs();
        afterFinishing(Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject));
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        //取出入参时的 args，因为在函数执行过程中 args可能被修改
        args = popArgs();
        afterFinishing(Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    private void pushArgs(Object[] args) {
        Object[] store = argsRef.get();
        int[] posHolder = (int[]) store[0];

        int cap = store.length - 1;
        int pos = posHolder[0];
        if (pos < cap) {
            pos++;
        } else {
            // if stack is full, reset pos
            pos = 1;
        }
        store[pos] = args;
        posHolder[0] = pos;
    }

    private Object[] popArgs() {
        Object[] store = argsRef.get();
        int[] posHolder = (int[]) store[0];

        int cap = store.length - 1;
        int pos = posHolder[0];
        if (pos > 0) {
            Object[] args = (Object[]) store[pos];
            store[pos] = null;
            posHolder[0] = pos - 1;
            return args;
        }

        pos = cap;
        Object[] args = (Object[]) store[pos];
        store[pos] = null;
        posHolder[0] = pos - 1;
        return args;
    }

    private void afterFinishing(Advice advice) {
        double cost = threadLocalWatch.costInMillis();
        TimeFragment timeTunnel = new TimeFragment(advice, LocalDateTime.now(), cost);

        boolean match = false;
        try {
            match = isConditionMet(command.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                process.write("Condition express: " + command.getConditionExpress() + " , result: " + match + "\n");
            }
        } catch (ExpressException e) {
            logger.warn("tt failed.", e);
            process.end(-1, "tt failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                          + ", visit " + LogUtil.loggingFile() + " for more details.");
        }

        if (!match) {
            return;
        }

        int index = command.putTimeTunnel(timeTunnel);

        TimeFragmentVO timeFragmentVO = TimeTunnelCommand.createTimeFragmentVO(index, timeTunnel, command.getExpand());
        TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                .setTimeFragmentList(Collections.singletonList(timeFragmentVO))
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

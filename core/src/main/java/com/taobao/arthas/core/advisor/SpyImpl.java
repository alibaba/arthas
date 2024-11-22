package com.taobao.arthas.core.advisor;

import java.arthas.SpyAPI.AbstractSpy;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.ProcessAware;
import com.taobao.arthas.core.util.StringUtils;

/**
 * <pre>
 * 怎么从 className|methodDesc 到 id 对应起来？？
 * 当id少时，可以id自己来判断是否符合？
 * 
 * 如果是每个 className|methodDesc 为 key ，是否
 * </pre>
 * 
 * @author hengyunabc 2020-04-24
 *
 */
public class SpyImpl extends AbstractSpy {
    private static final Logger logger = LoggerFactory.getLogger(SpyImpl.class);
    //将enter时执行过的AdviceListener暂存起来，exit时取出来，再执行过滤，避免对于同一次增强方法的调用，没有执行enter却执行了exit，
    // 用null作为分隔符，尽量减少内存占用
    private static final ThreadLocal<ArrayList<AdviceListener>> LISTENERS = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        ClassLoader classLoader = clazz.getClassLoader();

        String[] info = StringUtils.splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];
        // TODO listener 只用查一次，放到 thread local里保存起来就可以了！
        List<AdviceListener> listeners = AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(),
                methodName, methodDesc);

        ArrayList<AdviceListener> stack = LISTENERS.get();
        stack.add(null);
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    stack.add(adviceListener);
                    adviceListener.before(clazz, methodName, methodDesc, target, args);
                } catch (Throwable e) {
                    logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
                }
            }
        }
    }

    @Override
    public void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args, Object returnObject) {
        //基于目前的增强实现，atExit方法中不能抛出异常，否则，atExit和atExceptionExit的代码可能被同时执行，造成逻辑错乱

        String[] info = StringUtils.splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];

        ArrayList<AdviceListener> stack = LISTENERS.get();
        for (AdviceListener adviceListener; (adviceListener = stack.remove(stack.size() - 1)) != null; ) {
            try {
                if (skipAdviceListener(adviceListener)) {
                    continue;
                }
                adviceListener.afterReturning(clazz, methodName, methodDesc, target, args, returnObject);
            } catch (Throwable e) {
                logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
            }
        }
    }

    @Override
    public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Object[] args, Throwable throwable) {
        String[] info = StringUtils.splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];

        ArrayList<AdviceListener> stack = LISTENERS.get();
        for (AdviceListener adviceListener; (adviceListener = stack.remove(stack.size() - 1)) != null; ) {
            try {
                if (skipAdviceListener(adviceListener)) {
                    continue;
                }
                adviceListener.afterThrowing(clazz, methodName, methodDesc, target, args, throwable);
            } catch (Throwable e) {
                logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
            }
        }
    }

    @Override
    public void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
        ClassLoader classLoader = clazz.getClassLoader();
        String[] info = StringUtils.splitInvokeInfo(invokeInfo);
        String owner = info[0];
        String methodName = info[1];
        String methodDesc = info[2];

        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        ArrayList<AdviceListener> stack = LISTENERS.get();
        stack.add(null);
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    stack.add(adviceListener);
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    listener.invokeBeforeTracing(classLoader, owner, methodName, methodDesc, Integer.parseInt(info[3]));
                } catch (Throwable e) {
                    logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                }
            }
        }
    }

    @Override
    public void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
        ClassLoader classLoader = clazz.getClassLoader();
        String[] info = StringUtils.splitInvokeInfo(invokeInfo);
        String owner = info[0];
        String methodName = info[1];
        String methodDesc = info[2];

        ArrayList<AdviceListener> stack = LISTENERS.get();
        for (AdviceListener adviceListener; (adviceListener = stack.remove(stack.size() - 1)) != null; ) {
            try {
                if (skipAdviceListener(adviceListener)) {
                    continue;
                }
                final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                listener.invokeAfterTracing(classLoader, owner, methodName, methodDesc, Integer.parseInt(info[3]));
            } catch (Throwable e) {
                logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
            }
        }

    }

    @Override
    public void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
        ClassLoader classLoader = clazz.getClassLoader();
        String[] info = StringUtils.splitInvokeInfo(invokeInfo);
        String owner = info[0];
        String methodName = info[1];
        String methodDesc = info[2];

        ArrayList<AdviceListener> stack = LISTENERS.get();
        for (AdviceListener adviceListener; (adviceListener = stack.remove(stack.size() - 1)) != null; ) {
            try {
                if (skipAdviceListener(adviceListener)) {
                    continue;
                }
                final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                listener.invokeThrowTracing(classLoader, owner, methodName, methodDesc, Integer.parseInt(info[3]));
            } catch (Throwable e) {
                logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
            }
        }
    }

    private static boolean skipAdviceListener(AdviceListener adviceListener) {
        if (adviceListener instanceof ProcessAware) {
            ProcessAware processAware = (ProcessAware) adviceListener;
            ExecStatus status = processAware.getProcess().status();
            if (status.equals(ExecStatus.TERMINATED) || status.equals(ExecStatus.STOPPED)) {
                return true;
            }
        }
        return false;
    }

}
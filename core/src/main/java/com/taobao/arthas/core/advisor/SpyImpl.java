package com.taobao.arthas.core.advisor;

import java.arthas.SpyAPI.AbstractSpy;
import java.util.List;
import java.util.regex.Pattern;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.ProcessAware;

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

    @Override
    public void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        ClassLoader classLoader = clazz.getClassLoader();

        String[] info = splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];
        // TODO listener 只用查一次，放到 thread local里保存起来就可以了！
        List<AdviceListener> listeners = AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(),
                methodName, methodDesc);
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    adviceListener.before(clazz, methodName, methodDesc, target, args);
                } catch (Throwable e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
                    }
                }
            }
        }

    }

    @Override
    public void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args, Object returnObject) {
        ClassLoader classLoader = clazz.getClassLoader();

        String[] info = splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];

        List<AdviceListener> listeners = AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(),
                methodName, methodDesc);
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    adviceListener.afterReturning(clazz, methodName, methodDesc, target, args, returnObject);
                } catch (Throwable e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
                    }
                }
            }
        }
    }

    @Override
    public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Object[] args, Throwable throwable) {
        ClassLoader classLoader = clazz.getClassLoader();

        String[] info = splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];

        List<AdviceListener> listeners = AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(),
                methodName, methodDesc);
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    adviceListener.afterThrowing(clazz, methodName, methodDesc, target, args, throwable);
                } catch (Throwable e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
                    }
                }
            }
        }
    }

    @Override
    public void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
        ClassLoader classLoader = clazz.getClassLoader();
        String[] info = splitInvokeInfo(invokeInfo);
        String owner = info[0];
        String methodName = info[1];
        String methodDesc = info[2];

        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    listener.invokeBeforeTracing(owner, methodName, methodDesc, Integer.parseInt(info[3]));
                } catch (Throwable e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                    }
                }
            }
        }
    }

    @Override
    public void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
        ClassLoader classLoader = clazz.getClassLoader();
        String[] info = splitInvokeInfo(invokeInfo);
        String owner = info[0];
        String methodName = info[1];
        String methodDesc = info[2];
        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    listener.invokeAfterTracing(owner, methodName, methodDesc, Integer.parseInt(info[3]));
                } catch (Throwable e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                    }
                }
            }
        }

    }

    @Override
    public void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
        ClassLoader classLoader = clazz.getClassLoader();
        String[] info = splitInvokeInfo(invokeInfo);
        String owner = info[0];
        String methodName = info[1];
        String methodDesc = info[2];

        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    listener.invokeThrowTracing(owner, methodName, methodDesc, Integer.parseInt(info[3]));
                } catch (Throwable e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                    }
                }
            }
        }
    }

    private String[] splitMethodInfo(String methodInfo) {
        return methodInfo.split(Pattern.quote("|"));
    }

    private String[] splitInvokeInfo(String invokeInfo) {
        return invokeInfo.split(Pattern.quote("|"));
    }

    private boolean skipAdviceListener(AdviceListener adviceListener) {
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
package com.taobao.arthas.core.advisor;

import java.arthas.SpyAPI.AbstractSpy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, List<String>> splitCache = new ConcurrentHashMap<String, List<String>>();

    @Override
    public void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        ClassLoader classLoader = clazz.getClassLoader();

        List<String> strs = splitMethodInfo(methodInfo);
        String methodName = strs.get(0);
        String methodDesc = strs.get(1);
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

        List<String> strs = splitMethodInfo(methodInfo);
        String methodName = strs.get(0);
        String methodDesc = strs.get(1);

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

        List<String> strs = splitMethodInfo(methodInfo);
        String methodName = strs.get(0);
        String methodDesc = strs.get(1);

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
        List<String> strs = splitInvokeInfo(invokeInfo);
        String owner = strs.get(0);
        String methodName = strs.get(1);
        String methodDesc = strs.get(2);

        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        if (listeners != null) {
            int lineNumber = Integer.parseInt(strs.get(3));
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    listener.invokeBeforeTracing(owner, methodName, methodDesc, lineNumber);
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
        List<String> strs = splitInvokeInfo(invokeInfo);
        String owner = strs.get(0);
        String methodName = strs.get(1);
        String methodDesc = strs.get(2);
        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        if (listeners != null) {
            int lineNumber = Integer.parseInt(strs.get(3));
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    listener.invokeAfterTracing(owner, methodName, methodDesc, lineNumber);
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
        List<String> strs = splitInvokeInfo(invokeInfo);
        String owner = strs.get(0);
        String methodName = strs.get(1);
        String methodDesc = strs.get(2);

        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        if (listeners != null) {
            int lineNumber = Integer.parseInt(strs.get(3));
            for (AdviceListener adviceListener : listeners) {
                try {
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    listener.invokeThrowTracing(owner, methodName, methodDesc, lineNumber);
                } catch (Throwable e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                    }
                }
            }
        }
    }

    private List<String> splitMethodInfo(String methodInfo) {
        return splitString(methodInfo);
    }

    private List<String> splitInvokeInfo(String invokeInfo) {
        return splitString(invokeInfo);
    }

    /**
     * 经过优化的字符串split方法，减少产生的内存碎片。
     * trace/watch 等字节码拦截回调每次都需要进行字符串split，是一个性能瓶颈hotspot。
     * 注意： 返回的List为重用的缓存对象，不能直接引用它，有需要请复制一份
     * @param str
     * @return
     */
    private List<String> splitString(String str) {
        List<String> strs = splitCache.get(str);
        if (strs == null) {
            strs = new ArrayList<String>();
            StringUtils.splitToList(str, '|', strs);
            splitCache.put(str, strs);
        }
        return strs;
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
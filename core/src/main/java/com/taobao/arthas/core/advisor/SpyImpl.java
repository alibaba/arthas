package com.taobao.arthas.core.advisor;

import java.arthas.SpyAPI.AbstractSpy;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.ProcessAware;
import com.taobao.arthas.core.util.StringUtils;

/**
 * Spy接口的实现类
 *
 * <p>此类是Arthas字节码增强后的代码与Arthas核心逻辑之间的桥梁。
 * 当目标类的方法被增强后，会调用此类的方法，然后此类将调用转发给注册的AdviceListener。</p>
 *
 * <p><b>设计说明：</b></p>
 * <pre>
 * 怎么从 className|methodDesc 到 id 对应起来？？
 * 当id少时，可以id自己来判断是否符合？
 *
 * 如果是每个 className|methodDesc 为 key ，是否
 * </pre>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 *   <li>在方法进入时（atEnter）：通知所有注册的AdviceListener</li>
 *   <li>在方法正常退出时（atExit）：通知所有注册的AdviceListener</li>
 *   <li>在方法异常退出时（atExceptionExit）：通知所有注册的AdviceListener</li>
 *   <li>在方法内部调用其他方法时（atBeforeInvoke/atAfterInvoke/atInvokeException）：通知InvokeTraceable监听器</li>
 * </ul>
 *
 * <p><b>关键特性：</b></p>
 * <ul>
 *   <li>通过AdviceListenerManager查询匹配的AdviceListener</li>
 *   <li>支持多个AdviceListener同时监听同一个方法</li>
 *   <li>自动跳过已终止或已停止的监听器</li>
 *   <li>异常隔离：单个监听器的异常不会影响其他监听器的执行</li>
 * </ul>
 *
 * @author hengyunabc
 * @since 2020-04-24
 */
public class SpyImpl extends AbstractSpy {
    /**
     * 日志记录器，用于记录SpyImpl执行过程中的错误和异常信息
     */
    private static final Logger logger = LoggerFactory.getLogger(SpyImpl.class);

    /**
     * 方法进入时的回调处理
     *
     * <p>当被增强的方法进入时（即方法开始执行前），此方法会被调用。
     * 该方法会查询所有匹配的AdviceListener，并依次调用它们的before方法。</p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>获取类的ClassLoader</li>
     *   <li>解析方法信息（方法名和方法描述符）</li>
     *   <li>查询注册的AdviceListener列表</li>
     *   <li>遍历所有监听器，调用其before方法</li>
     *   <li>捕获并记录任何异常，确保不会影响其他监听器的执行</li>
     * </ol>
     *
     * <p><b>性能优化点：</b></p>
     * <ul>
     *   <li>TODO: listener只用查一次，放到thread local里保存起来就可以了！</li>
     * </ul>
     *
     * @param clazz 被调用的类对象
     * @param methodInfo 方法信息，格式为 "methodName|methodDesc"
     * @param target 目标对象，如果是静态方法则为null
     * @param args 方法参数数组
     */
    @Override
    public void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        // 获取当前类的类加载器，用于查询监听器
        ClassLoader classLoader = clazz.getClassLoader();

        // 解析方法信息，提取方法名和方法描述符
        String[] info = StringUtils.splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];

        // 查询所有匹配此类的AdviceListener
        // TODO listener 只用查一次，放到 thread local里保存起来就可以了！
        List<AdviceListener> listeners = AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(),
                methodName, methodDesc);

        // 如果有匹配的监听器，则依次调用
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    // 检查是否应该跳过此监听器（例如进程已终止）
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    // 调用监听器的方法进入回调
                    adviceListener.before(clazz, methodName, methodDesc, target, args);
                } catch (Throwable e) {
                    // 记录异常，但不影响其他监听器的执行
                    logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
                }
            }
        }

    }

    /**
     * 方法正常退出时的回调处理
     *
     * <p>当被增强的方法正常执行完成（未抛出异常）时，此方法会被调用。
     * 该方法会查询所有匹配的AdviceListener，并依次调用它们的afterReturning方法。</p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>获取类的ClassLoader</li>
     *   <li>解析方法信息</li>
     *   <li>查询注册的AdviceListener列表</li>
     *   <li>遍历所有监听器，调用其afterReturning方法，传递返回值</li>
     *   <li>捕获并记录任何异常</li>
     * </ol>
     *
     * @param clazz 被调用的类对象
     * @param methodInfo 方法信息，格式为 "methodName|methodDesc"
     * @param target 目标对象，如果是静态方法则为null
     * @param args 方法参数数组
     * @param returnObject 方法的返回值
     */
    @Override
    public void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args, Object returnObject) {
        // 获取当前类的类加载器
        ClassLoader classLoader = clazz.getClassLoader();

        // 解析方法信息，提取方法名和方法描述符
        String[] info = StringUtils.splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];

        // 查询所有匹配此类的AdviceListener
        List<AdviceListener> listeners = AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(),
                methodName, methodDesc);

        // 如果有匹配的监听器，则依次调用
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    // 检查是否应该跳过此监听器
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    // 调用监听器的方法正常返回回调
                    adviceListener.afterReturning(clazz, methodName, methodDesc, target, args, returnObject);
                } catch (Throwable e) {
                    // 记录异常，但不影响其他监听器的执行
                    logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
                }
            }
        }
    }

    /**
     * 方法异常退出时的回调处理
     *
     * <p>当被增强的方法执行过程中抛出异常时，此方法会被调用。
     * 该方法会查询所有匹配的AdviceListener，并依次调用它们的afterThrowing方法。</p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>获取类的ClassLoader</li>
     *   <li>解析方法信息</li>
     *   <li>查询注册的AdviceListener列表</li>
     *   <li>遍历所有监听器，调用其afterThrowing方法，传递异常对象</li>
     *   <li>捕获并记录任何异常</li>
     * </ol>
     *
     * @param clazz 被调用的类对象
     * @param methodInfo 方法信息，格式为 "methodName|methodDesc"
     * @param target 目标对象，如果是静态方法则为null
     * @param args 方法参数数组
     * @param throwable 方法抛出的异常对象
     */
    @Override
    public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Object[] args, Throwable throwable) {
        // 获取当前类的类加载器
        ClassLoader classLoader = clazz.getClassLoader();

        // 解析方法信息，提取方法名和方法描述符
        String[] info = StringUtils.splitMethodInfo(methodInfo);
        String methodName = info[0];
        String methodDesc = info[1];

        // 查询所有匹配此类的AdviceListener
        List<AdviceListener> listeners = AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(),
                methodName, methodDesc);

        // 如果有匹配的监听器，则依次调用
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    // 检查是否应该跳过此监听器
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    // 调用监听器的方法异常返回回调
                    adviceListener.afterThrowing(clazz, methodName, methodDesc, target, args, throwable);
                } catch (Throwable e) {
                    // 记录异常，但不影响其他监听器的执行
                    logger.error("class: {}, methodInfo: {}", clazz.getName(), methodInfo, e);
                }
            }
        }
    }

    /**
     * 方法内部调用其他方法前的回调处理
     *
     * <p>当一个方法内部调用另一个方法时，在被调用方法执行前，此方法会被调用。
     * 此方法主要用于实现"trace"命令，跟踪方法调用链路。</p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>获取类的ClassLoader</li>
     *   <li>解析调用信息，提取被调用类的类名、方法名、方法描述符和行号</li>
     *   <li>查询所有匹配的InvokeTraceable监听器</li>
     *   <li>遍历所有监听器，调用其invokeBeforeTracing方法</li>
     *   <li>捕获并记录任何异常</li>
     * </ol>
     *
     * <p><b>invokeInfo格式：</b></p>
     * <pre>owner|methodName|methodDesc|lineNumber</pre>
     *
     * @param clazz 当前正在执行的类对象
     * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
     * @param target 目标对象，如果是静态方法则为null
     */
    @Override
    public void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
        // 获取当前类的类加载器
        ClassLoader classLoader = clazz.getClassLoader();

        // 解析调用信息
        String[] info = StringUtils.splitInvokeInfo(invokeInfo);
        String owner = info[0];  // 被调用类的类名
        String methodName = info[1];  // 被调用的方法名
        String methodDesc = info[2];  // 被调用方法的描述符

        // 查询所有匹配的InvokeTraceable监听器（用于trace命令）
        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        // 如果有匹配的监听器，则依次调用
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    // 检查是否应该跳过此监听器
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    // 强制转换为InvokeTraceable接口
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    // 调用监听器的调用前跟踪回调，传递行号信息
                    listener.invokeBeforeTracing(classLoader, owner, methodName, methodDesc, Integer.parseInt(info[3]));
                } catch (Throwable e) {
                    // 记录异常，但不影响其他监听器的执行
                    logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                }
            }
        }
    }

    /**
     * 方法内部调用其他方法后的回调处理
     *
     * <p>当一个方法内部调用另一个方法，且被调用方法正常执行完成后，此方法会被调用。
     * 此方法主要用于实现"trace"命令，记录方法调用链路的完成情况。</p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>获取类的ClassLoader</li>
     *   <li>解析调用信息</li>
     *   <li>查询所有匹配的InvokeTraceable监听器</li>
     *   <li>遍历所有监听器，调用其invokeAfterTracing方法</li>
     *   <li>捕获并记录任何异常</li>
     * </ol>
     *
     * @param clazz 当前正在执行的类对象
     * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
     * @param target 目标对象，如果是静态方法则为null
     */
    @Override
    public void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
        // 获取当前类的类加载器
        ClassLoader classLoader = clazz.getClassLoader();

        // 解析调用信息
        String[] info = StringUtils.splitInvokeInfo(invokeInfo);
        String owner = info[0];  // 被调用类的类名
        String methodName = info[1];  // 被调用的方法名
        String methodDesc = info[2];  // 被调用方法的描述符

        // 查询所有匹配的InvokeTraceable监听器
        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        // 如果有匹配的监听器，则依次调用
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    // 检查是否应该跳过此监听器
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    // 强制转换为InvokeTraceable接口
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    // 调用监听器的调用后跟踪回调，传递行号信息
                    listener.invokeAfterTracing(classLoader, owner, methodName, methodDesc, Integer.parseInt(info[3]));
                } catch (Throwable e) {
                    // 记录异常，但不影响其他监听器的执行
                    logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                }
            }
        }

    }

    /**
     * 方法内部调用其他方法抛出异常时的回调处理
     *
     * <p>当一个方法内部调用另一个方法，且被调用方法抛出异常时，此方法会被调用。
     * 此方法主要用于实现"trace"命令，记录方法调用链路中的异常情况。</p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>获取类的ClassLoader</li>
     *   <li>解析调用信息</li>
     *   <li>查询所有匹配的InvokeTraceable监听器</li>
     *   <li>遍历所有监听器，调用其invokeThrowTracing方法</li>
     *   <li>捕获并记录任何异常</li>
     * </ol>
     *
     * @param clazz 当前正在执行的类对象
     * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
     * @param target 目标对象，如果是静态方法则为null
     * @param throwable 被调用方法抛出的异常对象
     */
    @Override
    public void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
        // 获取当前类的类加载器
        ClassLoader classLoader = clazz.getClassLoader();

        // 解析调用信息
        String[] info = StringUtils.splitInvokeInfo(invokeInfo);
        String owner = info[0];  // 被调用类的类名
        String methodName = info[1];  // 被调用的方法名
        String methodDesc = info[2];  // 被调用方法的描述符

        // 查询所有匹配的InvokeTraceable监听器
        List<AdviceListener> listeners = AdviceListenerManager.queryTraceAdviceListeners(classLoader, clazz.getName(),
                owner, methodName, methodDesc);

        // 如果有匹配的监听器，则依次调用
        if (listeners != null) {
            for (AdviceListener adviceListener : listeners) {
                try {
                    // 检查是否应该跳过此监听器
                    if (skipAdviceListener(adviceListener)) {
                        continue;
                    }
                    // 强制转换为InvokeTraceable接口
                    final InvokeTraceable listener = (InvokeTraceable) adviceListener;
                    // 调用监听器的异常跟踪回调，传递行号信息
                    listener.invokeThrowTracing(classLoader, owner, methodName, methodDesc, Integer.parseInt(info[3]));
                } catch (Throwable e) {
                    // 记录异常，但不影响其他监听器的执行
                    logger.error("class: {}, invokeInfo: {}", clazz.getName(), invokeInfo, e);
                }
            }
        }
    }

    /**
     * 判断是否应该跳过某个AdviceListener
     *
     * <p>此方法用于检查AdviceListener是否应该被跳过。
     * 如果监听器关联的进程已经终止或停止，则应该跳过该监听器。</p>
     *
     * <p><b>跳过条件：</b></p>
     * <ul>
     *   <li>监听器实现了ProcessAware接口</li>
     *   <li>监听器关联的进程为null</li>
     *   <li>监听器关联的进程状态为TERMINATED或STOPPED</li>
 * </ul>
     *
     * <p><b>设计说明：</b></p>
     * <p>当一个命令（如watch、trace等）被用户终止后，对应的AdviceListener应该不再被调用。
     * 此方法通过检查进程状态来实现这一功能，避免在命令终止后继续执行监听逻辑。</p>
     *
     * @param adviceListener 要检查的AdviceListener
     * @return 如果应该跳过该监听器则返回true，否则返回false
     */
    private static boolean skipAdviceListener(AdviceListener adviceListener) {
        // 检查监听器是否实现了ProcessAware接口
        if (adviceListener instanceof ProcessAware) {
            ProcessAware processAware = (ProcessAware) adviceListener;
            // 获取监听器关联的进程
            Process process = processAware.getProcess();
            // 如果进程为null，说明已经失效，应该跳过
            if (process == null) {
                return true;
            }
            // 获取进程的当前状态
            ExecStatus status = process.status();
            // 如果进程已经终止或停止，应该跳过此监听器
            if (status.equals(ExecStatus.TERMINATED) || status.equals(ExecStatus.STOPPED)) {
                return true;
            }
        }
        // 默认不跳过
        return false;
    }

}
package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Trace命令的监听器
 * 负责监听方法调用并构建调用链路树
 *
 * @author beiwei30 on 29/11/2016.
 */
public class TraceAdviceListener extends AbstractTraceAdviceListener implements InvokeTraceable {

    /**
     * 构造函数
     *
     * @param command Trace命令对象
     * @param process 命令处理进程
     * @param verbose 是否使用详细模式输出
     */
    public TraceAdviceListener(TraceCommand command, CommandProcess process, boolean verbose) {
        super(command, process);
        super.setVerbose(verbose);
    }

    /**
     * 方法调用前的回调
     * trace命令会在被观测的方法体中，在每个方法调用前后插入字节码，
     * 所以当方法调用开始时，会回调此接口
     *
     * @param classLoader 类加载器
     * @param tracingClassName 正在追踪的类名
     * @param tracingMethodName 正在追踪的方法名
     * @param tracingMethodDesc 正在追踪的方法描述符
     * @param tracingLineNumber 正在追踪的行号
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void invokeBeforeTracing(ClassLoader classLoader, String tracingClassName, String tracingMethodName, String tracingMethodDesc, int tracingLineNumber)
            throws Throwable {
        // 在调用树中开始一个新的节点，类名将延迟标准化（normalize className later）
        threadLocalTraceEntity(classLoader).tree.begin(tracingClassName, tracingMethodName, tracingLineNumber, true);
    }

    /**
     * 方法调用后的回调
     * 当被追踪的方法正常返回时，会回调此接口
     *
     * @param classLoader 类加载器
     * @param tracingClassName 正在追踪的类名
     * @param tracingMethodName 正在追踪的方法名
     * @param tracingMethodDesc 正在追踪的方法描述符
     * @param tracingLineNumber 正在追踪的行号
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void invokeAfterTracing(ClassLoader classLoader, String tracingClassName, String tracingMethodName, String tracingMethodDesc, int tracingLineNumber)
            throws Throwable {
        // 结束当前调用树节点
        threadLocalTraceEntity(classLoader).tree.end();
    }

    /**
     * 方法抛出异常时的回调
     * 当被追踪的方法抛出异常时，会回调此接口
     *
     * @param classLoader 类加载器
     * @param tracingClassName 正在追踪的类名
     * @param tracingMethodName 正在追踪的方法名
     * @param tracingMethodDesc 正在追踪的方法描述符
     * @param tracingLineNumber 正在追踪的行号
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void invokeThrowTracing(ClassLoader classLoader, String tracingClassName, String tracingMethodName, String tracingMethodDesc, int tracingLineNumber)
            throws Throwable {
        // 结束当前调用树节点，并标记为异常状态
        threadLocalTraceEntity(classLoader).tree.end(true);
    }

}

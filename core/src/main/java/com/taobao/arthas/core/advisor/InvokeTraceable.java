package com.taobao.arthas.core.advisor;

/**
 * 方法调用跟踪接口
 *
 * <p>此接口用于在方法内部调用其他方法时进行跟踪。当一个方法内部调用另外一个方法时，
 * 会触发此接口中定义的跟踪方法，实现对方法调用的监控和追踪。</p>
 *
 * <p>该接口主要用于实现类似"trace"命令的功能，可以跟踪方法调用链路，
 * 帮助开发者了解方法的调用层次和执行流程。</p>
 *
 * <p>接口中定义了三个跟踪时机：</p>
 * <ul>
 *   <li>调用前跟踪（invokeBeforeTracing）：在目标方法调用之前触发</li>
 *   <li>调用后跟踪（invokeAfterTracing）：在目标方法调用正常返回后触发</li>
 *   <li>异常跟踪（invokeThrowTracing）：在目标方法调用抛出异常时触发</li>
 * </ul>
 *
 * <p>通过实现此接口，可以在方法调用的不同时机执行自定义的逻辑，
 * 例如记录调用链路、统计调用次数、分析性能瓶颈等。</p>
 *
 * @author vlinux
 * @since 2015-05-27
 */
public interface InvokeTraceable {

    /**
     * 在方法调用之前进行跟踪
     *
     * <p>当一个方法内部调用另一个方法时，在被调用方法执行之前会触发此方法。
     * 可以在此处记录调用信息、检查调用条件等。</p>
     *
     * <p>典型应用场景：</p>
     * <ul>
     *   <li>记录方法调用的起始时间</li>
     *   <li>构建方法调用链路</li>
     *   <li>输出方法调用日志</li>
     * </ul>
     *
     * @param classLoader 类加载器，用于加载被调用的类
     * @param tracingClassName 被调用类的类名，格式为全限定名（如 "com.example.MyClass"）
     * @param tracingMethodName 被调用的方法名
     * @param tracingMethodDesc 被调用方法的描述符（JVM类型签名，描述参数类型和返回类型）
     * @param tracingLineNumber 执行调用的源代码行号
     * @throws Throwable 如果通知过程中出现错误，可以抛出异常
     */
    void invokeBeforeTracing(
            ClassLoader classLoader,
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc,
            int tracingLineNumber) throws Throwable;

    /**
     * 在方法调用抛出异常时进行跟踪
     *
     * <p>当一个方法内部调用另一个方法，且被调用方法抛出异常时，会触发此方法。
     * 可以在此处记录异常信息、进行异常处理等。</p>
     *
     * <p>典型应用场景：</p>
     * <ul>
     *   <li>记录方法调用失败的异常信息</li>
     *   <li>统计方法调用失败率</li>
     *   <li>分析异常原因和调用链路</li>
     * </ul>
     *
     * @param classLoader 类加载器，用于加载被调用的类
     * @param tracingClassName 被调用类的类名，格式为全限定名
     * @param tracingMethodName 被调用的方法名
     * @param tracingMethodDesc 被调用方法的描述符
     * @param tracingLineNumber 执行调用的源代码行号
     * @throws Throwable 如果通知过程中出现错误，可以抛出异常
     */
    void invokeThrowTracing(
            ClassLoader classLoader,
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc,
            int tracingLineNumber) throws Throwable;


    /**
     * 在方法调用正常返回后进行跟踪
     *
     * <p>当一个方法内部调用另一个方法，且被调用方法正常执行完成后（未抛出异常），
     * 会触发此方法。可以在此处记录调用结果、统计执行时间等。</p>
     *
     * <p>典型应用场景：</p>
     * <ul>
     *   <li>记录方法调用的结束时间，计算耗时</li>
     *   <li>构建完整的方法调用树</li>
     *   <li>分析方法调用的返回结果</li>
     * </ul>
     *
     * @param classLoader 类加载器，用于加载被调用的类
     * @param tracingClassName 被调用类的类名，格式为全限定名
     * @param tracingMethodName 被调用的方法名
     * @param tracingMethodDesc 被调用方法的描述符
     * @param tracingLineNumber 执行调用的源代码行号
     * @throws Throwable 如果通知过程中出现错误，可以抛出异常
     */
    void invokeAfterTracing(
            ClassLoader classLoader,
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc,
            int tracingLineNumber) throws Throwable;


}

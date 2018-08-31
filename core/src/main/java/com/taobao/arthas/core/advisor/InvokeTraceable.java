package com.taobao.arthas.core.advisor;

/**
 * 方法调用跟踪<br/>
 * 当一个方法内部调用另外一个方法时，会出发此跟踪方法
 * Created by vlinux on 15/5/27.
 */
public interface InvokeTraceable {

    /**
     * 调用之前跟踪
     *
     * @param tracingClassName  调用类名
     * @param tracingMethodName 调用方法名
     * @param tracingMethodDesc 调用方法描述
     * @throws Throwable 通知过程出错
     */
    void invokeBeforeTracing(
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc) throws Throwable;

    /**
     * 抛异常后跟踪
     *
     * @param tracingClassName  调用类名
     * @param tracingMethodName 调用方法名
     * @param tracingMethodDesc 调用方法描述
     * @throws Throwable 通知过程出错
     */
    void invokeThrowTracing(
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc) throws Throwable;


    /**
     * 调用之后跟踪
     *
     * @param tracingClassName  调用类名
     * @param tracingMethodName 调用方法名
     * @param tracingMethodDesc 调用方法描述
     * @throws Throwable 通知过程出错
     */
    void invokeAfterTracing(
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc) throws Throwable;


}

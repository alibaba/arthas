package com.taobao.arthas.core.advisor;

/**
 * 通知监听器<br/>
 * Created by vlinux on 15/5/17.
 */
public interface AdviceListener {

    long id();

    /**
     * 监听器创建<br/>
     * 监听器被注册时触发
     */
    void create();

    /**
     * 监听器销毁<br/>
     * 监听器被销毁时触发
     */
    void destroy();

    /**
     * 前置通知
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param methodDesc 方法描述
     * @param target     目标类实例
     *                   若目标为静态方法,则为null
     * @param args       参数列表
     * @throws Throwable 通知过程出错
     */
    void before(
            Class<?> clazz, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable;

    /**
     * 返回通知
     *
     * @param clazz        类
     * @param methodName   方法名
     * @param methodDesc   方法描述
     * @param target       目标类实例
     *                     若目标为静态方法,则为null
     * @param args         参数列表
     * @param returnObject 返回结果
     *                     若为无返回值方法(void),则为null
     * @throws Throwable 通知过程出错
     */
    void afterReturning(
            Class<?> clazz, String methodName, String methodDesc,
            Object target, Object[] args,
            Object returnObject) throws Throwable;

    /**
     * 异常通知
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param methodDesc 方法描述
     * @param target     目标类实例
     *                   若目标为静态方法,则为null
     * @param args       参数列表
     * @param throwable  目标异常
     * @throws Throwable 通知过程出错
     */
    void afterThrowing(
            Class<?> clazz, String methodName, String methodDesc,
            Object target, Object[] args,
            Throwable throwable) throws Throwable;

    /**
     * 行观测的监听回调方法
     * line 命令中使用，查看本地变量等
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param methodDesc 方法描述
     * @param target     目标类实例，若目标为静态方法,则为null
     * @param args       参数列表
     * @param line       行标识，可能是行号(LineNumber)，也可能是行的特殊标号(LineCode)
     * @param vars       本地变量数组
     * @param varNames   本地变量名数组
     * @throws Throwable 通知过程出错
     */
    void atLine(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args, String line, Object[] vars, String[] varNames) throws Throwable;

}

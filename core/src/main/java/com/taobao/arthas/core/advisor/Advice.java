package com.taobao.arthas.core.advisor;

/**
 * 通知点 Created by vlinux on 15/5/20.
 */
public class Advice {

    private final ClassLoader loader;
    private final Class<?> clazz;
    private final ArthasMethod method;
    private final Object target;
    private final Object[] params;
    private final Object returnObj;
    private final Throwable throwExp;

    private final static int ACCESS_BEFORE = 1;
    private final static int ACCESS_AFTER_RETUNING = 1 << 1;
    private final static int ACCESS_AFTER_THROWING = 1 << 2;

    private final boolean isBefore;
    private final boolean isThrow;
    private final boolean isReturn;

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isAfterReturning() {
        return isReturn;
    }

    public boolean isAfterThrowing() {
        return isThrow;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public Object getTarget() {
        return target;
    }

    public Object[] getParams() {
        return params;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public Throwable getThrowExp() {
        return throwExp;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public ArthasMethod getMethod() {
        return method;
    }

    /**
     * for finish
     *
     * @param loader 类加载器
     * @param clazz 类
     * @param method 方法
     * @param target 目标类
     * @param params 调用参数
     * @param returnObj 返回值
     * @param throwExp 抛出异常
     * @param access 进入场景
     */
    private Advice(
            ClassLoader loader,
            Class<?> clazz,
            ArthasMethod method,
            Object target,
            Object[] params,
            Object returnObj,
            Throwable throwExp,
            int access) {
        this.loader = loader;
        this.clazz = clazz;
        this.method = method;
        this.target = target;
        this.params = params;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
        isBefore = (access & ACCESS_BEFORE) == ACCESS_BEFORE;
        isThrow = (access & ACCESS_AFTER_THROWING) == ACCESS_AFTER_THROWING;
        isReturn = (access & ACCESS_AFTER_RETUNING) == ACCESS_AFTER_RETUNING;
    }

    public static Advice newForBefore(ClassLoader loader,
            Class<?> clazz,
            ArthasMethod method,
            Object target,
            Object[] params) {
        return new Advice(
                loader,
                clazz,
                method,
                target,
                params,
                null, //returnObj
                null, //throwExp
                ACCESS_BEFORE
        );
    }

    public static Advice newForAfterRetuning(ClassLoader loader,
            Class<?> clazz,
            ArthasMethod method,
            Object target,
            Object[] params,
            Object returnObj) {
        return new Advice(
                loader,
                clazz,
                method,
                target,
                params,
                returnObj,
                null, //throwExp
                ACCESS_AFTER_RETUNING
        );
    }

    public static Advice newForAfterThrowing(ClassLoader loader,
            Class<?> clazz,
            ArthasMethod method,
            Object target,
            Object[] params,
            Throwable throwExp) {
        return new Advice(
                loader,
                clazz,
                method,
                target,
                params,
                null, //returnObj
                throwExp,
                ACCESS_AFTER_THROWING
        );
    }

}

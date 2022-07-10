package com.taobao.arthas.core.advisor;

import java.util.LinkedHashMap;

/**
 * 通知点 Created by vlinux on 15/5/20.
 */
public class Advice {

    private final ClassLoader loader;
    private final Class<?> clazz;
    private final ArthasMethod method;
    private final Object target;
    private final Object[] params;
    private final int line;
    private final LinkedHashMap<String, Object> varMap;
    private final Object returnObj;
    private final Throwable throwExp;
    private final boolean isBefore;
    private final boolean isThrow;
    private final boolean isReturn;
    private final boolean isAtLine;

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isAfterReturning() {
        return isReturn;
    }

    public boolean isAfterThrowing() {
        return isThrow;
    }

    public boolean isAtLine() {
        return isAtLine;
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

    public int getLine() {
        return line;
    }

    public String[] getVarNames() {
        return varMap.keySet().toArray(new String[0]);
    }

    public Object[] getVars() {
        return varMap.values().toArray(new Object[0]);
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
     * @param loader    类加载器
     * @param clazz     类
     * @param method    方法
     * @param target    目标类
     * @param params    调用参数
     * @param returnObj 返回值
     * @param throwExp  抛出异常
     * @param access    进入场景
     */
    private Advice(
            ClassLoader loader,
            Class<?> clazz,
            ArthasMethod method,
            Object target,
            Object[] params,
            int line,
            String[] varNames,
            Object[] vars,
            Object returnObj,
            Throwable throwExp,
            int access) {
        this.loader = loader;
        this.clazz = clazz;
        this.method = method;
        this.target = target;
        this.varMap = new LinkedHashMap<String, Object>();
        this.params = params;
        this.line = line;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
        isBefore = (access & AccessPoint.ACCESS_BEFORE.getValue()) == AccessPoint.ACCESS_BEFORE.getValue();
        isThrow = (access & AccessPoint.ACCESS_AFTER_THROWING.getValue()) == AccessPoint.ACCESS_AFTER_THROWING.getValue();
        isReturn = (access & AccessPoint.ACCESS_AFTER_RETUNING.getValue()) == AccessPoint.ACCESS_AFTER_RETUNING.getValue();
        isAtLine = (access & AccessPoint.ACCESS_AT_LINE.getValue()) == AccessPoint.ACCESS_AT_LINE.getValue();

        if (varNames != null && vars != null) {
            for (int i = 0; i < varNames.length; i++) {
                varMap.put(varNames[i], vars[i]);
            }
        }
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
                0,
                null,
                null,
                null, //returnObj
                null, //throwExp
                AccessPoint.ACCESS_BEFORE.getValue()
        );
    }

    public static Advice newForAfterReturning(ClassLoader loader,
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
                0,
                null, // varNames
                null, // vars
                returnObj,
                null, //throwExp
                AccessPoint.ACCESS_AFTER_RETUNING.getValue()
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
                0,
                null, // varNames
                null, // vars
                null, //returnObj
                throwExp,
                AccessPoint.ACCESS_AFTER_THROWING.getValue()
        );

    }

    public static Advice newForAtLine(ClassLoader loader,
        Class<?> clazz,
        ArthasMethod method,
        Object target,
        Object[] params,
        int line,
        String[] varNames,
        Object[] vars) {
        return new Advice(
            loader,
            clazz,
            method,
            target,
            params,
            line,
            varNames,
            vars,
            null, // returnObj
            null, // throwExp
            AccessPoint.ACCESS_AT_LINE.getValue()
        );

    }

}

package com.taobao.arthas.core.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final int lineNumber;
    private final String[] argNames;
    private final Object[] localVars;
    private final String[] localVarNames;
    private final Map<String, Object> localVarMap;
    private final boolean isBefore;
    private final boolean isThrow;
    private final boolean isReturn;
    private final boolean isLine;

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isAfterReturning() {
        return isReturn;
    }

    public boolean isAfterThrowing() {
        return isThrow;
    }

    public boolean isLine() {
        return isLine;
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

    public int getLineNumber() {
        return lineNumber;
    }

    public String[] getArgNames() {
        return argNames;
    }

    public Object[] getLocalVars() {
        return localVars;
    }

    public Object[] getLocals() {
        return localVars;
    }

    public String[] getLocalVarNames() {
        return localVarNames;
    }

    public Map<String, Object> getLocalVarMap() {
        return localVarMap;
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
            Object returnObj,
            Throwable throwExp,
            int access) {
        this(loader, clazz, method, target, params, returnObj, throwExp, access, -1, null, null, null);
    }

    private Advice(
            ClassLoader loader,
            Class<?> clazz,
            ArthasMethod method,
            Object target,
            Object[] params,
            Object returnObj,
            Throwable throwExp,
            int access,
            int lineNumber,
            String[] argNames,
            Object[] localVars,
            String[] localVarNames) {
        this.loader = loader;
        this.clazz = clazz;
        this.method = method;
        this.target = target;
        this.params = params;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
        this.lineNumber = lineNumber;
        this.argNames = argNames;
        LocalVariableSnapshot snapshot = normalizeLocalVariables(localVarNames, localVars);
        this.localVars = snapshot.values;
        this.localVarNames = snapshot.names;
        this.localVarMap = buildLocalVarMap(this.localVarNames, this.localVars);
        isBefore = (access & AccessPoint.ACCESS_BEFORE.getValue()) == AccessPoint.ACCESS_BEFORE.getValue();
        isThrow = (access & AccessPoint.ACCESS_AFTER_THROWING.getValue()) == AccessPoint.ACCESS_AFTER_THROWING.getValue();
        isReturn = (access & AccessPoint.ACCESS_AFTER_RETUNING.getValue()) == AccessPoint.ACCESS_AFTER_RETUNING.getValue();
        isLine = (access & AccessPoint.ACCESS_LINE.getValue()) == AccessPoint.ACCESS_LINE.getValue();
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
                null, //returnObj
                throwExp,
                AccessPoint.ACCESS_AFTER_THROWING.getValue()
        );

    }

    public static Advice newForLine(ClassLoader loader,
                                    Class<?> clazz,
                                    ArthasMethod method,
                                    Object target,
                                    Object[] params,
                                    int lineNumber,
                                    String[] argNames,
                                    Object[] localVars,
                                    String[] localVarNames) {
        return new Advice(
                loader,
                clazz,
                method,
                target,
                params,
                null, //returnObj
                null, //throwExp
                AccessPoint.ACCESS_LINE.getValue(),
                lineNumber,
                argNames,
                localVars,
                localVarNames
        );
    }

    private static Map<String, Object> buildLocalVarMap(String[] localVarNames, Object[] localVars) {
        if (localVarNames == null || localVars == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        int length = Math.min(localVarNames.length, localVars.length);
        for (int i = 0; i < length; i++) {
            String name = localVarNames[i];
            if (name != null && name.length() > 0) {
                result.put(name, localVars[i]);
            }
        }
        return result;
    }

    private static LocalVariableSnapshot normalizeLocalVariables(String[] localVarNames, Object[] localVars) {
        if (localVarNames == null || localVars == null) {
            return new LocalVariableSnapshot(localVarNames, localVars);
        }
        int length = Math.min(localVarNames.length, localVars.length);
        int thisIndex = -1;
        for (int i = 0; i < length; i++) {
            if ("this".equals(localVarNames[i])) {
                thisIndex = i;
                break;
            }
        }
        if (thisIndex < 0) {
            return new LocalVariableSnapshot(localVarNames, localVars);
        }
        String[] filteredNames = new String[length - 1];
        Object[] filteredValues = new Object[length - 1];
        int index = 0;
        for (int i = 0; i < length; i++) {
            if (i == thisIndex) {
                continue;
            }
            filteredNames[index] = localVarNames[i];
            filteredValues[index] = localVars[i];
            index++;
        }
        return new LocalVariableSnapshot(filteredNames, filteredValues);
    }

    private static class LocalVariableSnapshot {
        private final String[] names;
        private final Object[] values;

        private LocalVariableSnapshot(String[] names, Object[] values) {
            this.names = names;
            this.values = values;
        }
    }

}

package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author gongdewei 2020/4/9
 */
public class MethodModel extends ResultModel {
    private MethodVO methodInfo;
    private Class clazz;
    private Method method;
    private Constructor constructor;
    private boolean detail;

    public MethodModel() {
    }

    public MethodModel(Method method, Class clazz, boolean detail) {
        this.clazz = clazz;
        this.method = method;
        this.detail = detail;
    }

    public MethodModel(Constructor constructor, Class clazz, boolean detail) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.detail = detail;
    }

    public MethodVO getMethodInfo() {
        if (methodInfo == null) {
            synchronized (this){
                if (constructor!=null) {
                    methodInfo = ClassUtils.createMethodInfo(constructor, clazz, detail);
                } else {
                    methodInfo = ClassUtils.createMethodInfo(method, clazz, detail);
                }
            }
        }
        return methodInfo;
    }

    public boolean detail() {
        return detail;
    }

    public Class clazz() {
        return clazz;
    }

    public Constructor constructor() {
        return constructor;
    }

    public Method method() {
        return method;
    }

    @Override
    public String getType() {
        return "method";
    }
}

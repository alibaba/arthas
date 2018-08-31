package com.taobao.arthas.core.advisor;

/**
 * 通知监听适配器
 */
public class AdviceListenerAdapter implements AdviceListener {


    @Override
    public void create() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void before(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable {

    }

    @Override
    public void afterReturning(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args,
            Object returnObject) throws Throwable {

    }

    @Override
    public void afterThrowing(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args,
            Throwable throwable) throws Throwable {

    }

}


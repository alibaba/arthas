package com.taobao.arthas.core.util;

/**
 * @author jie xu
 * @description
 * 用于存储monitor命令的condition-express的执行结果,穿透before到afterReturning,afterThrowing
 * @created 2020/8/12
 **/
public class ThreadLocalContext {
    private final ThreadLocal<Boolean> conditionResult = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };

    public void setConditionResult(boolean pass) {
        conditionResult.set(pass);
    }

    public boolean getConditionResult() {
        return conditionResult.get();
    }
}

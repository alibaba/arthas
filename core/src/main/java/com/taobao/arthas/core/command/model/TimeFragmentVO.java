package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * VO for TimeFragment
 * @author gongdewei 2020/4/27
 */
public class TimeFragmentVO {
    private Integer index;
    private LocalDateTime timestamp;
    private double cost;
    private boolean isReturn;
    private boolean isThrow;
    private String object;
    private String className;
    private String methodName;
    private ObjectVO[] params;
    private ObjectVO returnObj;
    private ObjectVO throwExp;

    public TimeFragmentVO() {
    }

    public Integer getIndex() {
        return index;
    }

    public TimeFragmentVO setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TimeFragmentVO setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public double getCost() {
        return cost;
    }

    public TimeFragmentVO setCost(double cost) {
        this.cost = cost;
        return this;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public TimeFragmentVO setReturn(boolean aReturn) {
        isReturn = aReturn;
        return this;
    }

    public boolean isThrow() {
        return isThrow;
    }

    public TimeFragmentVO setThrow(boolean aThrow) {
        isThrow = aThrow;
        return this;
    }

    public String getObject() {
        return object;
    }

    public TimeFragmentVO setObject(String object) {
        this.object = object;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public TimeFragmentVO setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public TimeFragmentVO setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public ObjectVO[] getParams() {
        return params;
    }

    public TimeFragmentVO setParams(ObjectVO[] params) {
        this.params = params;
        return this;
    }

    public ObjectVO getReturnObj() {
        return returnObj;
    }

    public TimeFragmentVO setReturnObj(ObjectVO returnObj) {
        this.returnObj = returnObj;
        return this;
    }

    public ObjectVO getThrowExp() {
        return throwExp;
    }

    public TimeFragmentVO setThrowExp(ObjectVO throwExp) {
        this.throwExp = throwExp;
        return this;
    }
}

package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * Class enhance affect vo - pure data transfer object
 * @author gongdewei 2020/6/22
 */
public class EnhancerAffectVO {

    private long cost;
    private int methodCount;
    private int classCount;
    private long listenerId;
    private Throwable throwable;
    private List<String> classDumpFiles;
    private List<String> methods;
    private String overLimitMsg;

    public EnhancerAffectVO() {
    }

    public EnhancerAffectVO(long cost, int methodCount, int classCount, long listenerId) {
        this.cost = cost;
        this.methodCount = methodCount;
        this.classCount = classCount;
        this.listenerId = listenerId;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public long getListenerId() {
        return listenerId;
    }

    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public List<String> getClassDumpFiles() {
        return classDumpFiles;
    }

    public void setClassDumpFiles(List<String> classDumpFiles) {
        this.classDumpFiles = classDumpFiles;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public void setOverLimitMsg(String overLimitMsg) {
        this.overLimitMsg = overLimitMsg;
    }

    public String getOverLimitMsg() {
        return overLimitMsg;
    }
}

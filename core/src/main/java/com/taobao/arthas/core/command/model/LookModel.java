package com.taobao.arthas.core.command.model;

import java.util.Date;

/**
 * Look command result model
 *
 */
public class LookModel extends ResultModel {

    private Date ts;
    private ObjectVO value;

    private Integer sizeLimit;
    private String className;
    private String methodName;
    private String accessPoint;

    public LookModel() {
    }

    @Override
    public String getType() {
        return "look";
    }

    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    public ObjectVO getValue() {
        return value;
    }

    public void setValue(ObjectVO value) {
        this.value = value;
    }

    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(String accessPoint) {
        this.accessPoint = accessPoint;
    }
}
